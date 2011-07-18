package org.distropia.client;


public class DefaultResponse extends DefaultRequest {

	private static final long serialVersionUID = -5643811672311746024L;
	protected boolean succeeded;
	protected String failReason;
	

	public Boolean isSucceeded() {
		return succeeded;
	}
	public void setSucceeded(boolean succeeded) {
		this.succeeded = succeeded;
	}
	public String getFailReason() {
		return failReason;
	}
	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}
	
	public DefaultResponse(String sessionId, Boolean succeeded,
			String failReason) {
		super();
		this.sessionId = sessionId;
		this.succeeded = succeeded;
		this.failReason = failReason;
	}
	public DefaultResponse() {
		super();
	}
	public DefaultResponse(String sessionId) {
		super(sessionId);
	}
	
}
