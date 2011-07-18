package org.distropia.server.communication;

import java.util.ArrayList;

import org.distropia.server.Backend;
import org.distropia.server.Maintenanceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxiedHosts implements Maintenanceable {
	public static final int MAX_PROXIED_HOSTS = 4;
	public static final long TIMEOUT = 40000;
	public static final int MAX_THREAD_WAITING_TIME = 30000;
	protected static Logger logger = LoggerFactory.getLogger(ProxiedHosts.class);
	
	protected ArrayList<KnownHost> knownHosts = new ArrayList<KnownHost>(MAX_PROXIED_HOSTS);
	protected ArrayList<Long> timeOuts = new ArrayList<Long>(MAX_PROXIED_HOSTS);	
	
	public ProxiedHosts() {
		super();
		Backend.getMaintenanceList().addWithWeakReference( this, 10000);
	}

	@SuppressWarnings("unchecked")
	public synchronized ArrayList<KnownHost> getProxiedKnownHosts()
	{
		return (ArrayList<KnownHost>) knownHosts.clone();
	}
	
	public synchronized KnownHost getFromUniqueHostId(String uniqueHostId){
		for(KnownHost knownHost: knownHosts)
			if(uniqueHostId.equals( knownHost.getUniqueHostId())) return knownHost;
		return null;
	}	
	
	public synchronized boolean contains(KnownHost knownHost){
		return knownHosts.contains( knownHost);
	}
	
	public synchronized boolean containsAndUpdateLastAccess(KnownHost knownHost){
		int index = knownHosts.indexOf( knownHost);
		if (index == -1) return false;
		timeOuts.set( index, System.currentTimeMillis() + TIMEOUT);
		return true;
	}
	
	public synchronized void updateLastAccess( KnownHost knownHost){
		timeOuts.set( knownHosts.indexOf( knownHost), System.currentTimeMillis() + TIMEOUT);
	}
	
	public synchronized boolean add( KnownHost knownHost){
		if (knownHosts.size() >= MAX_PROXIED_HOSTS) return false;
		knownHosts.add( knownHost);
		timeOuts.add( Long.valueOf( System.currentTimeMillis() + TIMEOUT));
		return true;
	}
	
	public synchronized void remove( int index){
		if (index != -1){
			knownHosts.get(index).closeProxyCache();
			timeOuts.remove(index);
			knownHosts.remove(index);
		}
	}
	
	public synchronized void remove(KnownHost knownHost){
		remove( knownHosts.indexOf( knownHost));
	}

	public synchronized void close() {
		Backend.getMaintenanceList().remove( this);
		clear();		
	}

	public synchronized void clear() {
		while (knownHosts.size() > 0) remove(0);		
	}

	@Override
	public synchronized void maintenance() {
		long now = System.currentTimeMillis();
		for (int index= timeOuts.size()-1; index>=0; index--){
			if (timeOuts.get(index).longValue() < now)
			{
				logger.info("killing proxied host " + knownHosts.get(index));
				remove( index);
			}
		}
	}
}
