package org.distropia.server.database;

import java.io.File;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.ArrayUtils;
import org.distropia.server.communication.KnownHost;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class UserDatabase extends Database {
	public static final String KEY_UID = "uniqueUserID";
	public static final String KEY_USERNAME = "userName";
	public static final String KEY_USERPUBLICKEY = "userPublicKey";
	public static final String KEY_SIGNATURE = "signature";
	public static final String KEY_LASTUPDATE = "lastUpdate";	
	public static final String KEY_USERPRIVATEKEY = "userPrivateKey";
	public static final String KEY_HASHEDUSERPASSWORD = "hashedUserPassword";
	public static final String KEY_ENCODEDUSERPASSWORD = "encodedUserPassword";
	
	static final protected KeyPairTupleBinding keyPairEntryBinding = new KeyPairTupleBinding();
	
	protected Key encryptionKey = null;
	protected com.sleepycat.je.Database dbKeyStore = null;
	protected com.sleepycat.je.Database dbSignatures = null;
	
	
	@Override
	protected synchronized void open() throws Exception {
		super.open();
		if (dbKeyStore == null){
			DatabaseConfig dbConfig = new DatabaseConfig();
	        dbConfig.setTransactional(true);
	        dbConfig.setAllowCreate(true);
	        dbKeyStore = environment.openDatabase(null, "keyStore", dbConfig);
	        dbSignatures = environment.openDatabase(null, "signatures", dbConfig);
		}
	}

	@Override
	public synchronized void close() {
		if (dbKeyStore != null){
			dbKeyStore.close();
			dbKeyStore = null;
			dbSignatures.close();
			try {
				getPropertiesSignature(); // update signature for properties db if not already done
			} catch (Exception e) {
				logger.error("error updating properties db signature", e);
			} 
		}
		super.close();		
	}
	
	public boolean checkPropertiesSignature() throws Exception{
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initVerify( getUserPublicKey());
		propertiesToSignature( signature);
		return signature.verify( getPropertiesSignature());		
	}
	
	private void propertiesToSignature( Signature signature) throws Exception{
		open();
		ArrayList<DatabaseEntry> ignoreKeys = new ArrayList<DatabaseEntry>();
		DatabaseEntry entry = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_SIGNATURE, entry);
		ignoreKeys.add( entry);
		Transaction txn = environment.beginTransaction(null, null);
		try{
			DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();
			Cursor cursor = dbProperties.openCursor(null, null);		
			try{		
				while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
					if (!ignoreKeys.contains( foundKey)){
						signature.update( foundKey.getData());
						signature.update( foundData.getData());
					}
			    }
			}
			finally{
				cursor.close();
			}
			txn.commit();
		}
		catch (Exception e) {
			txn.abort();
			logger.error( "error cleaning known hosts", e);
		}
	}
	
	public byte[] getPropertiesSignature() throws Exception{
		byte[] result = getPropertyByteArray(KEY_SIGNATURE, null);
		if (result == null){
			// calculate new
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign( getUserPrivateKey());
			propertiesToSignature(signature);
			result = signature.sign();
			setPropertyByteArray(KEY_SIGNATURE, result);
		}
		return result;
	}
	
	@Override
	public void setProperty( DatabaseEntry key, DatabaseEntry value) throws Exception{
		super.setProperty(key, value);
		DatabaseEntry keyEntry = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_LASTUPDATE, keyEntry);		
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeLong( getUpdateTime());
		super.setProperty( keyEntry, new DatabaseEntry( tupleOutput.getBufferBytes()));
		super.deleteProperty(KEY_SIGNATURE);
	}
	
	public KeyPair getKeyPairFor( String uniqueUserID) throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry entry = new DatabaseEntry();
		stringEntryBinding.objectToEntry( uniqueUserID, key);
		if (dbKeyStore.get(null, key, entry, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			decrypt(entry, encryptionKey);
			return keyPairEntryBinding.entryToObject( entry);
		}
		return null;		
	}
	
	public void setKeyPairFor( String uniqueUserID) throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry entry = new DatabaseEntry();
		stringEntryBinding.objectToEntry( uniqueUserID, entry);
		encrypt(entry, encryptionKey);
		dbKeyStore.put(null, key, entry);
	}
	
	public PublicKey getUserPublicKey() throws Exception{
		byte[] bPublicKey = getPropertyByteArray(KEY_USERPUBLICKEY, null);
		if ((bPublicKey != null) && (bPublicKey.length > 0)){
			KeyFactory kf = KeyFactory.getInstance("RSA");			
			return kf.generatePublic( new X509EncodedKeySpec( bPublicKey));
		}
		return null;
	}
	
	public void setUserPrivateKey( PrivateKey privateKey) throws Exception{
		if (privateKey == null) deleteProperty(KEY_USERPUBLICKEY);
		else{
			setPropertyByteArray(KEY_USERPRIVATEKEY, encrypt( privateKey.getEncoded(), encryptionKey));
		}
	}
	
	public PrivateKey getUserPrivateKey() throws Exception{
		byte[] bPrivateKey= getPropertyByteArray(KEY_USERPRIVATEKEY, null);
		if ((bPrivateKey != null) && (bPrivateKey.length > 0)){
			KeyFactory kf = KeyFactory.getInstance("RSA");			
			return kf.generatePrivate( new PKCS8EncodedKeySpec( decrypt( bPrivateKey, encryptionKey)));		
		}
		return null;
	}
	
	public void setUserPublicKey( PublicKey publicKey) throws Exception{
		if (publicKey == null) deleteProperty(KEY_USERPUBLICKEY);
		else setPropertyByteArray(KEY_USERPUBLICKEY, publicKey.getEncoded());
	}


	public String getUserName() throws Exception{
		DatabaseEntry entry = getProperty( KEY_USERNAME);
		if (entry == null) return null;
		decrypt(entry, encryptionKey);
		return stringEntryBinding.entryToObject( entry);
	}
	
	public void setUserName( String userName) throws Exception{
		DatabaseEntry entry = new DatabaseEntry();
		stringEntryBinding.objectToEntry( userName, entry);
		encrypt( entry, encryptionKey);
		setProperty(KEY_USERNAME, entry);
	}
	
	public void setEncryptionKey(String password) throws Exception {
		System.out.println("setting password:"+password);
		
		if (password == null) encryptionKey = null;
		else {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(password.toCharArray(), getHashedUserPassword().getSalt(), 1024, 256);
			
			Key keyStoreKey = new SecretKeySpec( factory.generateSecret(spec).getEncoded(), "AES");
			
			
			//Key keyStoreKey = new SecretKeySpec( password, "AES");
			byte[] userPassword = getPropertyByteArray(KEY_ENCODEDUSERPASSWORD, null);
			if (userPassword != null){
				byte[] keyBytes = decrypt( userPassword, keyStoreKey);
				encryptionKey = new SecretKeySpec( keyBytes, "AES");
			}
			else{ // create new
				KeyGenerator kgen = KeyGenerator.getInstance("AES");
				kgen.init(256);
				encryptionKey = kgen.generateKey();
				setPropertyByteArray(KEY_ENCODEDUSERPASSWORD, encrypt(encryptionKey.getEncoded(), keyStoreKey));				
			}
		}
	}
	
	public UserDatabase(File databasePath) throws Exception {
		super(databasePath);
	}
	
	public HashedPassword getHashedUserPassword() throws Exception
	{
		DatabaseEntry entry = getProperty( KEY_HASHEDUSERPASSWORD);
		if (entry == null) return null;
		return (new HashedPasswordTupleBinding()).entryToObject(entry);
	}
	
	public void setHashedUserPassword( HashedPassword hashedPassword) throws Exception
	{
		if (hashedPassword == null) {
			setProperty( KEY_HASHEDUSERPASSWORD, null);
			return;
		}
		DatabaseEntry databaseEntry = new DatabaseEntry();
		(new HashedPasswordTupleBinding()).objectToEntry(hashedPassword, databaseEntry);
		setProperty( KEY_HASHEDUSERPASSWORD, databaseEntry);
	}
	
	protected void encrypt(DatabaseEntry entry, Key key) throws Exception{
		if (entry == null) return;
		if (entry.getData() == null) return;
		entry.setData( encrypt(entry.getData(), key));
	}
	
	protected byte[] encrypt(byte[] in, Key key) throws Exception
	{
		if (in == null) return null;
		if (key == null) throw new Exception("No key for encryption!");
		
		Cipher cipher = Cipher.getInstance("AES");
	    
	    KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(256);
		Key myKey = kgen.generateKey();
		cipher.init(Cipher.WRAP_MODE, key);
		byte[] encodedKey = cipher.wrap( myKey);
		
		TupleOutput to = new TupleOutput();
		byte shift = (byte) (new Random()).nextInt(256);
		to.writeUnsignedByte( shift);
		
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
	    digest.reset();
	    byte[] hash = digest.digest( in);
	    to.write( hash);
		
		for (int index=0; index < in.length; index++)
			in[index] = (byte) (in[index] - shift);
		to.write(in);
		
		cipher.init(Cipher.ENCRYPT_MODE, myKey);	
	    return ArrayUtils.addAll( encodedKey, cipher.doFinal( to.toByteArray()));	    
	}
	
	protected void decrypt(DatabaseEntry entry, Key key) throws Exception{
		if (entry == null) return;
		if (entry.getData() == null) return;
		entry.setData( decrypt(entry.getData(), key));
	}
	
	protected byte[] decrypt(byte[] in, Key key) throws Exception
	{
		if (in == null) return null;
		if (key == null) throw new Exception("No key for decryption!");
		
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.UNWRAP_MODE, key);
	    
	    Key myKey = cipher.unwrap( Arrays.copyOfRange(in, 0, 48), "AES", Cipher.SECRET_KEY);
	    cipher.init(Cipher.DECRYPT_MODE, myKey);
	    
	    TupleInput ti = new TupleInput( cipher.doFinal( Arrays.copyOfRange(in, 48, in.length)));
	    
	    int shift = ti.readUnsignedByte();
	    byte[] hash = new byte[20];
	    ti.read( hash);
	    byte[] result = new byte[ti.available()];
	   
	    ti.read( result);
	    for (int index=0; index < result.length; index++)
			result[index] = (byte) (result[index] + shift);
	    
	    MessageDigest digest = MessageDigest.getInstance("SHA-1");
	    digest.reset();
		if (!Arrays.equals( digest.digest( result), hash)) throw new Exception("Encryption exception");
	    
		return result;
	}
	
	public static void main(String[] args) throws NumberFormatException, Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(256);
		Key key = kgen.generateKey();
		
		UserDatabase userDatabase = new UserDatabase(new File("/tmp/"));
		/*
		byte[] enc = userDatabase.encrypt("hallo123".getBytes(), key);
		
		System.out.println( enc.length+ " " + new String(userDatabase.decrypt(enc, key)));*/
		
	}
	
	public String getUniqueUserID() throws Exception
	{
		return getPropertyString(KEY_UID, null);
	}
	
	public void setUniqueUserID( String uniqueUserID) throws Exception
	{
		setPropertyString(KEY_UID, uniqueUserID);
	}
}
