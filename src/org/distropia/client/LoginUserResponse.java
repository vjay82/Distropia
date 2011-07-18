package org.distropia.client;

public class LoginUserResponse extends DefaultUserResponse {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3907933038439212641L;
	protected Boolean userDoesNotExistOrWrongPassword;
	
	public LoginUserResponse() {
		super();
	}
	public String getSessionId() {
		return sessionId;
	}
	public Boolean getUserDoesNotExistOrWrongPassword() {
		return userDoesNotExistOrWrongPassword;
	}
	public void setUserDoesNotExistOrWrongPassword(
			Boolean userDoesNotExistOrWrongPassword) {
		this.userDoesNotExistOrWrongPassword = userDoesNotExistOrWrongPassword;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public LoginUserResponse(String sessionId, Boolean succeeded,
			String failReason, String uniqueUserId, boolean admin,
			Boolean userDoesNotExistOrWrongPassword) {
		super(sessionId, succeeded, failReason, uniqueUserId, admin);
		this.userDoesNotExistOrWrongPassword = userDoesNotExistOrWrongPassword;
	}
	public LoginUserResponse(String sessionId, Boolean succeeded,
			String failReason, String uniqueUserId, boolean admin) {
		super(sessionId, succeeded, failReason, uniqueUserId, admin);
	}
	public LoginUserResponse(String sessionId, Boolean succeeded,
			String failReason) {
		super(sessionId, succeeded, failReason);
	}

	
	
}
