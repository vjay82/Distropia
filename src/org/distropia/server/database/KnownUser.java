package org.distropia.server.database;

public class TrustedUser {
	String userUID;
	byte[] publicKey;
	public String getUserUID() {
		return userUID;
	}
	public void setUserUID(String userUID) {
		this.userUID = userUID;
	}
	public byte[] getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	public TrustedUser(String userUID, byte[] publicKey) {
		super();
		this.userUID = userUID;
		this.publicKey = publicKey;
	}
	public TrustedUser() {
		super();
		// TODO Auto-generated constructor stub
	}
	
}
