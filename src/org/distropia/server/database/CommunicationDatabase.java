package org.distropia.server.database;

import java.io.File;
import java.net.InetSocketAddress;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.tomp2p.peers.PeerAddress;

import org.apache.xerces.impl.dv.util.Base64;
import org.distropia.client.Utils;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;


public class CommunicationDatabase extends Database {
	public static final String KEY_UNIQUEHOSTID = "UniqueHostId";
	public static final String TABLE_KNOWNHOSTS = "KnownHosts";
	public static final String TABLE_DHTHOSTS = "DHTHosts";
	public static final String TABLE_ASYMMETRIC_KEYS = "AsymmetricKeys";
	protected static final int MAXIMUM_SAVETIMEFORUNIMPORTANTKNOWNHOST = 60000 * 60 * 24 * 30 * 5; // 5 months
	
	@Override
	protected void createDefaultTables(Statement statement) throws SQLException {
		super.createDefaultTables(statement);
		statement.executeUpdate("create table if not exists " + TABLE_KNOWNHOSTS + " (uniqueHostId TEXT PRIMARY KEY, privateKey BLOB, publicKey BLOB, foreignPublicKey BLOB, addresses TEXT, importantHost BOOLEAN, updateTime INTEGER);");
		statement.executeUpdate("create table if not exists " + TABLE_DHTHOSTS + " (peerId TEXT PRIMARY KEY, address TEXT, port INTEGER);");
		statement.executeUpdate("create table if not exists " + TABLE_ASYMMETRIC_KEYS + " (userUID TEXT PRIMARY KEY, privateKey BLOB, publicKey BLOB, updateTime INTEGER);");
	}

	public CommunicationDatabase(File databaseFile) throws Exception {
		super(databaseFile);
		cleanKnownHosts();
	}
	
	public String getUniqueHostId() throws Exception
	{
		String result = getProperty( KEY_UNIQUEHOSTID, null);
		if ((result == null) || ("".equals( result)))
		{
			result = java.util.UUID.randomUUID().toString().replaceAll("-", "");
			setProperty( KEY_UNIQUEHOSTID, result);
		}
		return result;
	}
	
	private KnownHost knownHostFromResultRest( ResultSet rs) throws Exception
	{
		boolean importantHost = rs.getBoolean(6);
		long lastAccessTime = rs.getLong( 7);
		
		// we ignore hosts that are too old
		
		if (importantHost || (lastAccessTime + MAXIMUM_SAVETIMEFORUNIMPORTANTKNOWNHOST > (new Date()).getTime() )){						
			KnownHost knownHost = new KnownHost();
			knownHost.setUniqueHostId( rs.getString( 1));
			KeyFactory kf = KeyFactory.getInstance("RSA");
			
			if ((rs.getBytes(2) != null) && (rs.getBytes( 3) != null))
			{
				PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec( rs.getBytes(2));
				X509EncodedKeySpec publicKey = new X509EncodedKeySpec( rs.getBytes(3));
				knownHost.setKeyPair( new KeyPair(kf.generatePublic( publicKey), kf.generatePrivate( privateKey)));
			}
			else knownHost.setKeyPair( null);
			
			if (rs.getBytes( 4) != null)
			{
				X509EncodedKeySpec foreignPublicKey = new X509EncodedKeySpec( rs.getBytes(4));
				knownHost.setForeignPublicKey( kf.generatePublic( foreignPublicKey));
			}
			else knownHost.setForeignPublicKey( null);
			
			List<String> addresses = Arrays.asList( rs.getString( 5).split("\n"));
			for(String address: addresses) knownHost.addAddress(address);
			knownHost.setImportantHost( importantHost);
			knownHost.setLastAccess( lastAccessTime);
			return knownHost;
		}
		return null;
	}
	
