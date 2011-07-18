package org.distropia.server.communication;

import java.util.ArrayList;

public class HereAreMyAddressesRequest extends DefaultServerRequest{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2039235673602032262L;
	protected ArrayList<String> addresses = null;
	public ArrayList<String> getAddresses() {
		return addresses;
	}
	public void setAddresses(ArrayList<String> addresses) {
		this.addresses = addresses;
	}
	public HereAreMyAddressesRequest(ArrayList<String> addresses) {
		super();
		this.addresses = addresses;
	}
	public HereAreMyAddressesRequest() {
		super();
	}

	
}
