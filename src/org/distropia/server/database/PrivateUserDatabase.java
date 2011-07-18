package org.distropia.server.database;

import java.io.File;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import net.tomp2p.utils.Utils;

import org.apache.xerces.impl.dv.util.Base64;
import org.distropia.client.UserCredentials;

public class PrivateUserDatabase extends UserDatabase {
	public static final String TABLE_HASHEDPASSWORDS = "HashedPasswords";
	public static final String TABLE_ASYMMETRIC_KEYS = "AsymmetricKeys";
	public static final String TABLE_TRUSTEDUSERS = "TrustedUsers";
	
	public static final String KEY_USERNAME = "userName";
	public static final String KEY_USERPASSWORD = "userPassword";
	public static final String KEY_CREDENTIALS = "credentials";
	
	protected SecretKeySpec userPassword = null;
	
	/**
	 * contains password, every uid knows for encrypting messages, pictures and so on, so that only that uids can see them
	 */
	private static final String TABLE_USERGROUPS = "UserGroups";
	

	public void setUserPasswordForDecryption(SecretKeySpec userPassword) {
		this.userPassword = userPassword;
	}

	public PrivateUserDatabase(File databaseFile) throws Exception {
		super(databaseFile);
	}

	@Override
	protected void createDefaultTables(Statement statement) throws SQLException {
		super.createDefaultTables(statement);
		statement.executeUpdate("create table if not exists " + TABLE_HASHEDPASSWORDS + " (key TEXT PRIMARY KEY, password BLOB, salt BLOB, updateTime INTEGER);");
		statement.executeUpdate("create table if not exists " + TABLE_ASYMMETRIC_KEYS + " (userUID TEXT PRIMARY KEY, privateKey BLOB, publicKey BLOB, updateTime INTEGER);");
		statement.executeUpdate("create table if not exists " + TABLE_USERGROUPS + " (userUID TEXT, sharedSecret BLOB, isInternalGroup BOOLEAN, groupName TEXT, updateTime INTEGER);");
		statement.executeUpdate("create table if not exists " + TABLE_TRUSTEDUSERS + " (userUID TEXT PRIMARY KEY, publicKey BLOB, updateTime INTEGER);");
	}
	
	
	
	public HashedPassword getHashedUserPassword() throws SQLException
	{
		return getHashedPassword( KEY_USERPASSWORD);
	}
	
