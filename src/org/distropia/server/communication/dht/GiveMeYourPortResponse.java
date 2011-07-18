package org.distropia.server.communication.dht;

import java.io.Serializable;

public class GiveMeYourPortResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4108652963446746807L;
	int port;
	String uniqueHostId;
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getUniqueHostId() {
		return uniqueHostId;
	}
	public void setUniqueHostId(String uniqueHostId) {
		this.uniqueHostId = uniqueHostId;
	}
	public GiveMeYourPortResponse(int port, String uniqueHostId) {
		super();
		this.port = port;
		this.uniqueHostId = uniqueHostId;
	}
	public GiveMeYourPortResponse() {
		super();
	}
	

}
