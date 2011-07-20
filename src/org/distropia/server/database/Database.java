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

public class Database implements Maintenanceable{
	protected static final int TIMER_INTERVAL_MAINTENANCE = 60000; // every minute
	protected static final int AUTOCLOSE_AFTER = 60000; // automatically closes every unused db after one minute
	
	protected static Logger logger = LoggerFactory.getLogger(UserProfile.class);
	protected File databasePath = null;
	protected Environment environment = null;
	protected long lastAccess = 0;
	protected com.sleepycat.je.Database dbProperties = null;
	static final protected EntryBinding<String> stringEntryBinding = TupleBinding.getPrimitiveBinding(String.class);

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
	        Backend.getMaintenanceList().addWithWeakReference( this, TIMER_INTERVAL_MAINTENANCE);
		}
	}
	
	public synchronized void close(){
		if (environment != null){
			dbProperties.close();
			environment.close();
			environment = null;
			Backend.getMaintenanceList().remove( this);
		}
	}
	
	public Database(File databasePath) throws Exception {
		super();
		this.databasePath = databasePath;
		if ((!databasePath.exists()) && (!databasePath.mkdirs())) throw new Exception("Could not create path " + databasePath.getAbsolutePath());
		
	}	
	
	public long getProperty( String key, long defaultValue) throws Exception
	{
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeLong( defaultValue);		
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		DatabaseEntry theDefaultValue = new DatabaseEntry( tupleOutput.getBufferBytes());
		DatabaseEntry result = getProperty(theKey, theDefaultValue);		
		TupleInput tupleInput = new TupleInput( result.getData());		
		return tupleInput.readLong();
	}
	
	public int getProperty( String key, int defaultValue) throws Exception
	{
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeInt( defaultValue);		
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		DatabaseEntry theDefaultValue = new DatabaseEntry( tupleOutput.getBufferBytes());
		DatabaseEntry result = getProperty(theKey, theDefaultValue);		
		TupleInput tupleInput = new TupleInput( result.getData());		
		return tupleInput.readInt();
	}
	
	public String getProperty( String key, String defaultValue) throws Exception
	{
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		DatabaseEntry theDefaultValue = new DatabaseEntry();
		stringEntryBinding.objectToEntry(defaultValue, theDefaultValue);
		DatabaseEntry result = getProperty(theKey, theDefaultValue);	
		return stringEntryBinding.entryToObject( result);
	}
	
	private DatabaseEntry getProperty( DatabaseEntry theKey, DatabaseEntry defaultValue) throws Exception{
		open();
		DatabaseEntry result = new DatabaseEntry();
		if (dbProperties.get(null, theKey, result, LockMode.DEFAULT) == OperationStatus.SUCCESS) return result;
		return defaultValue;
	}
	
	public void setProperty( String key, int value) throws Exception
	{
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeInt( value);		
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		DatabaseEntry theValue = new DatabaseEntry( tupleOutput.getBufferBytes());
		setProperty(theKey, theValue);
	}
	
	public void setProperty( String key, long value) throws Exception
	{
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeLong( value);		
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		DatabaseEntry theValue = new DatabaseEntry( tupleOutput.getBufferBytes());
		setProperty(theKey, theValue);
	}
	
	public void setProperty( String key, String value) throws Exception
	{
		DatabaseEntry theKey = new DatabaseEntry();
		stringEntryBinding.objectToEntry(key, theKey);
		DatabaseEntry theValue = new DatabaseEntry();
		stringEntryBinding.objectToEntry(value, theValue);
		setProperty(theKey, theValue);
	}
	
	private void setProperty( DatabaseEntry key, DatabaseEntry value) throws Exception{
		open();
		dbProperties.put(null, key, value);
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