	public HashedPassword getHashedPassword(String key) throws SQLException
	{
		Statement stat = connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery("select password, salt from "+TABLE_HASHEDPASSWORDS+" WHERE key = \"" + key + "\";");
			try {
				if (rs.isClosed()) return null;
				if (rs.getRow() == 0) return null;
				return new HashedPassword( rs.getBytes(1), rs.getBytes(2));
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public UserCredentials getCredentials( ) throws Exception{
		byte[] b = Base64.decode( getEncryptedProperty(KEY_CREDENTIALS, null));
		if (b.length == 0) return null;
		return (UserCredentials) Utils.decodeJavaObject(b, 0, b.length);
	}
	
	public void setCredentials( UserCredentials userCredentials) throws Exception{
		setEncryptedProperty(KEY_CREDENTIALS, Base64.encode( Utils.encodeJavaObject(userCredentials)));
	}
	
	public String getUsername() throws Exception{
		return getEncryptedProperty( KEY_USERNAME, null);
	}
	
	public void setUsername( String userName) throws Exception
	{
		setEncryptedProperty( KEY_USERNAME, filterDataBaseString( userName));
	}
	
	public String getEncryptedProperty( String key, String defaultValue) throws Exception
	{
		throwUserPasswordIsNullException();
		String newDefaultValue = null;
		if (defaultValue != null) newDefaultValue = encrypt( defaultValue);
		String result = super.getProperty(key, newDefaultValue);
		if (result == null) return null;
		return decrypt( result);
	}
	
	public void setEncryptedProperty( String key, String value) throws Exception
	{
		throwUserPasswordIsNullException();
		String newValue = null;
		if (value != null) newValue = encrypt( value);
		super.setProperty(key, newValue);
	}
	
	public void setHashedUserPassword( HashedPassword hashedPassword) throws Exception
	{
		setHashedPassword(KEY_USERPASSWORD, hashedPassword);
	}
	
	protected void throwUserPasswordIsNullException() throws Exception
	{
		if (userPassword == null) throw new Exception("Userpassword is null, it has to be initialized.");
	}
	
	public void setHashedPassword( String key, HashedPassword hashedPassword) throws Exception
	{
		try {
			PreparedStatement prep = connection.prepareStatement(
			  "update "+TABLE_HASHEDPASSWORDS+" set password = ?, salt = ?, updateTime = ? WHERE key = \"" + key+ "\";" );

			long updateTime = getUpdateTime();
			prep.setBytes(1, hashedPassword.getHash());
			prep.setBytes(2, hashedPassword.getSalt());
			prep.setLong(3, updateTime);
			prep.execute();
			int updateCount = prep.getUpdateCount();
			prep.close();
			if (updateCount == 0)
			{
				prep = connection.prepareStatement( "insert into " + TABLE_HASHEDPASSWORDS + " values (?, ?, ?, ?);");
				
				prep.setString(1, key);
				prep.setBytes(2, hashedPassword.getHash());
				prep.setBytes(3, hashedPassword.getSalt());
				prep.setLong(4, updateTime);
				prep.execute();
				prep.close();
				
			}
			
		} catch (SQLException e) { // selbe wie oben...
			e.printStackTrace();
			logger.error("Error setting hashed password " + key + " at database " + getName(), e);			
		}
	}
	
	public KeyPair getAsymmetricKeys(String userUID, boolean couldGenerateNew) throws Exception
	{
		throwUserPasswordIsNullException();
		KeyPair result = null;
		Statement stat = connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery("select privateKey, publicKey from " + TABLE_ASYMMETRIC_KEYS + " WHERE userUID = \"" + userUID + "\";");
			try {
				if (!rs.isClosed())
				{
					PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec( decrypt( rs.getBytes(1)));
					X509EncodedKeySpec publicKey = new X509EncodedKeySpec( decrypt( rs.getBytes(2)));
					KeyFactory kf = KeyFactory.getInstance("RSA");
					result = new KeyPair( kf.generatePublic( publicKey), kf.generatePrivate( privateKey));						
				}
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (result == null)
		{
			logger.info("creating new keypair for node uid " + userUID);	
			// creating keys
			try {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
				keyPairGenerator.initialize( 2048);
			    result = keyPairGenerator.genKeyPair();
			    setAsymmetricKeys(userUID, result);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				logger.error("error creating keypair for node uid " + userUID, e);
			} 
		}
		return result;
	}
	
	public void setAsymmetricKeys( String userUID, KeyPair keyPair) throws Exception
	{
		throwUserPasswordIsNullException();
		try {
			PreparedStatement prep = connection.prepareStatement(
			  "update " + TABLE_ASYMMETRIC_KEYS + " set privateKey = ?, publicKey = ?, updateTime = ? WHERE userUID = \"" + userUID+ "\";" );

			long updateTime = getUpdateTime();
			byte[] encPrivateKey = encrypt( keyPair.getPrivate().getEncoded());
			byte[] encPublicKey = encrypt( keyPair.getPublic().getEncoded());
			
			prep.setBytes(1, encPrivateKey);
			prep.setBytes(2, encPublicKey);
			prep.setLong(3, updateTime);
			prep.execute();
			int updateCount = prep.getUpdateCount();
			prep.close();
			if (updateCount == 0)
			{
				prep = connection.prepareStatement( "insert into "+TABLE_ASYMMETRIC_KEYS+" values (?, ?, ?, ?);");
				
				prep.setString(1, userUID);
				prep.setBytes(2, encPrivateKey);
				prep.setBytes(3, encPublicKey);
				prep.setLong(4, updateTime);
				prep.execute();
				prep.close();
				
			}
			
		} catch (SQLException e) { // selbe wie oben...
			e.printStackTrace();
			logger.error("Error setting async key " + userUID + " at database " + getName(), e);			
		}
	}
	
	protected byte[] encrypt(byte[] in) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.ENCRYPT_MODE, userPassword);
	    return cipher.doFinal( in);
	}
	
	protected String encrypt(String in) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, userPassword);
	    return Base64.encode(cipher.doFinal( in.getBytes("UTF-8")));
	}
	
	protected byte[] decrypt(byte[] in) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.DECRYPT_MODE, userPassword);
	    return cipher.doFinal( in);
	}
	
	protected String decrypt(String in) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.DECRYPT_MODE, userPassword);
	    return new String(cipher.doFinal( Base64.decode( in)), "UTF-8");
	}

	public ArrayList<UserGroup> getUserGroups( String whereStatement) throws Exception
	{
		throwUserPasswordIsNullException();
		ArrayList<UserGroup> result = new ArrayList<UserGroup>();
		Statement stat = connection.createStatement();
		try {
			String query = "select userUID, sharedSecret, isInternalGroup, groupName, updateTime from " + TABLE_USERGROUPS;
			if (whereStatement != null) query = query + " WHERE (" + whereStatement + ")";
			
			ResultSet rs = stat.executeQuery( query + ";");
			try {
				if (!rs.isClosed())
				{
					while (rs.next())
					{					
						result.add(new UserGroup( decrypt( rs.getString(1)), 
												  decrypt( rs.getBytes(2)),
												  rs.getBoolean(3),
												  decrypt( rs.getString(4)),
												  rs.getLong( 5))
						);
					}
				}
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public ArrayList<TrustedUser> getTrustedUsers() throws Exception
	{
		ArrayList<TrustedUser> result = new ArrayList<TrustedUser>();
		Statement stat = connection.createStatement();
		try {
			String query = "select userUID, publicKey from " + TABLE_TRUSTEDUSERS;
			
			ResultSet rs = stat.executeQuery( query + ";");
			try {
				if (!rs.isClosed())
				{
					while (rs.next())
					{					
						result.add(new TrustedUser( rs.getString(1),
													rs.getBytes(2))
						);
					}
				}
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public void deleteTrustedUser( TrustedUser trustedUser) throws Exception
	{
		try {
			Statement statement = connection.createStatement();
			try 
			{
				statement.execute("delete from " + TABLE_TRUSTEDUSERS + " WHERE userUID = \"" + trustedUser.getUserUID() + "\";" );
			}
			finally
			{
				statement.close();
			}
		} catch (SQLException e) { // selbe wie oben...
			e.printStackTrace();
			logger.error("Error deleting userUID " + trustedUser.getUserUID() + " at database " + getName(), e);			
		}
	}
	
	public void setTrustedUser( TrustedUser trustedUser) throws Exception
	{
		try {
			PreparedStatement prep = connection.prepareStatement(
			  "update " + TABLE_TRUSTEDUSERS + " set publicKey = ?, updateTime = ? WHERE userUID = \"" + trustedUser.getUserUID() + "\";" );

			long updateTime = getUpdateTime();
			
			prep.setBytes(1, trustedUser.getPublicKey());
			prep.setLong(2, updateTime);
			prep.execute();
			int updateCount = prep.getUpdateCount();
			prep.close();
			if (updateCount == 0)
			{
				prep = connection.prepareStatement( "insert into " + TABLE_TRUSTEDUSERS + " values (?, ?, ?);");
				
				prep.setString(1, trustedUser.getUserUID());
				prep.setBytes(2, trustedUser.getPublicKey());
				prep.setLong(3, updateTime);
				prep.execute();
				prep.close();
				
			}
			
		} catch (SQLException e) { // selbe wie oben...
			e.printStackTrace();
			logger.error("Error setting userUID " + trustedUser.getUserUID() + " at database " + getName(), e);			
		}
	}
	
	
}
