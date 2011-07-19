package org.distropia.server;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import javax.servlet.http.HttpServletResponse;

import net.tomp2p.peers.PeerAddress;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.distropia.client.Utils;
import org.distropia.server.communication.DistributedBookHttpClient;
import org.distropia.server.communication.HereAreMyAddressesRequest;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;
import org.distropia.server.communication.ProxiedHosts;
import org.distropia.server.communication.ProxyConnectionThread;
import org.distropia.server.communication.dht.DHT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectionStatus {
	protected volatile boolean connectedToInternet = false; // i am connected
	protected volatile boolean lastConnectedToInternet = false;	
	
	protected boolean checking = false;
	protected int internetPort = 8080; // my port
	protected String internetProtocol = "http";
	protected String internetAddress = null;
	protected String lastInternetAddress = null;
	protected java.util.Timer testConnectionTimer = null;
	protected volatile boolean needsBootstrap = false;
	private static transient Logger logger = LoggerFactory.getLogger(ConnectionStatus.class); // $codepro.audit.disable transientFieldInNonSerializable
	protected ArrayList<ProxyConnectionThread> proxyConnectionThreads = new ArrayList<ProxyConnectionThread>();
	protected ArrayList<RemoveAddressFromHostIfOnlineItem> removeAddressFromHostIfOnline = new ArrayList<RemoveAddressFromHostIfOnlineItem>();
	protected long nextConnectionCheckAt = System.currentTimeMillis() + 1000;
	protected int successfulConnectionChecks = 0;
	protected ArrayList<String> reachableAt = new ArrayList<String>();
	protected long nextDHTPublish = 0;
	protected long nextCheckMyInternetIp = 0;
	protected long nextRemoveOneItemFromCheckedAddressesToBeOurs = 0;
	protected volatile int externalInternetPort = -1;
	protected volatile int externalInternetPortFromConfig = -1;
	protected ArrayList<String> addressesToCheckIfOurs = new ArrayList<String>();
	protected ArrayList<String> alreadyCheckedAddressesToBeOurs = new ArrayList<String>();
	protected boolean upnpAllowed = true;
	protected boolean didBootstrap = false;
	protected String manualInternetAddress = null;
	
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getReachableAt(){
		synchronized (reachableAt) {
			return (ArrayList<String>) reachableAt.clone();
		}
	}
	
	public void addAddressesThatCouldBeOurs(List<String> addresses){
		for(String address: addresses)
			addAddressThatCouldBeOurs( address);
	}
	
	public void addAddressThatCouldBeOurs(String address){
		if (Utils.isNullOrEmpty( address)) return;
		if (Backend.getAutoconf().isThisAPrivateNetwork( address)) return;
		synchronized (alreadyCheckedAddressesToBeOurs) {
			if (alreadyCheckedAddressesToBeOurs.contains( address)) return;
		}
		synchronized (addressesToCheckIfOurs) {
			if (!addressesToCheckIfOurs.contains( address)) 
				addressesToCheckIfOurs.add( address);
		}
	}
	
	public boolean isReachable(){
		return internetAddress != null;
	}
	
	
	@SuppressWarnings("unchecked")
	public void onConnectionStatusChanged()
	{
		ArrayList<String> addresses = new ArrayList<String>();
		
		if (isReachable())
		{
			addresses.add( KnownHost.buildAddress( internetProtocol, getInternetAddress(), getInternetPort()));
		}
		
		ArrayList<ProxyConnectionThread> proxyConnectionThreads = null;
		synchronized ( this.proxyConnectionThreads) {
			proxyConnectionThreads = (ArrayList<ProxyConnectionThread>) this.proxyConnectionThreads.clone();
		}
			
		for(ProxyConnectionThread proxyConnectionThread: proxyConnectionThreads)
		{
			if (proxyConnectionThread.isAccepted()) 
			{
				KnownHost proxyHost = proxyConnectionThread.getProxyHost();
				if (proxyHost != null)
					if (proxyHost.getUniqueHostId() != null)
					{
						StringBuilder uid = new StringBuilder( "uid://" + proxyHost.getUniqueHostId());
						List<String> proxyAddresses = proxyHost.getAddresses();
						synchronized (proxyAddresses) {
							for(String address: proxyAddresses){
								if (address != null)
									if(!address.startsWith("uid://"))
										uid.append("|" + address);
							}
						}
						addresses.add( uid.toString());
					}
			}
		}
		
		boolean changed;
		synchronized ( reachableAt) {
			
			changed = !addresses.equals( reachableAt);
			if (changed)
			{
				reachableAt.clear();
				reachableAt.addAll( addresses);				
			}
		}
		
		if (changed) // populate new addresses
		{
			logger.info("My addresses changed, populating!");
			nextDHTPublish = 0;
			sendAddressesToAllImportantKnownHosts( addresses);
		}
		else logger.info("My addresses did not change.");
	}
	
	private void sendAddressesToAllImportantKnownHosts(ArrayList<String> addresses) {
		HereAreMyAddressesRequest request = new HereAreMyAddressesRequest(addresses);
		
		KnownHosts knownHosts = Backend.getMyKnownHosts();
		synchronized (knownHosts) {
			for(KnownHost knownHost: knownHosts){
				if (knownHost.isImportantHost()) knownHost.sendCommandAsync(request, null);
				
			}
		}
	}
	
	public boolean isConnectedToInternetDirectly()
	{
		if (getInternetAddress() == null) return false;
		return (Backend.getAutoconf().isOneOfMyLocalIPs( getInternetAddress()));
	}

	public void onProxyConnectionThreadFinished( ProxyConnectionThread proxyConnectionThread)
	{
		synchronized (proxyConnectionThreads) {
			proxyConnectionThreads.remove( proxyConnectionThread);
		}
		onConnectionStatusChanged();
	}
	
	protected static class RemoveAddressFromHostIfOnlineItem{
		String address;
		KnownHost knownHost;
		public RemoveAddressFromHostIfOnlineItem(String address,
				KnownHost knownHost) {
			super();
			this.address = address;
			this.knownHost = knownHost;
		}		
	}
	
	public synchronized void removeAddressFromHostIfOnline(KnownHost knownHost, String address)
	{
		if (connectedToInternet){
			removeAddressFromHostIfOnline.add( new RemoveAddressFromHostIfOnlineItem(address, knownHost));
			reduceTimeToNextConnectionCheck();
		}
	}
	
	public synchronized void setNextConnectionCheckToImmediateIfNotOnlineAndReachable()
	{
		if ((!connectedToInternet) && (!isReachable()))
			nextConnectionCheckAt = 0;
	}
	
	public synchronized void reduceTimeToNextConnectionCheck()
	{
		if (isConnectedToInternet())
			nextConnectionCheckAt = nextConnectionCheckAt - 10000;
	}

	public boolean isConnectedToInternet() {
		return connectedToInternet;
	}


	public void setConnectedToInternet(boolean connectedToInternet) {
		this.connectedToInternet = connectedToInternet;
	}


	public ArrayList<ProxyConnectionThread> getProxyConnectionThreads() {
		return proxyConnectionThreads;
	}
	
	private synchronized boolean isNextCheckPending()
	{
		return nextConnectionCheckAt < System.currentTimeMillis(); 
	}
	
	private synchronized boolean isNextDHTPublishPending()
	{
		return nextDHTPublish < System.currentTimeMillis(); 
	}
	
	public void doConnectionWork()
	{
		
		if (!isReachable()){
			if (nextRemoveOneItemFromCheckedAddressesToBeOurs < System.currentTimeMillis()){
				synchronized (alreadyCheckedAddressesToBeOurs) {
					if (alreadyCheckedAddressesToBeOurs.size()>0) alreadyCheckedAddressesToBeOurs.remove(0);
				}
				nextRemoveOneItemFromCheckedAddressesToBeOurs = System.currentTimeMillis() + 60000;
				while (alreadyCheckedAddressesToBeOurs.size()>0) alreadyCheckedAddressesToBeOurs.remove(0);
			}
		}
		synchronized (addressesToCheckIfOurs) {
			while (addressesToCheckIfOurs.size()>100) addressesToCheckIfOurs.remove(0);
		}
		
		if (isNextDHTPublishPending()){
			synchronized ( reachableAt) {
				if (reachableAt.size()>0){
					if (Backend.getDHT().publishMyAddresses( reachableAt)) nextDHTPublish = System.currentTimeMillis() + 600000; // publish every 10 Minutes
				}
				else nextDHTPublish = System.currentTimeMillis() + 600000; // publish every 10 Minutes
			}			
		}
		
		if (isNextCheckPending())
		{
			logger.info("checking connection status");
			KnownHosts knownHosts = Backend.getMyKnownHosts();
			
			// rebuilt this routine to first check via DHT
			if (!connectedToInternet){
				DHT dht = Backend.getDHT();
				if (dht.needsBootstrap() || (knownHosts.size() == 0)){
					ArrayList<InetSocketAddress> bootstrapFrom = new ArrayList<InetSocketAddress>(5);
					for(int index=0; index<4; index++){
						InetSocketAddress randomPeer = Backend.getCommunicationDatabase().getRandomPeerAddress();
						if (randomPeer != null){
							if (!bootstrapFrom.contains( randomPeer)) bootstrapFrom.add( randomPeer);
						}
					}
					
					if ((bootstrapFrom.size() > 0) && (dht.bootstrapFromInetSocketAddress( bootstrapFrom))){ // successful bootstrap, now we can get some knownHosts from DHT
						ArrayList<PeerAddress> fetchFrom = new ArrayList<PeerAddress>();
						fetchFrom.addAll( dht.getKnownPeers());
						int max = KnownHosts.MINIMUM_LIST_SIZE;
						for(PeerAddress peerAddress: fetchFrom){
							if (dht.getKnownHostOfPeerAddress(peerAddress) != null) // is already added	to knownHosts
							{
								max--;
								if (max==0) break;
							}
						}
					}
				}
			}
			
			checking = true;
			boolean onlyEstimatedOnline = false;
			try
			{
				if (!didBootstrap && !needsBootstrap)
				{
					didBootstrap = true;
					if (knownHosts.size() == 0)
					{
						Backend.getInstance().onNeedsBootstrap();
						
						if (knownHosts.size() == 0)
						{
							needsBootstrap = true;
							logger.error("Error, no known hosts, bootstrap required!");
						}
						else needsBootstrap = false;					
					}
					else needsBootstrap = false;
				}
				
				List<KnownHost> alreadyPingedList = new ArrayList<KnownHost>(5);
				boolean newConnectedToInternet = false;
				if (knownHosts.size() >0)
				{
					Random random = new Random();
					for(int index=0; index<5; index++)
					{
						KnownHost hostToTest;
						synchronized (knownHosts) {
							if (alreadyPingedList.size() >= knownHosts.size()) break; // we tested all hosts
							hostToTest = knownHosts.get( random.nextInt(knownHosts.size()));
							
						}
						if (alreadyPingedList.contains( hostToTest)) continue;
						alreadyPingedList.add( hostToTest);
						if (!hostToTest.couldBeReached()){
							continue;
						}
						
						
						try {
							
							if (Utils.isNullOrEmpty( hostToTest.getUniqueHostId())){
								logger.info("checking against node " + hostToTest + " also getting UID");
								hostToTest.ping(); // throws exception if not reachable
								if ((hostToTest.getUniqueHostId() == null) || (Utils.equalsWithNull(hostToTest.getUniqueHostId(), Backend.getUniqueHostId()))){ // we pinged ourself, could happen with wrong config
									logger.error("we got ourself, removing");
									synchronized ( knownHosts) {
										knownHosts.remove( hostToTest);
									}
									continue;
								}
								newConnectedToInternet = true;
								break;
							}
							logger.info("checking against node " + hostToTest);
							hostToTest.ping(); // throws exception if not reachable
							newConnectedToInternet = true;							
							break;
						} catch (Exception e) { // bäm
							logger.info("exception while trying node " + hostToTest.getUniqueHostId() + " " + e.getMessage());
							//e.printStackTrace();
						}
					}
					
				}
				
				if ((!newConnectedToInternet) && (!Utils.isNullOrEmpty( manualInternetAddress))){
					// if we dont have any known hosts, we define the connectedToInternet-Status if we reach our manually configured external interface
					// it seems we are the first node
					newConnectedToInternet = (testAddressIfIsMe( KnownHost.buildAddress(internetProtocol, manualInternetAddress, externalInternetPort)));
					onlyEstimatedOnline = true;
				}
				
				connectedToInternet = newConnectedToInternet;
	
			}
			finally
			{
				if (onlyEstimatedOnline) logger.info("status connectedToInternet:" + connectedToInternet + " (but only estimated) internetAddress:" + internetAddress);
				else logger.info("status connectedToInternet:" + connectedToInternet + " internetAddress:" + internetAddress);
				
				checking = false;
				/*if (connectedToInternet) {
					if (successfulConnectionChecks < 240) successfulConnectionChecks++;
					nextConnectionCheckAt = System.currentTimeMillis() + 60000 + (successfulConnectionChecks*1000);
				}
				else{
					successfulConnectionChecks = 0;
					nextConnectionCheckAt = System.currentTimeMillis() + 10000;
				}*/
				synchronized (this) {
					nextConnectionCheckAt = System.currentTimeMillis() + 10000;
				}
				
				
				// try to guess our internetIP
				if (connectedToInternet)
				{
					// only check for new internetIP every minute
					if ((internetAddress == null) && (nextCheckMyInternetIp < System.currentTimeMillis())){
						addAddressesThatCouldBeOurs( Backend.getAutoconf().getPossibleExternalNetworkAddresses());
						testAddressesThatCouldBeMine(); 
						nextCheckMyInternetIp = System.currentTimeMillis() + 60000;
					}
					
					if (isReachable() && (!testAddressIfIsMe( KnownHost.buildAddress( internetProtocol, internetAddress, getInternetPort())))){
						internetAddress = null;						
					}
					
					if (internetAddress == null){
						if ( upnpAllowed && (Backend.getAutoconf().isUPNPAvailable()) && (Backend.getDHT().getPort(true) > 0) ){ // only try upnp when DHT already selected a port
							logger.info("trying upnp");
							if (Backend.getAutoconf().mapPortsWithUPNP()){
								logger.info("upnp was successful");
								// not needed anymore
								/*, testing now: "+KnownHost.buildAddress( internetProtocol, internetAddress, getInternetPort()));
								if (!testAddressIfIsMe(KnownHost.buildAddress( internetProtocol, internetAddress, getInternetPort()))){
									internetAddress = null;
									Backend.getAutoconf().unmapUPNPPorts();
									System.out.println("failed2");
								}
								else System.out.println("success2");*/ 
							}
							else logger.info("upnp failed");
						}
					}
					
					if (!onlyEstimatedOnline){
						Backend.getDHT().manageDHT( false);
					}
				}
			
				removeAddressesFromHosts(onlyEstimatedOnline);
				manageProxyThreads();
				
				boolean changed = false;
				if (lastConnectedToInternet != connectedToInternet) changed = true;
				else if (!Utils.equalsWithNull(lastInternetAddress, internetAddress)) changed = true;
				
				if (changed){
					onConnectionStatusChanged();
				}
				
				
				lastConnectedToInternet = connectedToInternet;
				lastInternetAddress = internetAddress;
								
			}
			
		}
	}
	
	@SuppressWarnings("unchecked")
	private void testAddressesThatCouldBeMine() {
		
		ArrayList<String> addresses;
		synchronized (addressesToCheckIfOurs) {
			addresses = (ArrayList<String>) addressesToCheckIfOurs.clone();
			addressesToCheckIfOurs.clear();
		}
		
		synchronized (alreadyCheckedAddressesToBeOurs) {
			for (String address: addresses)
				if (!alreadyCheckedAddressesToBeOurs.contains( address))
					alreadyCheckedAddressesToBeOurs.add( address);
		}
		
		for(String address: addresses){
			if ((externalInternetPort > 0) && (externalInternetPort < 65536) && ( testAddressIfIsMe( KnownHost.buildAddress(internetProtocol, address, externalInternetPort))))
			{
				internetAddress = address;
				externalInternetPort = externalInternetPortFromConfig;
				return;
			}
			else if ( testAddressIfIsMe( KnownHost.buildAddress(internetProtocol, address, internetPort)))
			{
				internetAddress = address;
				externalInternetPort = internetPort;
				return;
			}
		}
	
	}

	
	public synchronized boolean testAddressIfIsMeOrNobody(String address){
		if (!Utils.isNullOrEmpty( address))
		{
			String idThatHasToMatch = java.util.UUID.randomUUID().toString().replaceAll("-", "");
			Backend.setConnectionTestId( idThatHasToMatch);
			HttpClient httpClient = new DistributedBookHttpClient();
			HttpGet httpGet = new HttpGet( address + Backend.getServletContextPath());
			httpGet.getParams().setIntParameter("http.socket.timeout", 5000);
			httpGet.setHeader("command", "getConnectionTestId");
			try
			{
				HttpResponse response = null;
				try{
					response = httpClient.execute( httpGet);
				}
				catch (Exception e) {
					return true; // is nobody
				}
				
				if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK)
				{
					HttpEntity entity = response.getEntity();
					if ( entity.getContentLength() < 0) return false;
					InputStream is = entity.getContent();
					try{
						byte[] answer = new byte[(int) Math.min( entity.getContentLength(), 50)];
						if (is.read(answer) < answer.length) return false;
						return (idThatHasToMatch.equals( new String( answer))); // is me?
					}
					finally{
						is.close();
					}
				}
				return false;
			}
			catch (Exception e) {
			}
		}
		return false;
	}
	
	protected synchronized boolean testAddressIfIsMe(String address){
		if (!Utils.isNullOrEmpty( address))
		{
			String idThatHasToMatch = java.util.UUID.randomUUID().toString().replaceAll("-", "");
			Backend.setConnectionTestId( idThatHasToMatch);
			HttpClient httpClient = new DistributedBookHttpClient();
			HttpGet httpGet = new HttpGet( address + Backend.getServletContextPath());
			httpGet.getParams().setIntParameter("http.socket.timeout", 5000);
			httpGet.setHeader("command", "getConnectionTestId");
			try
			{
				HttpResponse response = httpClient.execute( httpGet);
				if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK)
				{
					HttpEntity entity = response.getEntity();
					if ( entity.getContentLength() < 0) return false;
					InputStream is = entity.getContent();
					try{
						byte[] answer = new byte[(int) Math.min( entity.getContentLength(), 50)];
						if (is.read(answer) < answer.length) return false;
						return (idThatHasToMatch.equals( new String( answer)));
					}
					finally{
						is.close();
					}
				}
				return false;
			}
			catch (Exception e) {
			}
		}
		return false;
	}
	
