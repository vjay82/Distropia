package org.distropia.server.communication;


public class WrappedServerCommandRequest extends DefaultServerRequest {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4546343712953728215L;
	protected String forUniqueHostId;
	protected String fromUniqueHostId;
	protected Boolean encrypted;
	protected byte[] data;
	//protected String remoteAddr = null;
	protected int entryNumber = 0;
	
	
	
	
	public int getEntryNumber() {
		return entryNumber;
	}
	public void setEntryNumber(int entryNumber) {
		this.entryNumber = entryNumber;
	}/*
	public String getRemoteAddr() {
		return remoteAddr;
	}
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}*/
	public String getForUniqueHostId() {
		return forUniqueHostId;
	}
	public void setForUniqueHostId(String forUniqueHostId) {
		this.forUniqueHostId = forUniqueHostId;
	}
	public String getFromUniqueHostId() {
		return fromUniqueHostId;
	}
	public void setFromUniqueHostId(String fromUniqueHostId) {
		this.fromUniqueHostId = fromUniqueHostId;
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
	public WrappedServerCommandRequest(String forUniqueHostId,
			String fromUniqueHostId, Boolean encrypted, byte[] data) {
		super();
		this.forUniqueHostId = forUniqueHostId;
		this.fromUniqueHostId = fromUniqueHostId;
		this.encrypted = encrypted;
		this.data = data;
	}
	public WrappedServerCommandRequest() {
		super();
	}
	
	
	
}
