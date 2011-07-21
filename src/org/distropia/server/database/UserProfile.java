package org.distropia.server.database;


import java.io.File;

import org.distropia.server.Maintenanceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicUserProfile implements Maintenanceable{
	
	static Logger logger = LoggerFactory.getLogger(BasicUserProfile.class);
	
	protected File dataDirectory = null;
	protected String uniqueUserID = null;
	protected PrivateUserDatabase privateUserDatabase = null;
	protected PublicUserDatabase publicUserDatabase = null;
	protected long privateUserDatabaseLastAccess = 0;
	protected long publicUserDatabaseLastAccess = 0;
	
	public synchronized void closePrivateUserDatabase(){
		if (privateUserDatabase != null)
		{
			privateUserDatabase.close();
			privateUserDatabase = null;
		}
	}
	
	public synchronized void closePublicUserDatabase(){
		if (publicUserDatabase != null)
		{
			publicUserDatabase.close();
			publicUserDatabase = null;
		}
	}
	
	public void close()
	{		
		closePrivateUserDatabase();
		closePublicUserDatabase();
	}
	
	protected File getPrivateDatabasePath() {
		return new File( dataDirectory.getAbsolutePath() + File.pathSeparator + "privateDatabase.db");
	}
	
	protected File getPublicDatabasePath() {
		return new File( dataDirectory.getAbsolutePath() + File.pathSeparator + "publicDatabase.db");
	}
	
	public BasicUserProfile( File dataDirectory) {
		super();
		
		this.dataDirectory = dataDirectory;
	}
	
	
	public synchronized PrivateUserDatabase getPrivateUserDatabase() throws Exception
	{
		privateUserDatabaseLastAccess = System.currentTimeMillis();
		if (privateUserDatabase == null)
		{
			privateUserDatabase = new PrivateUserDatabase( getPrivateDatabasePath());
		}
		return privateUserDatabase;		
	}
	
	
	public synchronized PublicUserDatabase getPublicUserDatabase() throws Exception
	{
		publicUserDatabaseLastAccess = System.currentTimeMillis();
		if (publicUserDatabase == null)
		{
			publicUserDatabase = new PublicUserDatabase( getPublicDatabasePath());		
		}
		return publicUserDatabase;		
	}

	public File getDataDirectory() {
		return dataDirectory;
	}

	public void setDataDirectory(File dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	@Override
	public String toString() {
		
		return super.toString();
	}

	public String getUniqueUserID() {
		return uniqueUserID;
	}

	@Override
	public synchronized void maintenance() {
		long xMillisAgo = System.currentTimeMillis() - 60000;
		if (privateUserDatabaseLastAccess < xMillisAgo) closePrivateUserDatabase();
		if (publicUserDatabaseLastAccess < xMillisAgo) closePublicUserDatabase();
	}
	
}
