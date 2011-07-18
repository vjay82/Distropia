package org.distropia.server.communication.lowlevel;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class NetworkAddresses extends ArrayList<NetworkAddress> {
	public boolean containsIP( String ip){
		for (NetworkAddress networkAddress: this)
			if (networkAddress.getIp().equals( ip)) return true;
		return false;
	}
	
	public NetworkAddress getWhichIsInSameNetworkWith( String ip){
		for (NetworkAddress networkAddress: this)
			if (networkAddress.isInSameNetwork(ip))
					return networkAddress;
		return null;
	}
}
