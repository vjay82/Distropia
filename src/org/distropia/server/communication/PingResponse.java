package org.distropia.server.communication;


public class PingResponse extends DefaultServerResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4430217369148011393L;
	protected String pingResponseFromUniqueHostID;
	public String getPingResponseFromUniqueHostID() {
		return pingResponseFromUniqueHostID;
	}
	public void setPingResponseFromUniqueHostID(String pingResponseFromUniqueHostID) {
		this.pingResponseFromUniqueHostID = pingResponseFromUniqueHostID;
	}
	public PingResponse(String pingResponseFromUniqueHostID) {
		super();
		this.pingResponseFromUniqueHostID = pingResponseFromUniqueHostID;
	}
	public PingResponse() {
		super();
	}
}
