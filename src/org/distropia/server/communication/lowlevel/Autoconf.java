package org.distropia.server.communication.lowlevel;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Random;

import net.tomp2p.upnp.InternetGatewayDevice;

import org.distropia.client.Utils;
import org.distropia.server.Backend;
import org.distropia.server.Maintenanceable;
import org.distropia.server.communication.KnownHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Autoconf implements Maintenanceable {
	protected static final Logger logger = LoggerFactory.getLogger(Autoconf.class);
	/**
	 * This list contains all local network addresses + netmasks
	 */
	protected NetworkAddresses localNetworkAddresses = new NetworkAddresses();
	
	/**
	 * This list contains all possible addresses, that could be our public address in the following order
	 * 1. if manually configured in config file
	 * 2. detected by UPNP
	 * 3. all localAddresses, that are not private addresses
	 */
	protected ArrayList<String> possibleExternalNetworkAddresses = new ArrayList<String>();
	
	protected long nextAutodiscover = 0;
	protected boolean upnpAvailable = false;
	protected NetworkAddresses privateNetworkAddresses = new NetworkAddresses();
	protected String upnpMappingActiveForIP = null;
	protected long upnpBlockedUntil = 0;
	
	public synchronized NetworkAddresses getLocalNetworkAddresses(){
		return (NetworkAddresses) localNetworkAddresses.clone();
	}
	
	@SuppressWarnings("unchecked")
	public synchronized ArrayList<String> getPossibleExternalNetworkAddresses() {
		updateNetworkAddresses();
		return (ArrayList<String>) possibleExternalNetworkAddresses.clone();
	}

	public Autoconf() {
		super();
		privateNetworkAddresses.add( new NetworkAddress("10.0.0.0", 8));
		privateNetworkAddresses.add( new NetworkAddress("172.16.0.0", 12));
		privateNetworkAddresses.add( new NetworkAddress("192.168.0.0", 16));
		privateNetworkAddresses.add( new NetworkAddress("127.0.0.1", 8));
	}

	public boolean isThisAPrivateNetwork( String ip){
		if (Backend.DEBUG_DISABLE_PRIVATE_IP_CHECK) return false;
		for(NetworkAddress networkAddress: privateNetworkAddresses)
			if (networkAddress.isInSameNetwork(ip)) return true;
		return false;
	}
	
	public synchronized boolean isOneOfMyLocalIPs( String ip){
		updateNetworkAddresses();
		return localNetworkAddresses.containsIP(ip);
	}
	
	public synchronized boolean isOneOfMyPossibleInternetIPs( String ip){
		updateNetworkAddresses();
		return possibleExternalNetworkAddresses.contains(ip);
	}
	
	public synchronized boolean isUPNPAvailable(){
		updateNetworkAddresses();
		if (upnpBlockedUntil > System.currentTimeMillis()) return false;
		return upnpAvailable;
	}
	
	protected synchronized void updateNetworkAddresses(){
		if (nextAutodiscover > System.currentTimeMillis()) return;
		nextAutodiscover = System.currentTimeMillis() + 60000;
		
		localNetworkAddresses.clear();
		try {
			Enumeration<NetworkInterface> netInter = NetworkInterface.getNetworkInterfaces();
			while ( netInter.hasMoreElements() ) 
			{ 
			  NetworkInterface ni = netInter.nextElement(); 
			  for ( InterfaceAddress iAddress : ni.getInterfaceAddresses() ) 
			  {
				  String ip = iAddress.getAddress().getHostAddress();
				  int netmask = iAddress.getNetworkPrefixLength();
				  if (netmask == 0) // dont allow this, or every networktest will result in true
					  netmask = 32; // results in ignore
				  localNetworkAddresses.add(new NetworkAddress( ip, netmask));			  
			  } 
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} 		 
		
		try {
			InetAddress[] adrs = InetAddress.getAllByName(InetAddress.getLocalHost().getHostAddress());
			for (int index = 0; index < adrs.length; index++)
			{
				if (!localNetworkAddresses.containsIP(adrs[index].getHostAddress()))
					localNetworkAddresses.add( new NetworkAddress( adrs[index].getHostAddress(), 32));
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		possibleExternalNetworkAddresses.clear();
		for(NetworkAddress networkAddress: localNetworkAddresses){
			if (isThisAPrivateNetwork( networkAddress.getIp())){
				possibleExternalNetworkAddresses.add( networkAddress.getIp());
			}
		}
		
		try {
			Collection<InternetGatewayDevice> IGDs = InternetGatewayDevice.getDevices(-1);
			if (IGDs == null) return;
			for (InternetGatewayDevice igd : IGDs)
			{
				upnpAvailable = true;
				String routerIP = igd.getExternalIPAddress();
				
				if (!possibleExternalNetworkAddresses.contains( routerIP))
					possibleExternalNetworkAddresses.add(0, routerIP);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		String manualExternalHost = Backend.getConfiguration().getManualExternalHost();
		if (!Utils.isNullOrEmpty( manualExternalHost)){
			try {
				possibleExternalNetworkAddresses.add(0, InetAddress.getByName( manualExternalHost).getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			
		}		
	}
	
	public synchronized void unmapUPNPPorts(){
		upnpMappingActiveForIP = null;
	}
	
	public synchronized boolean mapPortsWithUPNP() {
		if (internalMapPortsWithUPNP()) return true;
		upnpBlockedUntil = System.currentTimeMillis() + 60000*5;
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public boolean getIsPortOpened(String address, int port){
		try {
			Socket socket = null;
			try{
				socket = new Socket( address, port, true);
				return true;				
			} finally{
				if (socket != null) socket.close();
			}
		} catch (Exception e) {
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public String getLocalAddressUsedForContacting(String address){
		try {
			Socket socket = null;
			try{
				socket = new Socket( address, 80, false);
				String result = socket.getLocalAddress().toString();
				if (!Utils.isNullOrEmpty( result) && result.length() > 0 && result.charAt(0) == '/') return result.substring(1);
				return result;
			} finally{
				if (socket != null) socket.close();
			}
		} catch (Exception e) {
			logger.error("could not find out with which address i would connect to " + address);
		}
		return null;
	}
	
	private boolean internalMapPortsWithUPNP() {
		updateNetworkAddresses();
		logger.info("trying to start upnp mapping");
		try
		{
			Collection<InternetGatewayDevice> IGDs = InternetGatewayDevice.getDevices(-1);
			if (IGDs == null){
				logger.error("didn't found any devices");
				return false;
			}
			for (InternetGatewayDevice igd : IGDs)
			{
				String routerExternalAddress = igd.getExternalIPAddress();
				String routerInternalAddress = igd.getIGDRootDevice().myIP;
				if ((routerInternalAddress != null) || (routerExternalAddress != null)) // we can use both
				{
					if (!Utils.isNullOrEmpty(routerInternalAddress) && routerInternalAddress.length() > 0 && routerInternalAddress.charAt(0) == '/') routerInternalAddress = routerInternalAddress.substring(1);					
					String routerUseAddress = routerExternalAddress;
					if (Utils.isNullOrEmpty( routerUseAddress)) routerUseAddress = routerInternalAddress;
					logger.info("trying to map ports with device " + routerUseAddress);
					String myLocalNetworkAddress = getLocalAddressUsedForContacting( routerUseAddress);
							
					if (Utils.isNullOrEmpty(myLocalNetworkAddress) && !Utils.isNullOrEmpty(routerInternalAddress)){ // try to find out ourself
						NetworkAddress networkAddress = localNetworkAddresses.getWhichIsInSameNetworkWith( routerInternalAddress);
						if (networkAddress != null) myLocalNetworkAddress = networkAddress.getIp();
					}
							
					
					if ( Utils.isNullOrEmpty( myLocalNetworkAddress)) logger.error("did not found an ip in the same network with " + routerInternalAddress);
					else {
						
						logger.info("myIP: "+ myLocalNetworkAddress);
						if (myLocalNetworkAddress == null) return false;
						
						if (Utils.equalsWithNull( upnpMappingActiveForIP, routerUseAddress))
						{
							boolean success = igd.addPortMapping( Backend.APPLICATION_NAME, "TCP", "", Backend.getConnectionStatus().getExternalInternetPort(), myLocalNetworkAddress, Backend.getConnectionStatus().getInternetPort(true), 600) && // get it for 10 minutes
									  		  igd.addPortMapping( Backend.APPLICATION_NAME, "TCP", "", Backend.getDHT().getExternalPort(), myLocalNetworkAddress, Backend.getDHT().getPort(true), 600) &&
									          igd.addPortMapping( Backend.APPLICATION_NAME, "UDP", "", Backend.getDHT().getExternalPort(), myLocalNetworkAddress, Backend.getDHT().getPort(true), 600);
							if (!success) // UPNP renewal failed
							{
								upnpMappingActiveForIP = null;
								Backend.getMaintenanceList().remove( this);
								logger.error("error refreshing UPNP mapping");
							}
							else logger.info("refreshed UPNP mapping");
						}
						else
						{
							// at first time we probe the device
							boolean success = false;
							int externalInternetPort = Backend.getCommunicationDatabase().getPropertyInt("lastUPNPMappedInternetPort", Backend.getConnectionStatus().getExternalInternetPort());
							if ((externalInternetPort < 1) || (externalInternetPort > 65535)) externalInternetPort = Backend.getConnectionStatus().getInternetPort();
							for(int index=0; index<5; index++){
								// check if address is free or already taken by us
								success = Backend.getConnectionStatus().testAddressIfIsMeOrNobody( KnownHost.buildAddress(Backend.getConnectionStatus().getInternetProtocol(), routerUseAddress, externalInternetPort));
								if (!success) logger.error("port " + externalInternetPort + " is already used by sb. else");
								else { // take address
									success = igd.addPortMapping( Backend.APPLICATION_NAME, "TCP", "", externalInternetPort, myLocalNetworkAddress, Backend.getConnectionStatus().getInternetPort(), 600);
									if (!success) logger.error("port " + externalInternetPort + " ist already used by sb. else");
									else{ // check if we did that successfully
										success = Backend.getConnectionStatus().testAddressIfIsMeOrNobody( KnownHost.buildAddress(Backend.getConnectionStatus().getInternetProtocol(), routerUseAddress, externalInternetPort));
										if (!success) logger.error("port " + externalInternetPort + " is not me...");
									}
								}
								// otherwise take new random port and do again
								if (!success) externalInternetPort = (new Random()).nextInt(64510) + 1025;
								else break;
							}
							
							if (success){
								Backend.getCommunicationDatabase().setPropertyInt("lastUPNPMappedInternetPort", externalInternetPort);
								int externalDHTPort = Backend.getCommunicationDatabase().getPropertyInt("lastUPNPMappedDHTPort", Backend.getDHT().getPort());
								if ((externalDHTPort < 1) || (externalDHTPort > 65535)) externalDHTPort = Backend.getDHT().getPort(true);
								if ((externalDHTPort < 1) || (externalDHTPort > 65535)) externalDHTPort = (new Random()).nextInt(64510) + 1025;
								success = false;
								for(int index=0; index<15; index++){
									// we test everything by ourself, never trust a router ;)
									if ( getIsPortOpened( routerUseAddress, externalDHTPort)) // port already opened
									{
										// the trick is here to check if it is our port to just deregister it (we don't want to shut down the DHT)
										igd.deletePortMapping(myLocalNetworkAddress, externalDHTPort, "TCP");
										igd.deletePortMapping(myLocalNetworkAddress, externalDHTPort, "UDP");
										// if now the port is still open... it is not ours
										if ( getIsPortOpened( routerUseAddress, externalDHTPort)){
											externalDHTPort = (new Random()).nextInt(64510) + 1025;
											continue;
										}
									}
									success = igd.addPortMapping( Backend.APPLICATION_NAME, "TCP", "", externalDHTPort, myLocalNetworkAddress, Backend.getDHT().getPort( true), 600) &&
								              igd.addPortMapping( Backend.APPLICATION_NAME, "UDP", "", externalDHTPort, myLocalNetworkAddress, Backend.getDHT().getPort( true), 600);
									// now it has to be opened
									//System.out.println("checking used extdhtport: " + externalDHTPort);
									//if (success) success = getIsPortOpened( routerUseAddress, externalDHTPort);
									//System.out.println("checking used extdhtport: " + success);
									if (!success) externalDHTPort = (new Random()).nextInt(64510) + 1025;
									else break;
								}
								if (success){
									logger.info("UPNP was successful, used external port " + externalInternetPort + " for webserver and port " + externalDHTPort + " for DHT.");
									Backend.getCommunicationDatabase().setPropertyInt("lastUPNPMappedDHTPort", externalDHTPort);
									Backend.getConnectionStatus().setExternalInternetPort( externalInternetPort);
									Backend.getDHT().setExternalPort(externalDHTPort);
									if (!Utils.isNullOrEmpty( routerExternalAddress)){
										Backend.getConnectionStatus().setInternetAddress( routerExternalAddress);										
									}
									upnpMappingActiveForIP = routerUseAddress;
									Backend.getMaintenanceList().addWithWeakReference( this, 300000);
									logger.info("started UPNP mapping");
									return true;
								}
								else{
									logger.error("Was not able to open external port for DHT");
									igd.deletePortMapping(myLocalNetworkAddress, externalInternetPort, "TCP");
								}
							}
							else logger.error("Was not able to open external port for webserver");
						}
						
						
					}					
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("error starting upnp mapping", e);
		}
		return false;
	}

	@Override
	public void maintenance() {
		if (upnpMappingActiveForIP != null) mapPortsWithUPNP();
		else{
			Backend.getMaintenanceList().remove( this);
		}
		
	}
	
}
