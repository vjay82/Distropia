package org.distropia.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import org.distropia.client.DefaultRequest;

@SuppressWarnings("serial")
public class SessionCache implements Maintenanceable, Serializable{
	ArrayList<Session> sessions = new ArrayList<Session>();
	
	
	
	public SessionCache() {
		super();		
		Backend.getMaintenanceList().addWithWeakReference( this, 60000);
	}
	
	public Session getSessionForRequest( DefaultRequest defaultRequest){
		return getSessionForSessionId( defaultRequest.getSessionId());
	}

	public synchronized Session getSessionForSessionId( String sessionId){
		for(Session session: sessions)
			if (session.getSessionId().equals( sessionId)){
				if (!session.isTimeOut()){
					session.updateLastAccess();
					return session;
				}
			}
				
		Session session = new Session( UUID.randomUUID().toString());
		do {
			session = new Session( UUID.randomUUID().toString());
		} while ( containsSessionId( session.getSessionId()));
		sessions.add( session);
		session.updateLastAccess();
		return session;			
	}
	
	public synchronized boolean containsSessionId( String sessionId){
		for(Session session: sessions)
			if (session.getSessionId().equals( sessionId) && (!session.isTimeOut())) return true;
		return false;
	}

	@Override
	public synchronized void maintenance() {
		for(int index= sessions.size()-1; index>=0; index--)
			if (sessions.get(index).isTimeOut())
				sessions.remove(index);
	}
	
}
