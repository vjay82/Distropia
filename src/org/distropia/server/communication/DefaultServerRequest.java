package org.distropia.server.communication;


public class DefaultServerRequest extends CommunicationObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3677567177040492292L;
	String yourAddress;

	public String getYourAddress() {
		return yourAddress;
	}

	public void setYourAddress(String yourAddress) {
		this.yourAddress = yourAddress;
	}
	
}
