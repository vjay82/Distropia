package org.distropia.client;

public class DefaultUserResponse extends DefaultResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4268320776785384814L;
	String uniqueUserId;
	boolean admin;
	public String getUniqueUserId() {
		return uniqueUserId;
	}
	public void setUniqueUserId(String uniqueUserId) {
		this.uniqueUserId = uniqueUserId;
	}
	public boolean isAdmin() {
		return admin;
	}
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
	public DefaultUserResponse(String sessionId, Boolean succeeded,
			String failReason, String uniqueUserId, boolean admin) {
		super(sessionId, succeeded, failReason);
		this.uniqueUserId = uniqueUserId;
		this.admin = admin;
	}
	public DefaultUserResponse() {
		super();
	}
	public DefaultUserResponse(String sessionId, Boolean succeeded,
			String failReason) {
		super(sessionId, succeeded, failReason);
	}
	public DefaultUserResponse(String sessionId) {
		super(sessionId);
	}
}
