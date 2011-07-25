package org.distropia.server.communication.dht;

import java.io.Serializable;
import java.util.ArrayList;

import org.distropia.client.PublicUserCredentials;

public class UserItem implements Serializable{
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 5328544778974325565L;
	private PublicUserCredentials credentials;
    private ArrayList<String> storedAtMachine;
    
    private boolean compareWithNull(Object a, Object b){
    	return ((a == b) || ((a != null) && a.equals(b)));
    }

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof UserItem)) return false;
		return ( compareWithNull( credentials, ((UserItem)obj).credentials));
	}

	public PublicUserCredentials getCredentials() {
		return credentials;
	}

	public void setCredentials(PublicUserCredentials credentials) {
		this.credentials = credentials;
	}

	public ArrayList<String> getStoredAtMachine() {
		return storedAtMachine;
	}

	public void setStoredAtMachine(ArrayList<String> storedAtMachine) {
		this.storedAtMachine = storedAtMachine;
	}

	public UserItem() {
		super();
	}

	public UserItem(PublicUserCredentials credentials,
			ArrayList<String> storedAtMachine) {
		super();
		this.credentials = credentials;
		this.storedAtMachine = storedAtMachine;
	}
	
    
}
