package org.distropia.server.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserProfiles extends ArrayList<UserProfile>{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2584238882225861240L;
	private File userProfileDirectory = null;
	static Logger logger = LoggerFactory.getLogger(UserProfiles.class);

	public UserProfiles(File userProfileDirectory) throws Exception {
		super();
		this.userProfileDirectory = userProfileDirectory;
		loadUserProfiles();
	}
	
	public void close()
	{
		for( UserProfile basicUserProfile: this)
		{
			basicUserProfile.close();
		}
		clear();
	}
	
	protected UserProfile loadUserProfile( File dataDirectory) throws Exception{
		return new UserProfile( dataDirectory);
	}
	
	private void loadUserProfiles() throws Exception 
    {
		logger.info("loading userProfiles from directory " + userProfileDirectory.getAbsolutePath());
    	File[] entries = userProfileDirectory.listFiles();
    	for (File entry : entries)
    	{
    		if (entry.isDirectory())
    		{
    			add( loadUserProfile( entry));
    		}
    	}
    }
	
	protected String createNewUniqueUserID()
	{
		return java.util.UUID.randomUUID().toString().replaceAll("-", "");
	}
	
	public UserProfile createNewUser() throws Exception{
		
		int tryCount = 100;
		do {
			String uniqueUserID = createNewUniqueUserID();
			File dataDirectory = new File( userProfileDirectory.getAbsolutePath() + File.separator + uniqueUserID);
			if (!dataDirectory.exists() && dataDirectory.mkdirs())
			{
				UserProfile userProfile = loadUserProfile( dataDirectory);
				try {
					userProfile.setUniqueUserID(uniqueUserID);
					return userProfile;
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
	
	public synchronized UserProfile login( String userName, String password) throws Exception{
		logger.info("login called " + userName);
		if (size()==0) logger.error(" ... but i have no users.");
		Thread.sleep(1000); // anti brute force
		for(UserProfile userProfile: this){
			if (userProfile.login(userName, password)){
				return userProfile;
			}			
		}
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
	
}
