package org.distropia.server.database;

import java.security.PublicKey;

public class KnownUser {
	protected byte[] databaseKey;
	protected String uniqueUserID;
	protected PublicKey publicKey;
	
	
	public byte[] getDatabaseKey() {
		return databaseKey;
	}
	public void setDatabaseKey(byte[] databaseKey) {
		this.databaseKey = databaseKey;
	}
	public String getUniqueUserID() {
		return uniqueUserID;
	}
	public void setUniqueUserID(String uniqueUserID) {
		this.uniqueUserID = uniqueUserID;
	}
	public PublicKey getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}	
	public KnownUser() {
		super();
	}
	
	
}
