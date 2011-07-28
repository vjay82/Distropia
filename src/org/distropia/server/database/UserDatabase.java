package org.distropia.server.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.ArrayUtils;
import org.distropia.client.PublicUserCredentials;
import org.distropia.server.communication.GetUserDatabaseRequest;

import sun.security.action.GetLongAction;

import com.google.gwt.dev.util.collect.HashMap;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class UserDatabase extends DistropiaDatabase {
	public static final String KEY_UNIQUEUSERID = "uniqueUserID";
	public static final String KEY_ISSEARCHABLEBYNAME = "searchableByName";
	public static final String KEY_USERNAME = "userName";
	public static final String KEY_USERCREDENTIALS = "userCredentials";
	public static final String KEY_USERPUBLICKEY = "userPublicKey";	
	public static final String KEY_USERPRIVATEKEY = "userPrivateKey";
	public static final String KEY_HASHEDUSERPASSWORD = "hashedUserPassword";
	public static final String KEY_ENCODEDUSERPASSWORD = "encodedUserPassword";
	public static final String[] singletonDatabases = {"properties", "signatures", "knownUsers", "lastUpdates", "userGroups", "multiUserKeys", "publicProperties"};
	public static final int MAX_DATABASE_ENTRY_SIZE = 1024 * 1024 * 20;
	
	static final protected KnownUserTupleBinding knownUserEntryBinding = new KnownUserTupleBinding();
	static final protected UserGroupTupleBinding userGroupEntryBinding = new UserGroupTupleBinding();
	static final protected UserCredentialsTupleBinding userCredentialsEntryBinding = new UserCredentialsTupleBinding();
	static final protected PublicUserCredentialsTupleBinding publicUserCredentialsEntryBinding = new PublicUserCredentialsTupleBinding();
	
	protected Key encryptionKey = null;
	protected com.sleepycat.je.Database dbSignatures = null;
	protected com.sleepycat.je.Database dbKnownUsers = null;
	protected com.sleepycat.je.Database dbLastUpdates = null;
	protected com.sleepycat.je.Database dbUserGroups = null;
	protected com.sleepycat.je.Database dbMultiUserKeys = null;
	protected com.sleepycat.je.Database dbPublicProperties = null;
	protected Map<String, Database> availableDatabases = new HashMap<String, Database>();
	
	
	@Override
	protected synchronized void open() throws Exception {
		super.open();
		if (dbSignatures == null){
			DatabaseConfig dbConfig = new DatabaseConfig();
	        dbConfig.setTransactional(true);
	        dbConfig.setAllowCreate(true);
	        
	        availableDatabases.put("properties", dbProperties);
	        dbSignatures = environment.openDatabase(null, "signatures", dbConfig);
	        availableDatabases.put("signatures", dbSignatures);
	        dbLastUpdates = environment.openDatabase(null, "lastUpdates", dbConfig);
	        availableDatabases.put("lastUpdates", dbLastUpdates);
	        dbKnownUsers = environment.openDatabase(null, "knownUsers", dbConfig);
	        availableDatabases.put("knownUsers", dbKnownUsers);
	        dbUserGroups = environment.openDatabase(null, "knownUsers", dbConfig);
	        availableDatabases.put("userGroups", dbUserGroups);
	        dbMultiUserKeys = environment.openDatabase(null, "multiUserKeys", dbConfig);
	        availableDatabases.put("multiUserKeys", dbMultiUserKeys);
	        dbPublicProperties = environment.openDatabase(null, "publicProperties", dbConfig);
	        availableDatabases.put("publicProperties", dbPublicProperties);
		}
	}

	@Override
	public synchronized void close() {
		if (dbSignatures != null){
			try{
				// calculating missing signatures if user is logged in, after he went away we can't do this anymore
				getDatabaseSignature( null, dbProperties);
				getDatabaseSignature( null, dbKnownUsers);
				getDatabaseSignature( null, dbUserGroups);
				getDatabaseSignature( null, dbLastUpdates);
				getDatabaseSignature( null, dbMultiUserKeys);
				getDatabaseSignature( null, dbPublicProperties);
				
				dbKnownUsers.close();
				dbUserGroups.close();
				dbLastUpdates.close();
				dbMultiUserKeys.close();
				dbPublicProperties.close();
				dbSignatures.close();
			}
			catch (Exception e) {
				e.printStackTrace();
				logger.error("error closing database", e);
			}
			dbSignatures = null;
		}
		super.close();
	}
	
	public GetUserDatabaseRequest createGetUserDatabaseRequest( boolean initialFetch, boolean onlyBasicData) throws Exception{
		List<String> databasesToFetch = new ArrayList<String>();
        
        databasesToFetch.add( "properties");
        databasesToFetch.add( "publicProperties");
        databasesToFetch.add( "knownUsers");
        databasesToFetch.add( "userGroups");
        databasesToFetch.add( "multiUserKeys");
        
        
        List<Long> timeStamps = new ArrayList<Long>();
        if (initialFetch){
        	for(String dTF: databasesToFetch){
	        	timeStamps.add( Long.valueOf(0));
	        }
        }
        else
        	for(String dTF: databasesToFetch){
	        	timeStamps.add( getDatabaseLastUpdateTime( null, dTF));
	        }
        
        GetUserDatabaseRequest userDatabaseRequest = new GetUserDatabaseRequest();
		userDatabaseRequest.setDatabases( databasesToFetch);
		userDatabaseRequest.setOnlyNewerItemsThan( timeStamps);
		return userDatabaseRequest;
	}
	
	public void updateDatabase( InputStream in) throws Exception{
		open();
		ObjectInputStream o = new ObjectInputStream( in);
		DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        
		byte count = o.readByte();
		while (count > 0){
			count--;
			String name = o.readUTF();
			System.out.println(name);
			String tmpName = name + name + "_tmpDatabaseForTransfer";
			Transaction txn = environment.beginTransaction(null, null);
			try{
				if (environment.getDatabaseNames().contains(tmpName))
		        	environment.removeDatabase(txn, tmpName);
		        Database db = environment.openDatabase(txn, tmpName, dbConfig);
				try{
		        	
					if (Arrays.asList( singletonDatabases).contains( name)){
				        long itemCount = o.readLong();
				        boolean gotSomething = itemCount > 0;
				        while (itemCount > 0){
				        	itemCount--;
				        	int size = o.readInt();
				        	if (size > MAX_DATABASE_ENTRY_SIZE) throw new Exception("Size exceeded " + size);
				        	byte[] data = new byte[size];
				        	o.readFully( data);
				        	
				        	DatabaseEntry dbKey = new DatabaseEntry( data);
				        	
				        	size = o.readInt();
				        	if (size > MAX_DATABASE_ENTRY_SIZE) throw new Exception("Size exceeded " + size);
				        	data = new byte[size];
				        	o.readFully( data);
				        	
				        	DatabaseEntry dbValue = new DatabaseEntry( data);
				        	
				        	db.put(txn, dbKey, dbValue);
				        }
				        
				        if (gotSomething){
				        	int size = o.readInt();
					        if (size > MAX_DATABASE_ENTRY_SIZE) throw new Exception("Size exceeded");
				        	byte[] bSignature = new byte[size];
				        	o.readFully( bSignature);
			        		Signature signature = Signature.getInstance("SHA1withRSA");
							signature.initVerify( getUserPublicKey());
							databaseToSignature( txn, db, signature);
							
				        	
				        	if ( signature.verify( bSignature)){ // signature correct
								
				        		Database oldDb = availableDatabases.get( name);
				        		// there is a little security hole at this moment, a sending server could give a wrong timestamp
				        		// we limit the impact by comparing it against the values we know
				        		long timeStamp = o.readLong();
				        		if (timeStamp > getUpdateTime() || timeStamp < getDatabaseLastUpdateTime(txn, oldDb)) throw new Exception("wrong timestamp");
				        		
								
								// clear old database
								DatabaseEntry foundKey = new DatabaseEntry();
							    DatabaseEntry foundData = new DatabaseEntry();
							    Cursor cursor = oldDb.openCursor(txn, null);
								try{		
									while (cursor.getPrev(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
										oldDb.delete(txn, foundKey);
								    }
								}
								finally{
									cursor.close();
								}
							    
							    // now move everything over
							    
								cursor = db.openCursor(txn, null);
								try{
									while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
										oldDb.put(txn, foundKey, foundData);
								    }
								}
								finally{
									cursor.close();
								}
					        	
								setDatabaseSignature(txn, oldDb, bSignature);
								setDatabaseLastUpdate(txn, oldDb, timeStamp);
							}
				        	else throw new Exception("incorrect signature");
				        }
					}

		        	
		        }
		        finally{
		        	db.close();
		        	environment.removeDatabase(txn, tmpName);
		        }
		        
		        		        
				txn.commit();
			}
			catch (Exception e) {
				txn.abort();
				throw e;
			}
		}
	}
	
	public void getDataBaseDump( List<String> names, List<Long> newerThan, OutputStream os) throws Exception{
		open();
		ObjectOutputStream o = new ObjectOutputStream( os);
		//boolean dataPending = false;
		try{
			//int maxBytesToSend = 1024*1024;
			o.writeByte( names.size());
			for(int index=0; index<names.size(); index++){
				String name = names.get(index);
				long onlyNewerThan = newerThan.get(index);
				
				// TODO: build in dynamic limits for non singleton databases
				Database database = availableDatabases.get( name);
				if (database == null) throw new Exception ("Database not found " + name);
				Transaction txn = environment.beginTransaction(null, null);
				try{
					o.writeUTF( name);
					
					DatabaseEntry foundKey = new DatabaseEntry();
				    DatabaseEntry foundData = new DatabaseEntry();
					if (Arrays.asList( singletonDatabases).contains( name)){
				    	
						long wasLastUpdated = getDatabaseLastUpdateTime(txn, name);
						
						System.out.println("sending " + name + " wlu:" + wasLastUpdated + " ont:" + onlyNewerThan + " wluisgrater: " + (wasLastUpdated > onlyNewerThan));
						
						if (wasLastUpdated > onlyNewerThan){
							o.writeLong( database.count());
							
					    	// always write the whole database
					    	Cursor cursor = database.openCursor(txn, null);		
							try{		
								while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
									//maxBytesToSend = maxBytesToSend - foundKey.getSize() - foundData.getSize();
									o.writeInt( foundKey.getSize());
							        o.write( foundKey.getData());
							        o.writeInt( foundData.getSize());
							        o.write( foundData.getData());
							    }
							}
							finally{
								cursor.close();
							}
							
							if (database.count()>0){
								byte[] signature = getDatabaseSignature(txn, database);
								o.writeInt( signature.length);
								o.write( signature);
								o.writeLong( wasLastUpdated);
							}
						}
						else o.writeLong( 0);
				    }
					
					txn.commit();
				}
				catch (Exception e) {
					txn.abort();
					throw e;
				}
				/*if (maxBytesToSend < 1){
					dataPending = true;
					break;
				}*/
			}
			//o.writeBoolean( dataPending);
		}
		finally{
			o.close();
		}
	}
	
	public boolean checkDatabaseSignature( Database database) throws Exception{
		Transaction txn = environment.beginTransaction(null, null);
		try {
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify( getUserPublicKey());
			databaseToSignature( txn, database, signature);
			byte[] oldSignature = getDatabaseSignature( txn, database);
			txn.commit();
			if (oldSignature == null) throw new Exception("old signature was not calculated, error");
			return signature.verify( oldSignature);
		}
		catch (Exception e) {
			txn.abort();
			throw e;
		}
	}
	
	private void databaseToSignature( Transaction txn, com.sleepycat.je.Database database, Signature signature) throws Exception{
		open();
		
		
		DatabaseEntry foundKey = new DatabaseEntry();
	    DatabaseEntry foundData = new DatabaseEntry();
		Cursor cursor = database.openCursor(txn, null);		
		try{
			while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				//if (!ignoreKeys.contains( foundKey))
				{
					signature.update( foundKey.getData());
					signature.update( foundData.getData());
				}
		    }
		}
		finally{
			cursor.close();
		}
	}
	
	public long getDatabaseLastUpdateTime( Transaction txn, String name) throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry( name, key);
		DatabaseEntry result = new DatabaseEntry();
		if (dbLastUpdates.get(txn, key, result, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			TupleInput tupleInput = new TupleInput( result.getData());
			return tupleInput.readLong();
		}
		return 0;
	}
	
	public long getDatabaseLastUpdateTime( Transaction txn, Database database) throws Exception{
		return getDatabaseLastUpdateTime(txn, database.getDatabaseName());
	}
	
	public void resetDatabaseSignature( Transaction txn, Database database){		
		// remove entry from dbSignatures
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry( database.getDatabaseName(), key);
		dbSignatures.delete(txn, key);
		
		// write time to last update database
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeLong( getUpdateTime());
		dbLastUpdates.put(txn, key, new DatabaseEntry( tupleOutput.getBufferBytes()));
		
		// also remove entry from lastUpdateDatabase, as it had changed
		stringEntryBinding.objectToEntry( dbLastUpdates.getDatabaseName(), key);
		dbSignatures.delete(txn, key);
	}
	
	public void setDatabaseLastUpdate(Transaction txn, Database database, long lastUpdate){
		setDatabaseLastUpdate(txn, database.getDatabaseName(), lastUpdate);
	}
	
	public void setDatabaseLastUpdate(Transaction txn, String name, long lastUpdate){
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry( name, key);
		TupleOutput tupleOutput = new TupleOutput();
		tupleOutput.writeLong(lastUpdate);
		dbLastUpdates.put(txn, key, new DatabaseEntry( tupleOutput.getBufferBytes()));
	}
	
	public void setDatabaseSignature(Transaction txn, Database database, byte[] signature) throws Exception{
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry( database.getDatabaseName(), key);
		dbSignatures.put(txn, key, new DatabaseEntry( signature));
	}
	
	public byte[] getDatabaseSignature(Transaction txn, Database database) throws Exception{
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry( database.getDatabaseName(), key);
		DatabaseEntry result = new DatabaseEntry();
		if (dbSignatures.get(txn, key, result, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			return result.getData();
		}
		
		// calculate new
		if (encryptionKey == null) return null; // user is not logged in, we can't calculate a new key
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign( getUserPrivateKey());
		databaseToSignature( txn, database, signature);
		result.setData( signature.sign());
		dbSignatures.put(txn, key, result);
		return result.getData();
	}
	
	@Override
	public void setProperty( DatabaseEntry key, DatabaseEntry value) throws Exception{
		Transaction txn = environment.beginTransaction(null, null);
		try{
			super.setProperty(txn, key, value);
			resetDatabaseSignature(txn, dbProperties);
			txn.commit();
		}
		catch (Exception e) {
			txn.abort();
			throw e;
		}	
	}
	
	public List<KnownUser> getKnownUsers() throws Exception{
		open();
		List<KnownUser> result = new ArrayList<KnownUser>();
		Transaction txn = environment.beginTransaction(null, null);
		try{
			DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();
			Cursor cursor = dbKnownUsers.openCursor(null, null);		
			try{		
				while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			        KnownUser knownUser = knownUserEntryBinding.entryToObject( foundData);
			        knownUser.setDatabaseKey( foundKey.getData());
			        result.add( knownUser);
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
		return result;
	}
	
	public void setKnownUser( KnownUser knownUser) throws Exception{
		open();
		Transaction txn = environment.beginTransaction(null, null);
		try{
			DatabaseEntry entry = new DatabaseEntry();
			knownUserEntryBinding.objectToEntry( knownUser, entry);
			encrypt(entry, encryptionKey);
			if (knownUser.getDatabaseKey() != null) dbKnownUsers.put(txn, new DatabaseEntry( knownUser.getDatabaseKey()), entry);
			else{ // search new key
				knownUser.setDatabaseKey( new byte[8]);
				do {
					(new Random()).nextBytes( knownUser.getDatabaseKey());					
				} while ( dbKnownUsers.putNoOverwrite(txn, new DatabaseEntry( knownUser.getDatabaseKey()), entry) != OperationStatus.SUCCESS);
				
			}
			resetDatabaseSignature(txn, dbKnownUsers);
			txn.commit();
		}
		catch (Exception e) {
			txn.abort();
			throw e;
		}		
	}
	
	public List<SecretKey> decryptAllMultiUserKeysIAmAbleTo( PrivateKey key, String myUniqueUserID, byte[] ourSalt) throws Exception{
		open();
		List<SecretKey> result = new ArrayList<SecretKey>();
		Transaction txn = environment.beginTransaction(null, null);
		try{
			DatabaseEntry foundKey = new DatabaseEntry();
		    DatabaseEntry foundData = new DatabaseEntry();
			Cursor cursor = dbMultiUserKeys.openCursor(null, null);		
			try{		
				while (cursor.getPrev(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			    	try{
			    		foundKey.setData( decrypt(foundKey.getData(), key));
			    		if (myUniqueUserID.equals( stringEntryBinding.entryToObject( foundKey))){
			    			foundData.setData( decrypt(foundData.getData(), key));
			    			result.add( new SecretKeySpec( foundData.getData(), "AES"));			    			
			    		}
			    	}
			    	catch (Exception e) {
						// dontcare
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
		return result;
	}
	
	public void setMultiUserKeyFor( List<KnownUser> knownUsers, SecretKey secretKey) throws Exception{
		open();
		Transaction txn = environment.beginTransaction(null, null);
		try{
			
			for( KnownUser knownUser: knownUsers){
				DatabaseEntry key = new DatabaseEntry();
				DatabaseEntry entry = new DatabaseEntry();
				
				stringEntryBinding.objectToEntry( knownUser.getUniqueUserID(), key);
				key.setData( encrypt( key.getData(), knownUser.getPublicKey()));				
				entry.setData( encrypt( secretKey.getEncoded(), knownUser.getPublicKey()));
				dbMultiUserKeys.put(txn, key, entry);
			}
			
			resetDatabaseSignature(txn, dbMultiUserKeys);
			txn.commit();
		}
		catch (Exception e) {
			txn.abort();
			throw e;
		}
	}
	
	public UserGroup getUserGroup( String uniqueGroupID) throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry entry = new DatabaseEntry();
		stringEntryBinding.objectToEntry( uniqueGroupID, key);
		if (dbKnownUsers.get(null, key, entry, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			decrypt(entry, encryptionKey);
			UserGroup userGroup = userGroupEntryBinding.entryToObject( entry);
			userGroup.setUniqueGroupID( stringEntryBinding.entryToObject( key));
			return userGroup;
		}
		return null;		
	}
	
	public void setUserGroup( UserGroup userGroup) throws Exception{
		open();
		Transaction txn = environment.beginTransaction(null, null);
		try{
			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry entry = new DatabaseEntry();
			stringEntryBinding.objectToEntry( userGroup.getUniqueGroupID(), key);
			userGroupEntryBinding.objectToEntry( userGroup, entry);
			encrypt(entry, encryptionKey);
			dbKnownUsers.put(txn, key, entry);
			resetDatabaseSignature(txn, dbUserGroups);
			txn.commit();
		}
		catch (Exception e) {
			txn.abort();
			throw e;
		}
	}
	
	public PublicKey getUserPublicKey() throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_USERPUBLICKEY, key);
		
		DatabaseEntry result = new DatabaseEntry();
		if (dbPublicProperties.get(null, key, result, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			KeyFactory kf = KeyFactory.getInstance("RSA");			
			return kf.generatePublic( new X509EncodedKeySpec( result.getData()));			
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
		open();
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_USERPUBLICKEY, key);
		if (publicKey == null) dbPublicProperties.delete( null, key);
		else{
			dbPublicProperties.put( null, key, new DatabaseEntry(publicKey.getEncoded()));
		}
		resetDatabaseSignature(null, dbPublicProperties);
	}
	
	public String getUniqueUserID() throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_UNIQUEUSERID, key);
		
		DatabaseEntry result = new DatabaseEntry();
		if (dbPublicProperties.get(null, key, result, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			return stringEntryBinding.entryToObject( result);			
		}
		return null;
	}
	
	public void setUniqueUserID( String uniqueUserID) throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_UNIQUEUSERID, key);
		DatabaseEntry entry = new DatabaseEntry();
		stringEntryBinding.objectToEntry(uniqueUserID, entry);
		dbPublicProperties.put( null, key, entry);
		resetDatabaseSignature(null, dbPublicProperties);
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
	
	public UserCredentials getUserCredentials() throws Exception{
		DatabaseEntry entry = getProperty( KEY_USERCREDENTIALS);
		if (entry == null) return null;
		decrypt(entry, encryptionKey);
		return userCredentialsEntryBinding.entryToObject( entry);
	}
	
	public void setUserCredentials( UserCredentials userCredentials) throws Exception{
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_USERCREDENTIALS, key);
		DatabaseEntry entry = new DatabaseEntry();
		userCredentialsEntryBinding.objectToEntry( userCredentials, entry);
		encrypt( entry, encryptionKey);
		setProperty(key, entry);
		System.out.println("cred pub: " + userCredentials.isNamePublicVisible());
		// now modify public table
		
		publicUserCredentialsEntryBinding.objectToEntry( userCredentials.toPublicUserCredentials(), entry);
		
		System.out.println("pubn: " + userCredentials.getFirstName() + " " +userCredentials.getSurName());
		
		dbPublicProperties.put(null, key, entry);
		resetDatabaseSignature(null, dbPublicProperties);
	}
	
	public PublicUserCredentials getPublicUserCredentials() throws Exception{
		open();
		DatabaseEntry key = new DatabaseEntry();
		stringEntryBinding.objectToEntry(KEY_USERCREDENTIALS, key);
		DatabaseEntry result = new DatabaseEntry();
		if (dbPublicProperties.get(null, key, result, LockMode.DEFAULT) == OperationStatus.SUCCESS){
			PublicUserCredentials publicUserCredentials = publicUserCredentialsEntryBinding.entryToObject( result);
			publicUserCredentials.setPublicKey( getUserPublicKey().getEncoded());
			publicUserCredentials.setUniqueUserId( getUniqueUserID());
			return publicUserCredentials;
		}
		return null;
	}
	
	public void setEncryptionKey(String password) throws Exception {
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
}
