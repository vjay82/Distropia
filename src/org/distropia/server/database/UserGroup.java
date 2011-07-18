package org.distropia.server.database;

public class UserGroup {
	String userUID;
	byte[] sharedSecret;
	boolean internalGroup;
	String groupName;
	long updateTime;
	public String getUserUID() {
		return userUID;
	}
	public void setUserUID(String nodeUID) {
		this.userUID = nodeUID;
	}
	public byte[] getSharedSecret() {
		return sharedSecret;
	}
	public void setSharedSecret(byte[] sharedSecret) {
		this.sharedSecret = sharedSecret;
	}
	public boolean isInternalGroup() {
		return internalGroup;
	}
	public void setInternalGroup(boolean internalGroup) {
		this.internalGroup = internalGroup;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public long getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	public UserGroup(String userUID, byte[] sharedSecret,
			boolean internalGroup, String groupName, long updateTime) {
		super();
		this.userUID = userUID;
		this.sharedSecret = sharedSecret;
		this.internalGroup = internalGroup;
		this.groupName = groupName;
		this.updateTime = updateTime;
	}
	public UserGroup() {
		super();
	}
	
	
}
