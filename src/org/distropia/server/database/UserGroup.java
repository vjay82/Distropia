package org.distropia.server.database;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class UserGroup {
	String uniqueGroupID;
	List<String> members = new ArrayList<String>();
	SecretKey secretKey;
	public String getUniqueGroupID() {
		return uniqueGroupID;
	}
	public void setUniqueGroupID(String uniqueGroupID) {
		this.uniqueGroupID = uniqueGroupID;
	}
	public List<String> getMembers() {
		return members;
	}
	public void setMembers(List<String> members) {
		this.members = members;
	}
	public SecretKey getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}
	
}
