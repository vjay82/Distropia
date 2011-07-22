package org.distropia.server.database;

import java.io.File;
import java.util.Date;

import org.distropia.server.Backend;
import org.distropia.server.Maintenanceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class DistropiaDatabase implements Maintenanceable{
	protected static final int TIMER_INTERVAL_MAINTENANCE = 60000; // every minute
	protected static final int AUTOCLOSE_AFTER = 60000; // automatically closes every unused db after one minute
	
	static final protected EntryBinding<String> stringEntryBinding = TupleBinding.getPrimitiveBinding(String.class);
	protected static Logger logger = LoggerFactory.getLogger(DistropiaDatabase.class);
	protected File databasePath = null;
	protected Environment environment = null;
	protected long lastAccess = 0;
	protected com.sleepycat.je.Database dbProperties = null;
	

	@Override
	public synchronized void maintenance() {
		if (lastAccess + AUTOCLOSE_AFTER < System.currentTimeMillis())
		{
			close();
		}
	}
	
	protected synchronized void open() throws Exception{
		lastAccess = System.currentTimeMillis();
		
		if (environment == null){
			EnvironmentConfig envConfig = new EnvironmentConfig();
	        envConfig.setTransactional(true);
	        envConfig.setAllowCreate(true);
	        environment = new Environment( databasePath, envConfig);
	        
	        DatabaseConfig dbConfig = new DatabaseConfig();
	        dbConfig.setTransactional(true);
	        dbConfig.setAllowCreate(true);
	        dbProperties = environment.openDatabase(null, "properties", dbConfig);
	        if (Backend.getInstance() != null)
	        	if (Backend.getMaintenanceList() != null) 
	        		Backend.getMaintenanceList().addWithWeakReference( this, TIMER_INTERVAL_MAINTENANCE);
		}
	}
	
	public synchronized void close(){
		if (environment != null){
			dbProperties.close();
			dbProperties = null;
			try{
				environment.close();
			}catch (Exception e) {
				e.printStackTrace();
				logger.error("error closing database", e);
			}
			environment = null;
			Backend.getMaintenanceList().remove( this);
		}
	}
	
	public DistropiaDatabase(File databasePath) throws Exception {
		super();
		this.databasePath = databasePath;
		if ((!databasePath.exists()) && (!databasePath.mkdirs())) throw new Exception("Could not create path " + databasePath.getAbsolutePath());
		
	}	
	
	public long getPropertyLong( String key, long defaultValue) throws Exception
	{
		DatabaseEntry result = getProperty(key);
		if (result == null) return defaultValue;
		TupleInput tupleInput = new TupleInput( result.getData());		
		return tupleInput.readLong();
	}
	
	public int getPropertyInt( String key, int defaultValue) throws Exception
	{
		DatabaseEntry result = getProperty(key);
		if (result == null) return defaultValue;
		TupleInput tupleInput = new TupleInput( result.getData());		
		return tupleInput.readInt();
	}
	
	public byte[] getPropertyByteArray( String key, byte[] defaultValue) throws Exception
	{
		DatabaseEntry result = getProperty(key);
		if (result == null) return defaultValue;
		return result.getData();
	}
	
	public String getPropertyString( String key, String defaultValue) throws Exception
	{
		DatabaseEntry result = getProperty(key);
		if (result == null) return defaultValue;
		return stringEntryBinding.entryToObject( result);
	}
	
	public DatabaseEntry getProperty( String key) throws Exception{
		DatabaseEntry keyEntry = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, keyEntry);
		return getProperty(keyEntry);		
	}
	
	public DatabaseEntry getProperty( DatabaseEntry theKey) throws Exception{
		open();
		DatabaseEntry result = new DatabaseEntry();
		if (dbProperties.get(null, theKey, result, LockMode.DEFAULT) == OperationStatus.SUCCESS) return result;
		return null;
	}
	
	public void setPropertyInt( String key, int value) throws Exception
	{
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeInt( value);		
		DatabaseEntry theValue = new DatabaseEntry( tupleOutput.getBufferBytes());
		setProperty( key, theValue);
	}
	
	public void setPropertyLong( String key, long value) throws Exception
	{
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeLong( value);		
		DatabaseEntry theValue = new DatabaseEntry( tupleOutput.getBufferBytes());
		setProperty( key, theValue);
	}
	
	public void setPropertyString( String key, String value) throws Exception
	{
		if (value != null){
			DatabaseEntry theValue = new DatabaseEntry();
			stringEntryBinding.objectToEntry(value, theValue);
			setProperty( key, theValue);
		}
		else deleteProperty( key);
	}
	
	public void setProperty( String key, DatabaseEntry value) throws Exception
	{
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		setProperty(theKey, value);
	}
	
	public void setPropertyByteArray( String key, byte[] value) throws Exception
	{
		if (value == null) deleteProperty(key);
		else{
			DatabaseEntry theKey = new DatabaseEntry();
			stringEntryBinding.objectToEntry(key, theKey);
			setProperty(theKey, new DatabaseEntry( value));
		}
	}
	
	public void setProperty( DatabaseEntry key, DatabaseEntry value) throws Exception{
		open();
		if (value == null) {
			dbProperties.delete(null, key);
		}
		else dbProperties.put(null, key, value);
	}
	
	public void setProperty( Transaction txn, DatabaseEntry key, DatabaseEntry value) throws Exception{
		open();
		if (value == null) {
			dbProperties.delete(txn, key);
		}
		else dbProperties.put(txn, key, value);
	}
	
	public void deleteProperty( String key) throws Exception{
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		deleteProperty( theKey);
	}
	
	public void deleteProperty( DatabaseEntry key) throws Exception{
		open();
		dbProperties.delete(null, key);
	}
	
	public long getUpdateTime()
	{
		return (new Date()).getTime();
	}
	
	
	public String getName()
	{
		return databasePath.getName();
	}
}
