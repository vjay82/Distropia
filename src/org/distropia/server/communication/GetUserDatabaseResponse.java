package org.distropia.server.communication;

public class GetUserDatabaseResponse extends CommunicationObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8085969516113990336L;
	protected byte[] result;
	public byte[] getResult() {
		return result;
	}
	public void setResult(byte[] result) {
		this.result = result;
	}
	public GetUserDatabaseResponse(byte[] result) {
		super();
		this.result = result;
	}
	public GetUserDatabaseResponse() {
		super();
	}
	
}