/*
	private void addRemoteHosts(ArrayList<KnownHost> reachedThisHosts) {
		AsyncCommandCallback<GiveMeYourKnownHostsAddressesResponse> asyncCommandCallback = new AsyncCommandCallback<GiveMeYourKnownHostsAddressesResponse>() {

			@Override
			public void onFailure(Throwable caught) {
				// ignore
			}

			@Override
			public void onSuccess(GiveMeYourKnownHostsAddressesResponse result) {
				synchronized (BookService.getMyKnownHosts()) {
					for(KnownHost knownHost: result.getKnownHosts()){
						BookService.getMyKnownHosts().add( knownHost);
					}
				}				
			}
			
		};
		
		SimpleServerCommand request = new GiveMeYourKnownHostsAddressesRequest();
		for(KnownHost knownHost: reachedThisHosts)
		{
			knownHost.sendCommandAsync(request, true, asyncCommandCallback);
		}
	}
*/
	private synchronized void removeAddressesFromHosts( boolean ignoreThem) {		
		if (!ignoreThem && connectedToInternet && lastConnectedToInternet)
		{
			for( RemoveAddressFromHostIfOnlineItem removeAddressFromHostIfOnlineItem: removeAddressFromHostIfOnline)
			{
				try {
					removeAddressFromHostIfOnlineItem.knownHost.removeAddress( removeAddressFromHostIfOnlineItem.address);
				} catch (Exception e) {
					logger.error("error removing address", e);
				}				
			}
		}
		removeAddressFromHostIfOnline.clear();
	}

	
	private void manageProxyThreads() {
		if (connectedToInternet && !isReachable()){ // needs proxyhosts
			synchronized ( proxyConnectionThreads) {
				if (proxyConnectionThreads.size()<3){ // start new connections
					ArrayList<KnownHost> tryThisHosts = Backend.getMyKnownHosts().getKnownHostsWithHighesAccessTimeFirst( 0);
					
					for(KnownHost knownHost: tryThisHosts){
						if (knownHost.isInProxyMode()) continue;
						if (knownHost.isInForwardMode()) continue;
						if (proxyConnectionThreads.size()>2) break;
						boolean alreadyConnected = false;
						for (ProxyConnectionThread proxyConnectionThread: proxyConnectionThreads)
							if (proxyConnectionThread.getProxyHost().equals( knownHost))
							{
								alreadyConnected= true;
								break;
							}
						if (!alreadyConnected)
						{
							knownHost.setAskedForBeingMyProxy();
							ProxyConnectionThread proxyConnectionThread = new ProxyConnectionThread(knownHost);
							proxyConnectionThreads.add( proxyConnectionThread);
							proxyConnectionThread.setDaemon( true);
							proxyConnectionThread.start();
							logger.info("Started proxy connection thread to " + knownHost.getUniqueHostId());
						}
					}
				}
			}
		}
		else // no need of proxy hosts
		{
			synchronized ( proxyConnectionThreads) {
				if (proxyConnectionThreads.size() > 0){ 
					for (ProxyConnectionThread proxyConnectionThread: proxyConnectionThreads){
						proxyConnectionThread.interrupt();
					}
				}
			}
		}
		
		
	}


	public ConnectionStatus(int myPort) {
		super();
		this.internetPort = myPort;
		this.externalInternetPort = Backend.getConfiguration().getExternalPort();
		
		TimerTask task = new TimerTask() 
		{
			
			@Override
			public void run() {
				doConnectionWork();
			}
		};
		testConnectionTimer = new java.util.Timer("test connection timer");
		testConnectionTimer.schedule(task, 1000, 1000);
		
		manualInternetAddress = Backend.getConfiguration().getManualExternalHost();
		externalInternetPortFromConfig = Backend.getConfiguration().getExternalPort();
		externalInternetPort = externalInternetPortFromConfig;
		if ((externalInternetPort < 1) || (externalInternetPort > 65535)) externalInternetPort = myPort;
		upnpAllowed = Backend.getConfiguration().isUPNPActive();
	}

	public int getInternetPort() {
		return getInternetPort( false);
	}

	public int getInternetPort( boolean ignoreDifferenExternalPort) {
		if (!ignoreDifferenExternalPort && (externalInternetPort>-1)) return externalInternetPort;
		return internetPort;
	}


	public void setInternetPort(int internetPort) {
		this.internetPort = internetPort;
	}


	public void setExternalInternetPort(int externalInternetPort) {
		this.externalInternetPort = externalInternetPort;
	}

	public String getInternetAddress() {
		return internetAddress;
	}


	public void setInternetAddress(String internetAddress) {
		this.internetAddress = internetAddress;
	}


	public boolean isNeedsBootstrap() {
		return needsBootstrap;
	}


	public String getInternetProtocol() {
		return internetProtocol;
	}

	public int getExternalInternetPort() {
		return externalInternetPort;
	}

	public void close() {
		releaseProxyConnections();
		if (testConnectionTimer != null){
			testConnectionTimer.cancel();
			testConnectionTimer = null;
		}
	}

	private void releaseProxyConnections() {
		ProxiedHosts proxiedHosts = Backend.getProxiedHosts();
		if (proxiedHosts != null) proxiedHosts.clear();
		for(ProxyConnectionThread proxyConnectionThread: proxyConnectionThreads)
			proxyConnectionThread.interrupt();
	}
	
	
}