	public void loadKnownHostsForBootstap( KnownHosts knownHosts) throws Exception
	{
		Statement stat = connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery( "SELECT uniqueHostId, privateKey, publicKey, foreignPublicKey, addresses, importantHost, updateTime FROM " + TABLE_KNOWNHOSTS + " LIMIT 100;");
			try {
				if (!rs.isClosed())
				{
					while (rs.next())
					{
						KnownHost knownHost = knownHostFromResultRest(rs);
						if (knownHost != null) knownHosts.add( knownHost);
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
	}
	
	public KnownHost getKnownHost( String uniqueHostId) throws Exception
	{
		Statement stat = connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery( "SELECT uniqueHostId, privateKey, publicKey, foreignPublicKey, addresses, importantHost, updateTime FROM " + TABLE_KNOWNHOSTS + " WHERE ( uniqueHostId='" + uniqueHostId + "');");
			try {
				if (!rs.isClosed() && (rs.getRow() != 0))
				{
					return knownHostFromResultRest(rs);
				}
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void cleanKnownHosts() throws Exception
	{
		Statement statement = connection.createStatement();
		try {
			statement.execute("DELETE FROM " + TABLE_KNOWNHOSTS + " WHERE ( (importantHost=0) AND ( updateTime < '" + String.valueOf( (new Date()).getTime() - MAXIMUM_SAVETIMEFORUNIMPORTANTKNOWNHOST) + "' ));");
		} finally {
			statement.close();
		}
	}
	
	public void deleteKnownHost( KnownHost knownHost) throws Exception
	{
		Statement statement = connection.createStatement();
		try {
			statement.execute("DELETE FROM " + TABLE_KNOWNHOSTS + " WHERE uniqueHostId = '" + knownHost.getUniqueHostId() + "';");
		} finally {
			statement.close();
		}
	}
	
	public void debug_deleteKnownHosts( ) throws Exception
	{
		Statement statement = connection.createStatement();
		try {
			statement.execute("DELETE FROM " + TABLE_KNOWNHOSTS);
		} finally {
			statement.close();
		}
	}
	
	public void setKnownHost( KnownHost knownHost) throws Exception
	{
		if (Utils.isNullOrEmpty( knownHost.getUniqueHostId())) return;
		try {
			PreparedStatement prep = connection.prepareStatement(
			  "update " + TABLE_KNOWNHOSTS + " set privateKey = ?, publicKey = ?, foreignPublicKey = ?, addresses = ?, importantHost = ?, updateTime = ? WHERE uniqueHostId = \"" + knownHost.getUniqueHostId() + "\";" );

			StringBuilder addresses = new StringBuilder();
			for (String s1: knownHost.getAddresses())
			{
				addresses.append( filterDataBaseString( s1) + "\n");
			}
			
			byte[] privateKey = null;
			byte[] publicKey = null;
			if (knownHost.getKeyPair() != null){
				privateKey = knownHost.getKeyPair().getPrivate().getEncoded();
				publicKey = knownHost.getKeyPair().getPublic().getEncoded();
			}
			
			byte[] foreignKey = null;
			if (knownHost.getForeignPublicKey() != null) foreignKey = knownHost.getForeignPublicKey().getEncoded();
			
			prep.setBytes(1, privateKey);
			prep.setBytes(2, publicKey);
			prep.setBytes(3, foreignKey);
			prep.setString(4, addresses.toString());
			prep.setBoolean(5, knownHost.isImportantHost());
			prep.setLong(6, knownHost.getLastAccess());
			prep.execute();
			int updateCount = prep.getUpdateCount();
			prep.close();
			if (updateCount == 0)
			{
				prep = connection.prepareStatement( "insert into " + TABLE_KNOWNHOSTS + " values (?, ?, ?, ?, ?, ?, ?);");
				
				prep.setString(1, knownHost.getUniqueHostId());
				prep.setBytes(2, privateKey);
				prep.setBytes(3, publicKey);
				prep.setBytes(4, foreignKey);
				prep.setString(5, addresses.toString());
				prep.setBoolean(6, knownHost.isImportantHost());
				prep.setLong(7, knownHost.getLastAccess());
				prep.execute();
				prep.close();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error setting knownHost " + knownHost.getUniqueHostId() + " at database " + getName(), e);			
		}
	}
	
	public void deletePeerAddress( PeerAddress peerAddress) throws Exception
	{
		Statement statement = connection.createStatement();
		try {
			statement.execute("DELETE FROM " + TABLE_DHTHOSTS + " WHERE peerId = '" + Base64.encode( peerAddress.getID().toByteArray()) + "';");
		} finally {
			statement.close();
		}
	}
	
	public InetSocketAddress getRandomPeerAddress()
	{		
		try {
			Statement stat = connection.createStatement();
			ResultSet rs = stat.executeQuery("SELECT address, port FROM " + TABLE_DHTHOSTS + " ORDER BY RANDOM() LIMIT 1;");
			try {
				if (!rs.isClosed() && (rs.getRow() != 0))
				{
					return new InetSocketAddress( rs.getString(1), rs.getInt(2));		
				}
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void insertPeerAddress( PeerAddress peerAddress)
	{
		try {
			PreparedStatement prep = connection.prepareStatement( "insert into " + TABLE_DHTHOSTS + " values (?, ?, ?);");
			prep.setString(1, Base64.encode( peerAddress.getID().toByteArray()));
			prep.setString(2, peerAddress.getInetAddress().getHostAddress());
			prep.setInt(3, peerAddress.portTCP());
			prep.execute();
			prep.close();			
		} catch (SQLException e) {
			//e.printStackTrace();
			//logger.error("Error setting peerAddress " + peerAddress.getID().toString() + " at database " + getName(), e);			
		}
	}
	
	public KeyPair getAsymmetricKeys(String userUID, boolean couldGenerateNew) throws Exception
	{
		KeyPair result = null;
		Statement stat = connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery("select privateKey, publicKey from " + TABLE_ASYMMETRIC_KEYS + " WHERE userUID = \"" + userUID + "\";");
			try {
				if (!rs.isClosed() && (rs.getRow() != 0))
				{
					PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec( rs.getBytes(1));
					X509EncodedKeySpec publicKey = new X509EncodedKeySpec( rs.getBytes(2));
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
		try {
			PreparedStatement prep = connection.prepareStatement(
			  "update " + TABLE_ASYMMETRIC_KEYS + " set privateKey = ?, publicKey = ?, updateTime = ? WHERE userUID = \"" + userUID+ "\";" );

			long updateTime = getUpdateTime();
			byte[] encPrivateKey = keyPair.getPrivate().getEncoded();
			byte[] encPublicKey = keyPair.getPublic().getEncoded();
			
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

}
