package org.distropia.server.communication;


public class WrappedServerCommandResponse extends DefaultServerResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = -585409599890734716L;
	protected Boolean encrypted;
	protected byte[] data;
	protected int entryNumber = 0;
	
	
	public int getEntryNumber() {
		return entryNumber;
	}
	public void setEntryNumber(int entryNumber) {
		this.entryNumber = entryNumber;
	}
	public Boolean getEncrypted() {
		return encrypted;
	}
	public void setEncrypted(Boolean encrypted) {
		this.encrypted = encrypted;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public WrappedServerCommandResponse(Boolean encrypted, byte[] data) {
		super();
		this.encrypted = encrypted;
		this.data = data;
	}
	
	public WrappedServerCommandResponse(Boolean encrypted, byte[] data,
			int entryNumber) {
		super();
		this.encrypted = encrypted;
		this.data = data;
		this.entryNumber = entryNumber;
	}
	public WrappedServerCommandResponse() {
		super();
	}
	
}
