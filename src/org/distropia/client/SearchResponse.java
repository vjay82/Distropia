package org.distropia.client;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class SearchResponse extends DefaultUserResponse {
	List<PublicUserCredentials> users = new ArrayList<PublicUserCredentials>();

	public List<PublicUserCredentials> getUsers() {
		return users;
	}

	public void setUsers(List<PublicUserCredentials> users) {
		this.users = users;
	}

	public SearchResponse() {
		super();
	}

	public SearchResponse(String sessionId, Boolean succeeded,
			String failReason, String uniqueUserId, boolean admin) {
		super(sessionId, succeeded, failReason, uniqueUserId, admin);
	}

	public SearchResponse(String sessionId, Boolean succeeded, String failReason) {
		super(sessionId, succeeded, failReason);
	}

	public SearchResponse(String sessionId) {
		super(sessionId);
	}
	
}
