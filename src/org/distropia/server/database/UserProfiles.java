package org.distropia.server.database;

import java.io.File;

public class UserProfiles extends BasicUserProfiles {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5831260621133160201L;

	public UserProfiles(File userProfileDirectory) {
		super(userProfileDirectory);
	}
	
	protected BasicUserProfile createUserProfile( File dataDirectory){
		return new UserProfile( dataDirectory);
	}
	
	public synchronized UserProfile login( String userName, String password) throws Exception{
		logger.info("login called " + userName);
		if (size()==0) logger.error(" ... but i have no users.");
		Thread.sleep(1000); // anti brute force
		for(BasicUserProfile userProfile: this){
			if (userProfile instanceof UserProfile){
				if (((UserProfile)userProfile).login(userName, password)){
					return (UserProfile)userProfile;
				}
			}
			else logger.error("userprofile is not an instance of UserProfile");
		}
		return null;
	}
}
