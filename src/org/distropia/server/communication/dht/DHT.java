package org.distropia.server.communication.dht;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.Bindings.Protocol;
import net.tomp2p.futures.BaseFutureListener;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureData;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.config.ConfigurationGet;
import net.tomp2p.p2p.config.ConfigurationStore;
import net.tomp2p.p2p.config.Configurations;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.peers.PeerMapChangeListener;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

import org.apache.xerces.impl.dv.util.Base64;
import org.apache.xerces.util.URI;
import org.distropia.client.PublicUserCredentials;
import org.distropia.server.Backend;
import org.distropia.server.communication.GiveMeYourDHTPortRequest;
import org.distropia.server.communication.GiveMeYourDHTPortResponse;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;
import org.distropia.server.database.CommunicationDatabase;
import org.distropia.server.database.UserProfile;
import org.distropia.server.database.UserProfiles;
import org.distropia.server.platformspecific.PlatformWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DHT {
	public static final Number160 DOMAIN_HOST_ADDRESS = new Number160(0);
	public static final Number160 DOMAIN_USER = new Number160(1);
	
	protected static final Logger logger = LoggerFactory.getLogger(DHT.class);
	protected Peer peer = null;
	protected int port = 0;
	protected String storageDirectory = null;
	protected CommunicationDatabase communicationDatabase;
	protected volatile int externalPort = -1;
	//protected boolean passiveMode = false;
	protected boolean wasCreatedLocally = true;
	protected int wasCreatedWithPort = 0;
	protected String wasCreatedWithExternalInetAddress = null;
	protected Number160 peerKey;
	
	public boolean isActive(){
		return !needsBootstrap();
	}
	
	/**
	 * This is in progress.
	 * @param dontBootstrap
	 */
	public void manageDHT( boolean dontBootstrap) {
		
		if (port < 1) port = Backend.getConnectionStatus().getInternetPort(true)+1;
		if ((externalPort < 1) || (externalPort>65535)) externalPort = port;
		
		boolean changed = false;
		if ((peer != null) && (peer.getConnectionBean() != null)) // if settings differ, modify
		{
			if ((Backend.getConnectionStatus().getInternetAddress() != null) && !Backend.getConnectionStatus().isConnectedToInternetDirectly() && (externalPort > -1))
			{
				//System.out.println("manage - IM BEHIND A IP ");
				if (wasCreatedLocally) changed = true;
				// TODO: fix the following commented line, because of bouncing ip/dns it could cause problems
				//if (!Backend.getConnectionStatus().getInternetAddress().equals(wasCreatedWithExternalInetAddress)) changed = true;
				if (wasCreatedWithPort != externalPort) changed = true;
			}
			else
			{
				//System.out.println("manage - IM LOCAL ");
				if (!wasCreatedLocally) changed = true;
				/*else{
					try {
						InetAddress inetAddress = InetAddress.getByName( Backend.getConnectionStatus().getInternetAddress());
						if (!peer.getPeerBean().getServerPeerAddress().getInetAddress().equals( inetAddress)) changed = true;
					}
					catch (Exception e) {
					}
				}*/
			}		
		}
		else changed = true;
		
		//System.out.println("manage - i will recreate: " + changed);
		
		// create new
		if (changed)
		{
			
			if (peer != null)
			{
				peer.shutdown();			
			}
			
			peer = new Peer( peerKey);
			peer.getConnectionConfiguration().setEnabledUPNPNAT( false);
			
			for(int index = 0; index < 5; index++)
			{
				try {
					
					if ((Backend.getConnectionStatus().getInternetAddress() != null) && !Backend.getConnectionStatus().isConnectedToInternetDirectly() && (externalPort > -1))
					{
						//peer.
						wasCreatedLocally = false;
						wasCreatedWithPort = externalPort;
						wasCreatedWithExternalInetAddress = Backend.getConnectionStatus().getInternetAddress();
						logger.info("recreating peer because of a change - running on " + Backend.getConnectionStatus().getInternetAddress() + " at port " + externalPort);
						
						if (Backend.getPlatformSpecific() instanceof PlatformWindows){
							// temporary workaround, it seems windows 7 has problems, if a network interface is not of the same protocol family
							// TODO: find a better solution
							Protocol protocol = Protocol.IPv4;
							if (Backend.getConnectionStatus().getInternetAddress().contains(":")) protocol = Protocol.IPv6;									
							Bindings b = new Bindings( protocol);
							b.setOutsideAddress( InetAddress.getByName( Backend.getConnectionStatus().getInternetAddress()), externalPort, externalPort);
							peer.listen( port, port, b);
						}
						else{
							Bindings b = new Bindings( false);
							b.setOutsideAddress( InetAddress.getByName( Backend.getConnectionStatus().getInternetAddress()), externalPort, externalPort);
							peer.listen( port, port, b);
						}
						
						
					}
					else {
						peer.listen( port, port);
						wasCreatedLocally = true;
						logger.info("recreating peer because of a change - running on localhost at port " + port);
					}
					
					peer.getPeerBean().setStorage( new DHTStorage( storageDirectory));
					peer.getPeerBean().getPeerMap().addPeerMapChangeListener(new PeerMapChangeListener() {
						
						@Override
						public void peerUpdated(PeerAddress peerAddress) {
							// ignore
						}
						
						@Override
						public void peerRemoved(PeerAddress peerAddress) {
							try {
								communicationDatabase.deletePeerAddress(peerAddress);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						
						@Override
						public void peerInserted(PeerAddress peerAddress) {
							try {
								communicationDatabase.setPeerAddress(peerAddress);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					
					peer.setObjectDataReply(new ObjectDataReply() {
						
						@Override
						public Object reply(PeerAddress sender, Object request) throws Exception {
							return onObjectRequest(sender, request);
						}
					});
					
					break;
				} catch (Exception e) {
					e.printStackTrace();
					port = (new Random()).nextInt(64510) + 1025;
				}
			}
			
			if (!peer.isListening()){
				logger.error("creating peer failed!");
				peer.shutdown();
				peer = null;
			}
		}
		
		
		
		
		if (!dontBootstrap && needsBootstrap() && Backend.getConnectionStatus().isConnectedToInternet())
		{
			bootstrapFromKnownHosts( Backend.getMyKnownHosts().getKnownHostsWithHighesAccessTimeFirstWhichAreDirectlyAccessible( 10));
		}
	}
	
	
	public int getContacts()
	{
		if ((peer == null) || !peer.isListening()) return 0;
		return peer.getPeerBean().getPeerMap().size();		
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getAddressesOfKnownHost( String uniqueHostId) throws Exception{
		if (uniqueHostId == null) return null;
		if (getContacts() > 0) 
		{
			ConfigurationGet cg1 = Configurations.defaultGetConfiguration();
			Number160 number = Number160.createHash( uniqueHostId);
			cg1.setDomain( DOMAIN_HOST_ADDRESS);
			cg1.setContentKey( number);
			FutureDHT future = peer.get( number, cg1);
			future.await(10000);
			if(future.isSuccess()) {
				Iterator<Data> it = future.getData().values().iterator();
			    while (it.hasNext()) {
			        Data data = it.next();
			        Object o = data.getObject();
			        if (o instanceof ArrayList)
			         return (ArrayList<String>) data.getObject();
			    }
			}
		}
		return null;
	}
	
	public boolean publishMyAddresses( ArrayList<String> addresses){		
		if ((peer != null) && (peer.isListening())) {
			logger.info("Publishing my addresses.");
			try {
				Data data = new Data( addresses);
				
				ConfigurationStore cs1 = Configurations.defaultStoreConfiguration();
				Number160 number = Number160.createHash( Backend.getUniqueHostId());
				cs1.setProtectDomain(false);
				cs1.setDomain( DOMAIN_HOST_ADDRESS);
				cs1.setContentKey( number);
				FutureDHT future = peer.put( number, data, cs1);
				future.addListener(new BaseFutureListener<FutureDHT>() {

					@Override
					public void operationComplete(FutureDHT future)
							throws Exception {
						if (future.isSuccess())
							logger.info("Put succeeded");
						else 
							logger.error("Put failed because: " + future.getFailedReason());						
					}

					@Override
					public void exceptionCaught(Throwable t) throws Exception {
						logger.error("Put failed", t);
						
					}
				});
				future.await(10000);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}			
			
		}
		return false;
	}
	
	public DHT( CommunicationDatabase communicationDatabase, String storageDirectory) throws Exception {
		super();
		this.storageDirectory = storageDirectory;
		this.communicationDatabase = communicationDatabase;
		String key = communicationDatabase.getPropertyString("dhtKey", null);
		if (key == null)
		{
			key = Base64.encode( Number160.createHash( Backend.getUniqueHostId()).toByteArray());
			communicationDatabase.setPropertyString("dhtKey", key);
		}
		peerKey = new Number160( Base64.decode( key));
		externalPort = Backend.getConfiguration().getExternalDHTPort();
	}
	
	private Object onObjectRequest( PeerAddress sender, Object request) throws Exception{
		if (request instanceof GiveMeYourPortRequest) return new GiveMeYourPortResponse(Backend.getConnectionStatus().getInternetPort(), Backend.getUniqueHostId());
		return null;
		
	}
	
	public void close(){
		if (peer != null){
			peer.shutdown();
			peer = null;
		}
	}

	public int getPort() {
		return getPort( false);
	}

	public int getPort( boolean ignoreDifferenExternalPort) {
		if (!ignoreDifferenExternalPort && ( externalPort>-1)) return externalPort;
		return port;
	}	
	
	public int getExternalPort() {
		return externalPort;
	}

	public boolean needsBootstrap(){
		return getContacts() == 0;
	}
	
	public void debug(){
		boolean b = false;
		PeerMap pm = peer.getPeerBean().getPeerMap();
		synchronized (pm) {
			Collection<PeerAddress> c = pm.getAll();
			synchronized (c) {
				for(PeerAddress p: c){
					b = true;
					System.out.println(p.getInetAddress().getHostAddress() + ":"+p.portTCP());
				}
			}
		}
		if (!b) System.out.println("no hosts");
	}
	
	public KnownHost getKnownHostOfPeerAddress(PeerAddress peerAddress){
		try{
			FutureData future = peer.send(peerAddress, new GiveMeYourPortRequest());
			future.await(5000);
			Object o = future.getObject();
			if (o instanceof GiveMeYourPortResponse)
			{
				String address = "http://" + peerAddress.getInetAddress().getHostAddress() + ":" + ((GiveMeYourPortResponse)o).getPort();
				logger.info("created knownHost form peerAddress " + peerAddress + ". Address: " + address);
				KnownHosts knownHosts = Backend.getMyKnownHosts();
				KnownHost knownHost = knownHosts.getKnownHostOrNull( ((GiveMeYourPortResponse)o).getUniqueHostId());
				if (knownHost == null)
				{
					knownHost = new KnownHost( ((GiveMeYourPortResponse)o).getUniqueHostId());
					knownHost.addAddress("http://" + peerAddress.getInetAddress().getHostAddress() + ":" + ((GiveMeYourPortResponse)o).getPort());
					knownHost.updateLastAccess();
					synchronized (knownHosts) {
							if (!knownHosts.contains( knownHost))
								knownHosts.add( knownHost);
							else return knownHosts.getKnownHostOrNull( ((GiveMeYourPortResponse)o).getUniqueHostId());
					}
					
				}
				else
				{
					knownHost.addAddress("http://" + peerAddress.getInetAddress().getHostAddress() + ":" + ((GiveMeYourPortResponse)o).getPort());					
				}
				return knownHost;
			}
		}
		catch (Exception e) {
			logger.error("Error sending GiveMeYourPortRequest to " + peerAddress, e);
		}
		return null;
	}
	
	public boolean bootstrapFromPeerAddress( ArrayList<PeerAddress> peerAddresses){
		if ((peer == null) || (!peer.isListening())){
			logger.error("Aborting Bootstrap because peer is null, or not listening!");
			return false;
		}		
		
		logger.info("Bootstrapping DHT");
		try 
		{ 
			FutureBootstrap future = peer.bootstrap( peerAddresses);
			future.await(5000);
			if (future.isSuccess()) logger.info("Bootstrapping DHT succeeded, contacts now: " + getContacts());
			else logger.error("Bootstrapping failed because: " + future.getFailedReason());
			
		} catch (Exception e) {
		}
		
		return !needsBootstrap();
	}
	
	
	public boolean bootstrapFromInetSocketAddress( ArrayList<InetSocketAddress> inetSocketAddresses){
		if ((peer == null) || (!peer.isListening())){
			logger.error("Aborting Bootstrap because peer is null, or not listening!");
			return false;
		}		
		
		logger.info("Bootstrapping DHT");
		for(InetSocketAddress inetSocketAddress: inetSocketAddresses){
			try 
			{ 
				logger.info("trying to bootstrap from: " + inetSocketAddress);
				FutureBootstrap future = peer.bootstrap( inetSocketAddress);
				future.await(5000);
				if (future.isSuccess()) logger.info("Bootstrapping DHT from " + inetSocketAddress.getAddress() + " succeeded, contacts now: " + getContacts());
				else logger.error("Bootstrapping from knownHost " + inetSocketAddress.getAddress() + " failed because: " + future.getFailedReason());
				
			} catch (Exception e) {
			}
		}
		return !needsBootstrap();
	}
	
	public Collection<PeerAddress> getKnownPeers(){
		return peer.getPeerBean().getPeerMap().getAll();
	}
	

	public boolean bootstrapFromKnownHosts( ArrayList<KnownHost> knownHosts){
		ArrayList<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>( knownHosts.size());
		
		for(KnownHost knownHost: knownHosts){
			if (knownHost.isInForwardMode() || knownHost.isInProxyMode() || !knownHost.couldBeReached()) continue;
			try{
				GiveMeYourDHTPortResponse giveMeYourDHTPortResponse = (GiveMeYourDHTPortResponse) knownHost.sendCommand(new GiveMeYourDHTPortRequest());
				if (giveMeYourDHTPortResponse != null){
					List<String> hostAddresses = knownHost.getAddresses();
					synchronized (hostAddresses) {
						for( String address: hostAddresses){
							if (!address.startsWith("uid://")){				
								URI uri = new URI( address);
								addresses.add(new InetSocketAddress( InetAddress.getByName( uri.getHost()), giveMeYourDHTPortResponse.getDhtPort()));								
							}
						}
					}
				}
			}
			catch (Exception e) {
				
			}
			
			
		}
		return bootstrapFromInetSocketAddress( addresses);
	}


	public void setExternalPort(int externalPort) {
		this.externalPort = externalPort;
	}
	
	public List<PublicUserCredentials> searchUser( String name){
		
		
		List<PublicUserCredentials> result = new ArrayList<PublicUserCredentials>();
		
		Number160 locationKey = Number160.createHash( name);
		logger.info("searching for " + name + " key: " + locationKey.toString());
		
		ConfigurationGet config = Configurations.defaultGetConfiguration();
		config.setDomain( DOMAIN_USER);
		config.setContentKey( locationKey);
		
		FutureDHT future = peer.get(locationKey, config);
		
		try {
			future.await(10000);
			if (future.isSuccess()){
				Collection<Data> futureResult = future.getData().values();
				logger.info("searching for " + name + " got " + futureResult.size() + " results");
				for(Data data: futureResult){
					@SuppressWarnings("unchecked")
					List<PublicUserCredentials> resultList = (List<PublicUserCredentials>) data.getObject();
					for(PublicUserCredentials puc: resultList){
						if (!result.contains( puc)) result.add( puc);
					}
					
				}
			}
			else logger.error("search failed");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error( "error searching user " + name, e);
		}
		
		return result;
	}

	public void pushUsers() {
		if ((peer == null) || (!peer.isListening())) return;
		logger.info("publishing users");
		
		UserProfiles userProfiles = Backend.getInstance().getUserProfiles();
		if (userProfiles == null) return; 
		List<PublicUserCredentials> publicUserCredentials = new ArrayList<PublicUserCredentials>();
		synchronized (userProfiles) {
			for(UserProfile userProfile: userProfiles){
				try
				{
					PublicUserCredentials puc = userProfile.getPublicUserCredentials();
					if ( puc != null) publicUserCredentials.add( puc);
				}
				catch (Exception e) {
					logger.error("unable to get public user credentials for an user", e);
				}
			}
		}
		
		// here we have a list of everything we have to publish
		
		
		for(PublicUserCredentials puc: publicUserCredentials){
			try{
				Data data = new Data( puc);
			
				// first push runs with uniqueID
				logger.info("publishing user uniqueUserID: " + puc.getUniqueUserId());
				Number160 pushingAtKey = Number160.createHash( puc.getUniqueUserId());
				ConfigurationStore cs = Configurations.defaultStoreConfiguration();
				cs.setProtectDomain(false);
				cs.setDomain( DOMAIN_USER);
				cs.setContentKey( pushingAtKey);
				peer.put( pushingAtKey, data, cs);
				
				if (puc.getFirstName() != null && (puc.getSurName() != null)){ // pushing with name
					logger.info("publishing user fn: " + puc.getFirstName() + " sn: " + puc.getSurName());
					pushingAtKey = Number160.createHash( puc.getFirstName() + " " + puc.getSurName());
					cs = Configurations.defaultStoreConfiguration();
					cs.setProtectDomain(false);
					cs.setDomain( DOMAIN_USER);
					cs.setContentKey( pushingAtKey);
					peer.put( pushingAtKey, data, cs);
				}
			}
			catch (Exception e) {
				logger.error("error pushing an user", e);
			}
		}		
	}
	
	
}
