package org.distropia.client;

public class BootstrapRequest extends DefaultRequest {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3648356484130007067L;
	String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public BootstrapRequest() {
		super();
	}

	public BootstrapRequest(String sessionId) {
		super(sessionId);
	}

	public BootstrapRequest(String sessionId, String address) {
		super(sessionId);
		this.address = address;
	}
	
}
