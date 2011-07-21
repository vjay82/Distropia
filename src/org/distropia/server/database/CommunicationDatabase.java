package org.distropia.server.database;

import java.io.File;
import java.util.Date;

import net.tomp2p.peers.PeerAddress;

import org.distropia.client.Utils;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;


public class CommunicationDatabase extends Database {
	protected static final String KEY_UNIQUEHOSTID = "UniqueHostId";

	protected static final int MAXIMUM_SAVETIMEFORUNIMPORTANTKNOWNHOST = 60000 * 60 * 24 * 30 * 5; // 5 months
	protected com.sleepycat.je.Database dbKnownHosts = null;
	protected com.sleepycat.je.Database dbDHTPeers = null;
	protected KnownHostTupleBinding knownHostTupleBinding = new KnownHostTupleBinding();

	@Override
	protected synchronized void open() throws Exception {
		super.open();
		if (dbKnownHosts == null){
			DatabaseConfig dbConfig = new DatabaseConfig();
	        dbConfig.setTransactional(true);
	        dbConfig.setAllowCreate(true);
	        dbKnownHosts = environment.openDatabase(null, "knownHosts", dbConfig);
	        dbDHTPeers = environment.openDatabase(null, "dhtPeers", dbConfig);
		}
	}

	@Override
	public synchronized void close() {
		if (dbKnownHosts != null){
			dbKnownHosts.close();
			dbDHTPeers.close();
			dbKnownHosts = null;
		}
		super.close();		
	}

	public CommunicationDatabase(File databasePath) throws Exception {
		super(databasePath);		
	}
	
	public String getUniqueHostId() throws Exception
	{
		String result = getPropertyString( KEY_UNIQUEHOSTID, null);
		if ((result == null) || ("".equals( result)))
		{
			result = java.util.UUID.randomUUID().toString().replaceAll("-", "");
			setPropertyString( KEY_UNIQUEHOSTID, result);
		}
		return result;
	}	
	
	public void loadKnownHostsForBootstap( KnownHosts knownHosts) throws Exception
	{
		open();
		
		DatabaseEntry foundKey = new DatabaseEntry();
	    DatabaseEntry foundData = new DatabaseEntry();
		Cursor cursor = dbKnownHosts.openCursor(null, null);		
		try{		
			while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				knownHosts.add( knownHostTupleBinding.entryToObject( new TupleInput( foundData.getData())));
		    }
		}
		finally{
			cursor.close();
		}
	}
	
	public KnownHost getKnownHost( String uniqueHostId) throws Exception
	{
		open();
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(uniqueHostId, theKey);
		DatabaseEntry result = new DatabaseEntry();
		if (dbKnownHosts.get(null, theKey, result, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			return knownHostTupleBinding.entryToObject( result);
		}
		return null;
	}
	
	public void cleanKnownHosts() throws Exception
	{
		long deleteOlderThan = (new Date()).getTime() - MAXIMUM_SAVETIMEFORUNIMPORTANTKNOWNHOST;
		open();
		Transaction txn = environment.beginTransaction(null, null);
		try{
			DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();
			Cursor cursor = dbKnownHosts.openCursor(null, null);		
			try{		
				while (cursor.getPrev(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			        KnownHost knownHost = knownHostTupleBinding.entryToObject( foundData);
			        if (!knownHost.isImportantHost() && knownHost.getLastAccess() < deleteOlderThan){
			        	dbKnownHosts.delete(txn, foundKey);
			        }			        
			    }
			}
			finally{
				cursor.close();
			}
			txn.commit();
		}
		catch (Exception e) {
			txn.abort();
			logger.error( "error cleaning known hosts", e);
		}
		
	}
	
	public void deleteKnownHost( KnownHost knownHost) throws Exception
	{
		open();
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry( knownHost.getUniqueHostId(), theKey);
		dbKnownHosts.delete(null, theKey);
	}
	
	public void setKnownHost( KnownHost knownHost) throws Exception
	{
		if (Utils.isNullOrEmpty( knownHost.getUniqueHostId())) return;
		open();
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry( knownHost.getUniqueHostId(), theKey);
		DatabaseEntry data = new DatabaseEntry();
		knownHostTupleBinding.objectToEntry( knownHost, data);		
		dbKnownHosts.put(null, theKey, data);
	}
	
	public void deletePeerAddress( PeerAddress peerAddress) throws Exception
	{
		open();
		DatabaseEntry theKey = new DatabaseEntry(peerAddress.getID().toByteArray());
		dbDHTPeers.delete(null, theKey);
	}
	
	public PeerAddress getRandomPeerAddress() throws Exception
	{		
		open();

		DatabaseEntry foundKey = new DatabaseEntry();
	    DatabaseEntry foundData = new DatabaseEntry();
		Cursor cursor = dbDHTPeers.openCursor(null, null);
		long count = (long)(Math.random() * dbDHTPeers.count());
		try{		
			while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				if (count == 0) return new PeerAddress( foundData.getData());
		        count--;
		    }
		}
		finally{
			cursor.close();
		}
		return null;
	}
	
	public void setPeerAddress( PeerAddress peerAddress) throws Exception
	{
		open();
		DatabaseEntry theKey = new DatabaseEntry(peerAddress.getID().toByteArray());
		DatabaseEntry data = new DatabaseEntry( peerAddress.toByteArray());
		dbDHTPeers.put(null, theKey, data);
	}
	
}
