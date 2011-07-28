package org.distropia.server.communication;

import java.util.List;

public class GetUserDatabaseRequest extends CommunicationObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4142115355420919518L;
	protected List<Long> onlyNewerItemsThan;
	protected List<String> databases;
	public List<String> getDatabases() {
		return databases;
	}
	public void setDatabases(List<String> databases) {
		this.databases = databases;
	}
	
	public GetUserDatabaseRequest(List<Long> onlyNewerItemsThan,
			List<String> databases) {
		super();
		this.onlyNewerItemsThan = onlyNewerItemsThan;
		this.databases = databases;
	}
	public List<Long> getOnlyNewerItemsThan() {
		return onlyNewerItemsThan;
	}
	public void setOnlyNewerItemsThan(List<Long> onlyNewerItemsThan) {
		this.onlyNewerItemsThan = onlyNewerItemsThan;
	}
	public GetUserDatabaseRequest() {
		super();
	}
	
	
 
}
