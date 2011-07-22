package org.distropia.server.database;


import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserProfile extends UserDatabase{
	protected static final Logger logger = LoggerFactory.getLogger(UserProfile.class);
	protected String uniqueUserID = null;
	protected PublicKey publicKey = null;
	
	public UserProfile( File databasePath) throws Exception {
		super( databasePath);
	}

	@Override
	public String toString() {
		
		return super.toString();
	}

	public String getUniqueUserID() {
		if (uniqueUserID == null)
			try {
				uniqueUserID = super.getUniqueUserID();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error loading uniqueUserID", e);
			}
		return uniqueUserID;
	}
	
	/*
	@Override
	public void setKeyPairFor(String uniqueUserID) throws Exception {
		if (getUniqueUserID().equals( uniqueUserID)) throw new Exception("You are not allowed oberwriting the users keypair.");
		super.setKeyPairFor(uniqueUserID);
	}*/

	public synchronized boolean login( String userName, String password) throws Exception
	{
		logger.info("login user - " + this + " - trying login");
		HashedPassword hashedPassword = getHashedUserPassword();
		if (hashedPassword == null) return false;
		if (hashedPassword.compare(password))
		{
			logger.info("login user - " + this + " - initializing password");
			setEncryptionKey(password); // from now on encrypted access takes place
			
			if (userName.equalsIgnoreCase( getUserName()))
			{
				
				
				return true;
			}
		}
			
		logger.info("login user - " + this + " - wrong password");
		return false;
	}
	
	public synchronized void initializeUser( String password) throws Exception
	{
		logger.info("creating User - creating hashed user password");
		HashedPassword hashedUserPassword = new HashedPassword();
		hashedUserPassword.createNew( password);
		setHashedUserPassword( hashedUserPassword);
		
		logger.info("creating User - initializing userPassword");
		setEncryptionKey(password); // from now on encrypted access could take place
		
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize( 2048);
	    KeyPair keyPair = keyPairGenerator.genKeyPair();
	    setUserPrivateKey( keyPair.getPrivate());
	    setUserPublicKey( keyPair.getPublic());
	}
	
	public synchronized PublicKey getUserPublicKey() throws Exception{
		if (publicKey == null) publicKey = super.getUserPublicKey();
		return publicKey;
	}
}
