package org.distropia.client;

@SuppressWarnings("serial")
public class SearchRequest extends DefaultRequest {
	String searchForName;

	public String getSearchForName() {
		return searchForName;
	}

	public void setSearchForName(String searchForName) {
		this.searchForName = searchForName;
	}

	public SearchRequest(String sessionId, String searchForName) {
		super(sessionId);
		this.searchForName = searchForName;
	}

	public SearchRequest() {
		super();
	}

	public SearchRequest(String sessionId) {
		super(sessionId);
	}
	
	
}
