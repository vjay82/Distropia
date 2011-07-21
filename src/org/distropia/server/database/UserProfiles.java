package org.distropia.server.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.distropia.server.Maintenanceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicUserProfiles extends ArrayList<BasicUserProfile> implements Maintenanceable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2584238882225861240L;
	private File userProfileDirectory = null;
	static Logger logger = LoggerFactory.getLogger(UserProfiles.class);

	public BasicUserProfiles(File userProfileDirectory) {
		super();
		this.userProfileDirectory = userProfileDirectory;
		loadUserProfiles();
	}
	
	public void close()
	{
		for( BasicUserProfile basicUserProfile: this)
		{
			basicUserProfile.close();
		}
		clear();
	}
	
	protected BasicUserProfile createUserProfile( File dataDirectory){
		return new BasicUserProfile( dataDirectory);
	}
	
	private void loadUserProfiles() 
    {
		logger.info("loading userProfiles from directory " + userProfileDirectory.getAbsolutePath());
    	File[] entries = userProfileDirectory.listFiles();
    	for (File entry : entries)
    	{
    		if (entry.isDirectory())
    		{
    			add( createUserProfile( entry));
    		}
    	}
    }
	
	protected String createNewUniqueUserID()
	{
		return java.util.UUID.randomUUID().toString().replaceAll("-", "");
	}
	
	public BasicUserProfile createNewUser(){
		
		int tryCount = 100;
		do {
			String uniqueUserID = createNewUniqueUserID();
			File dataDirectory = new File( userProfileDirectory.getAbsolutePath() + File.separator + uniqueUserID);
			if (dataDirectory.exists() || dataDirectory.mkdirs()) // creating directory
			{
				BasicUserProfile basicUser = createUserProfile( dataDirectory);
				try {
					basicUser.getPrivateUserDatabase().setUniqueUserID(uniqueUserID);
					basicUser.getPublicUserDatabase().setUniqueUserID(uniqueUserID);
					return basicUser;
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("error creating user", e);
					try {
						removeDir( dataDirectory);
					} catch (Exception e2) {
						e2.printStackTrace();
						logger.error("error deleting the user directory", e2);
					}
				}				
			}
			tryCount--;
		} while (tryCount>0);
		return null;
	}
	
	private void removeDir(File dir) throws IOException {
		String[] list = dir.list();        
        for (int i = 0; i < list.length; i++) {
        	String s = list[i];
        	File f = new File(dir, s);
        	if (f.isDirectory()) {
        		removeDir(f);
        	} else {
        		if (!f.delete()) {
        			throw new IOException("Unable to delete file " + f.getAbsolutePath());
        		}
        	}
        }
        if (!dir.delete()) {
        	throw new IOException("Unable to delete directory " + dir.getAbsolutePath());
        }
    }
	
	public File getUserProfileDirectory() {
		return userProfileDirectory;
	}

	@Override
	public synchronized void maintenance() {
		for(BasicUserProfile basicUserProfile: this)
			basicUserProfile.maintenance();
	}
	
}
