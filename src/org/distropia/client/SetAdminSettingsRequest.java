package org.distropia.client;

public class SetAdminSettingsRequest extends DefaultUserResponse {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7325189927316804467L;
	boolean automaticUpdates, onlyReachableByLocalhost;
	String automaticUpdatesFromBranch;
	
	public String getAutomaticUpdatesFromBranch() {
		return automaticUpdatesFromBranch;
	}

	public void setAutomaticUpdatesFromBranch(String automaticUpdatesFromBranch) {
		this.automaticUpdatesFromBranch = automaticUpdatesFromBranch;
	}
	
	public boolean isAutomaticUpdates() {
		return automaticUpdates;
	}

	public void setAutomaticUpdates(boolean automaticUpdates) {
		this.automaticUpdates = automaticUpdates;
	}

	public boolean isOnlyReachableByLocalhost() {
		return onlyReachableByLocalhost;
	}

	public void setOnlyReachableByLocalhost(boolean onlyReachableByLocalhost) {
		this.onlyReachableByLocalhost = onlyReachableByLocalhost;
	}

	public SetAdminSettingsRequest() {
		super();
	}

	public SetAdminSettingsRequest(String sessionId, Boolean succeeded,
			String failReason, String uniqueUserId, boolean admin) {
		super(sessionId, succeeded, failReason, uniqueUserId, admin);
	}

	public SetAdminSettingsRequest(String sessionId, Boolean succeeded,
			String failReason) {
		super(sessionId, succeeded, failReason);
	}
	
	public SetAdminSettingsRequest(String sessionId) {
		super(sessionId);
	}

}
