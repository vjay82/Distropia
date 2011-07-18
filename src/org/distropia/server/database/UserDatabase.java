package org.distropia.server.database;

import java.io.File;

public class UserDatabase extends Database {
	public static final String KEY_UID = "uniqueUserID";
	
	public UserDatabase(File databaseFile) throws Exception {
		super(databaseFile);
	}
	
	public String getUniqueUserID() throws Exception
	{
		return getProperty(KEY_UID, null);
	}
	
	public void setUniqueUserID( String uniqueUserID) throws Exception
	{
		setProperty(KEY_UID, uniqueUserID);
	}
}
