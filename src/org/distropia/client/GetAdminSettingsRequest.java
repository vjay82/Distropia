package org.distropia.client;

public class GetAdminSettingsRequest extends DefaultRequest {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1980175261590681633L;
	boolean statisticsOnly;

	public boolean isStatisticsOnly() {
		return statisticsOnly;
	}

	public void setStatisticsOnly(boolean statisticsOnly) {
		this.statisticsOnly = statisticsOnly;
	}

	public GetAdminSettingsRequest() {
		super();
	}

	public GetAdminSettingsRequest(String sessionId) {
		super(sessionId);
	}

	public GetAdminSettingsRequest(String sessionId, boolean statisticsOnly) {
		super(sessionId);
		this.statisticsOnly = statisticsOnly;
	}
	
}
