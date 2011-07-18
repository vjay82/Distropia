package org.distropia.client;

import java.io.Serializable;

public class DefaultRequest implements Serializable{

	private static final long serialVersionUID = -6130408211728240333L;
	protected String sessionId;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public DefaultRequest(String sessionId) {
		super();
		this.sessionId = sessionId;
	}

	public DefaultRequest() {
		super();
	}
	
}
