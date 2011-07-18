package org.distropia.server.database;


import java.io.File;

import javax.crypto.spec.SecretKeySpec;

import org.distropia.client.UserCredentials;

public class UserProfile extends BasicUserProfile {
	
	protected UserCredentials credentials = null;
	
	
	public UserProfile(File dataDirectory){
		super(dataDirectory);
	}
	
	private void setUserPasswordForDecryption(String password) throws Exception
	{
		if ((password == null) || ("".equals(password))) getPrivateUserDatabase().setUserPasswordForDecryption( null);
		else {
			while (password.length() < 16) password = password + password;
			password = password.substring(0, 16);
			getPrivateUserDatabase().setUserPasswordForDecryption( new SecretKeySpec( password.getBytes("UTF-8"), "AES"));
		}
	}
	
	public synchronized boolean login( String userName, String password) throws Exception
	{
		logger.info("login user - " + this + " - trying login");
		HashedPassword hashedPassword = getPrivateUserDatabase().getHashedUserPassword();
		if (hashedPassword.compare(password))
		{
			logger.info("login user - " + this + " - initializing password");
			this.setUserPasswordForDecryption(password); // from now on encrypted access takes place
			
			if (userName.equalsIgnoreCase( getPrivateUserDatabase().getUsername()))
			{
				logger.info("login user - " + this + " - loading uid");
				uniqueUserID = getPrivateUserDatabase().getUniqueUserID();
				return true;
			}
		}
			
		logger.info("login user - " + this + " - wrong password");
		return false;
	}
	
	public synchronized void setNewUserPassword( String password) throws Exception
	{
		logger.info("creating User - initializing userPassword");
		setUserPasswordForDecryption(password); // from now on encrypted access takes place
		
		logger.info("creating User - creating hashed user password");
		HashedPassword hashedUserPassword = new HashedPassword();
		hashedUserPassword.createNew( password);
		getPrivateUserDatabase().setHashedUserPassword( hashedUserPassword);
	}

	public UserCredentials getCredentials() {
		if (credentials == null){
			try {
				credentials = getPrivateUserDatabase().getCredentials();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error getting user credentials", e);
			}
		}
		return credentials;
	}

	public void setCredentials(UserCredentials credentials) {
		this.credentials = credentials;
		try {
			getPrivateUserDatabase().setCredentials( credentials);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("error setting user credentials", e);
		}
	}

	public void setUserName(String userName) throws Exception {
		getPrivateUserDatabase().setUsername(userName);
	}
	
	
}
