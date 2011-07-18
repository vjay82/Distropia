package org.distropia.server.communication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.distropia.client.Utils;
import org.distropia.server.Backend;
import org.distropia.server.database.CommunicationDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KnownHosts extends ArrayList<KnownHost> {

	/**
	 * 
	 */
	public static final int MAXIMUM_LIST_SIZE = 200;
	public static final int MINIMUM_LIST_SIZE = 100;
	
	private static final long serialVersionUID = -59641901459828056L;
	protected static transient Logger logger = LoggerFactory.getLogger(KnownHosts.class);
	
	protected transient CommunicationDatabase database = null;
	protected transient java.util.Timer saveTimer = null;
	protected List<KnownHost> modifiedHosts = new ArrayList<KnownHost>();
	

	@Override
	public boolean add(KnownHost e) {
		if (Utils.equalsWithNull( e.getUniqueHostId(), Backend.getUniqueHostId())) return false;
		boolean added = super.add(e);
		if (added) e.setKnownHosts( this);
		return added;
	}

	public KnownHosts(CommunicationDatabase database) {
		super();
		this.database = database;
	}
	
	public void sortForLastAccessTime(){
		Collections.sort( this, new Comparator<KnownHost>() {
			@Override
			public int compare(KnownHost o1, KnownHost o2) {
				if ( o2.getLastAccess() > o1.getLastAccess()) return 1;
				return -1;
			}			
		});
	}

	public ArrayList<KnownHost> getKnownHostsWithHighesAccessTimeFirstWhichAreDirectlyAccessible( int count){
		if (count == 0) count = size();
		ArrayList<KnownHost> result = new ArrayList<KnownHost>(count);
		synchronized (this) {
			sortForLastAccessTime();
			
			for(KnownHost knownHost: this){
				if ((knownHost.getAddresses().size() > 0) && (!knownHost.isInForwardMode()))
				{
				 	result.add( knownHost);
				 	if (result.size() == count) break;
				}
			}
		}
		
		return result;
	}	
	
	public ArrayList<KnownHost> getKnownHostsWithHighesAccessTimeFirst( int count){
		if (count == 0) count = size();
		ArrayList<KnownHost> result = new ArrayList<KnownHost>(count);
		synchronized (this) {
			sortForLastAccessTime();
			
			for(KnownHost knownHost: this){
				if (knownHost.couldBeReached())
				{
				 	result.add( knownHost);
				 	if (result.size() == count) break;
				}
			}
		}
		
		return result;
	}
	
	protected void cleanUp(){
		
		// this will drop every known host, that is not accessed
		// secure connection -> 1 minute
		// unsecure 20 seconds
		logger.info("cleaning up");
		long twoMinutesAgo = (new Date()).getTime() - 60000*2;
		long oneMinuteAgo = (new Date()).getTime() - 60000;
		ArrayList<KnownHost> removedHosts = new ArrayList<KnownHost>();
		synchronized ( this) {
			sortForLastAccessTime();
			
			// we limit the size of the live buffer to prevent attacks
			while (size() > MAXIMUM_LIST_SIZE) remove( size()-1);
			
			for(int index=size()-1; index>=MINIMUM_LIST_SIZE; index--)
			{
				KnownHost knownHost = get(index);
				if (knownHost.isSecureConnection())
				{
					if (get(index).getLastAccess() < twoMinutesAgo)
					{
						removedHosts.add( knownHost);
						remove( index);
					}
				}
				else
				{
					if (get(index).getLastAccess() < oneMinuteAgo) 
					{
						removedHosts.add( knownHost);
						remove( index);
					}
				}
			}
		}		
		
		// remove from proxied hosts
		if (removedHosts.size()>0)
		{
			for(KnownHost knownHost: removedHosts)
				knownHost.close();
		}
	}
	
	protected void saveModifiedHosts()
	{
		logger.info("Saving modified hosts.");
		List<KnownHost> toSave = new ArrayList<KnownHost>();
		synchronized (modifiedHosts) {
			
			toSave.addAll( modifiedHosts);
			modifiedHosts.clear();
			if (saveTimer !=  null){
				saveTimer.cancel();
				saveTimer = null;
			}
		}
		
		try {
			for (KnownHost knownHost: toSave){
				if(knownHost.isImportantEnoughForDatabase())
					database.setKnownHost( knownHost);
				else
					database.deleteKnownHost( knownHost);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param knownHost
	 * @param important if set, immediate save is triggered, otherwise it will be cached
	 */
	public void onKnownHostModified( KnownHost knownHost, boolean important)
	{
		if (logger.isDebugEnabled()) 
			logger.info("onKnownHostModified called " + knownHost.getUniqueHostId());
		
		if (important){
			synchronized (modifiedHosts) {
				if (!modifiedHosts.contains( knownHost)) modifiedHosts.add( knownHost);
			}
			saveModifiedHosts();
		}
		else {
			synchronized (modifiedHosts) {
				if (!modifiedHosts.contains( knownHost)) modifiedHosts.add( knownHost);
				if (saveTimer == null)
				{
					saveTimer = new java.util.Timer("Save modified known hosts to database - timer");
					TimerTask timerTask = new TimerTask() {
						
						@Override
						public void run() {
							saveModifiedHosts();
							cleanUp();
						}
					};
					saveTimer.schedule( timerTask, 30000);
				}
			}
			
		}
	}
	
	public KnownHost getKnownHostOrNull( String uniqueHostId) throws Exception{
		synchronized (this) {
			for (KnownHost knownHost: this)
				if ( uniqueHostId.equals( knownHost.getUniqueHostId())) return knownHost;
		}
		KnownHost knownHost = database.getKnownHost( uniqueHostId);
		if (knownHost != null){
			synchronized (this) {
				// maybe another thread added it already, check again
				for (KnownHost host: this)
					if ( uniqueHostId.equals( host.getUniqueHostId())) return knownHost;
				knownHost.updateLastAccess();
				add( knownHost);
				knownHost.setKnownHosts( this);
			}
		}
		return knownHost;
	}

	public void close() {
		saveModifiedHosts();
		for(KnownHost knownHost: this)
			knownHost.close();
		if (saveTimer != null)
		{
			saveTimer.cancel();
			saveTimer = null;
		}
	}
}
