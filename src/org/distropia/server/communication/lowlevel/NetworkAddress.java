package org.distropia.server.communication.lowlevel;

import java.net.InetAddress;
import java.util.BitSet;

public class NetworkAddress {
	protected String ip;
	protected int netmask;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getNetmask() {
		return netmask;
	}
	public void setNetmask(int netmask) {
		this.netmask = netmask;
	}
	public NetworkAddress(String ip, int netmask) {
		super();
		this.ip = ip;
		this.netmask = netmask;
	}
	public NetworkAddress() {
		super();
	}
	
	public static BitSet fromByteArray(byte[] bytes) {
	    BitSet bits = new BitSet();
	    for (int i=0; i<bytes.length*8; i++) {
	        if ((bytes[i/8]&(1<<(7-(i%8)))) > 0) {
	            bits.set(i);
	        }
	    }
	    return bits;
	}
	
	public boolean isInSameNetwork( String ip){
		try{
			
			byte[] b1 = InetAddress.getByName(this.ip).getAddress();
			byte[] b2 = InetAddress.getByName(ip).getAddress();
			
			if (b1.length != b2.length){
				//System.out.println("c " + this.ip  + " "+ip + " but not same length.");
				return false;
			}
			
			BitSet ip1 = fromByteArray( b1);
			BitSet ip2 = fromByteArray( b2);
			
			BitSet networkMask = new BitSet( ip1.length());
			networkMask.set(0, Math.max( netmask-1, 0));
			ip1.and( networkMask);
			ip2.and( networkMask);
			
//			System.out.println("c " + this.ip  + " -> " + ip1.toString() + " netmask is: " + netmask);
//			System.out.println("w " + ip + " -> " + ip2.toString());
//			System.out.println("result: " + (ip1.equals( ip2)));
			
			return (ip1.equals( ip2));
		}
		catch (Exception e) {
			return false;
		}
	}
	@Override
	public String toString() {
		return ip+"/"+netmask;
	}
	
	
}
