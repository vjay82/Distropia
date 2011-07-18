package org.distropia.server.database;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class HashedPassword {
	private byte[] hash = null;
	private byte[] salt = null;
	public byte[] getHash() {
		return hash;
	}
	public void setHash(byte[] hash) {
		this.hash = hash;
	}
	public byte[] getSalt() {
		return salt;
	}
	public void setSalt(byte[] salt) {
		this.salt = salt;
	}
	public HashedPassword(byte[] hash, byte[] salt) {
		super();
		this.hash = hash;
		this.salt = salt;
	}
	public HashedPassword() {
		super();
	}
	
	public boolean compare( String password) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		return (Arrays.equals( hash, getHash( 1000, password, salt)));
	}
	
	public void createNew( String password) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		// Uses a secure Random not a simple Random
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        
        // Salt generation 64 bits long
        salt = new byte[8];
        random.nextBytes(salt);
        
        hash = getHash( 1000, password, salt);
	}
	
	private byte[] getHash(int iterationNb, String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
	       MessageDigest digest = MessageDigest.getInstance("SHA-1");
	       digest.reset();
	       digest.update(salt);
	       byte[] input = digest.digest(password.getBytes("UTF-8"));
	       for (int i = 0; i < iterationNb; i++) {
	           digest.reset();
	           input = digest.digest(input);
	       }
	       return input;
	   }
}
