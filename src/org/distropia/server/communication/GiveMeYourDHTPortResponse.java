package org.distropia.server.communication;


public class GiveMeYourDHTPortResponse extends DefaultServerResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2715024852911503551L;
	int dhtPort;

	public int getDhtPort() {
		return dhtPort;
	}

	public void setDhtPort(int dhtPort) {
		this.dhtPort = dhtPort;
	}

	public GiveMeYourDHTPortResponse(int dhtPort) {
		super();
		this.dhtPort = dhtPort;
	}

	public GiveMeYourDHTPortResponse() {
		super();
	}
	
}
