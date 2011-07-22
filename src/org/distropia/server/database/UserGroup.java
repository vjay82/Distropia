package org.distropia.server.database;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class UserGroup {
	protected String uniqueGroupID = null;
	protected List<String> members = new ArrayList<String>();
	protected List<String> includeMembersOfThisGroup = new ArrayList<String>();
	protected List<String> excludeMembersOfThisGroup = new ArrayList<String>();
	protected boolean visible = false; // visible to user, or automatic generated group
	protected SecretKey secretKey = null;
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
	public List<String> getIncludeMembersOfThisGroup() {
		return includeMembersOfThisGroup;
	}
	public void setIncludeMembersOfThisGroup(List<String> includeMembersOfThisGroup) {
		this.includeMembersOfThisGroup = includeMembersOfThisGroup;
	}
	public List<String> getExcludeMembersOfThisGroup() {
		return excludeMembersOfThisGroup;
	}
	public void setExcludeMembersOfThisGroup(List<String> excludeMembersOfThisGroup) {
		this.excludeMembersOfThisGroup = excludeMembersOfThisGroup;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
}
