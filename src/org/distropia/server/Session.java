package org.distropia.server;

import java.util.Date;

import org.distropia.server.database.UserProfile;

public class Session {
	public static final int SESSIONTIMEOUT = 60000 * 30; 
	protected String sessionId;
	protected long lastAccess;
	protected UserProfile userProfile = null;
	protected boolean admin;

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(UserProfile userProfile) {
		this.userProfile = userProfile;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Session(String sessionId) {
		super();
		this.sessionId = sessionId;
	}

	public long getLastAccess() {
		return lastAccess;
	}

	public void updateLastAccess() {
		this.lastAccess = (new Date()).getTime();
	}
	
	public boolean isTimeOut(){
		return lastAccess + SESSIONTIMEOUT < System.currentTimeMillis();
	}
	
}
