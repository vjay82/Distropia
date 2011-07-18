package org.distropia.server.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.distropia.server.Backend;
import org.distropia.server.Maintenanceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database implements Maintenanceable{
	private static final int TIMER_INTERVAL_MAINTENANCE = 60000 * 60 * 24; // every day
	private static final int TIMER_INTERVAL_VACUUM = 60000 * 60 * 24 * 7; // every week
	private static final String KEY_LASTVACUUM = "lastVacuum";
	
	protected Connection connection = null;
	protected static Logger logger = LoggerFactory.getLogger(UserProfile.class);
	protected File databaseFile = null;
	protected long lastVacuum = 0;

	public Statement createStatement() throws SQLException{
		return connection.createStatement();
	}
	
	protected void createDefaultTables( Statement statement) throws SQLException{
		statement.executeUpdate("create table if not exists Properties (key TEXT PRIMARY KEY, value TEXT, updateTime INTEGER);");
	}
	
	protected String filterDataBaseString( String input){
		return input.replace("\"", "").replace("'", "");
	}
	
	@Override
	public synchronized void maintenance() {
		if (lastVacuum < getUpdateTime() - ((long) TIMER_INTERVAL_VACUUM))
		{
			lastVacuum = getUpdateTime();
			try{
				setProperty(KEY_LASTVACUUM, lastVacuum);
				logger.info("Rebuilding database " + getName());
				Statement statement = connection.createStatement();
				try{
					statement.executeUpdate("VACUUM;");
				}
				finally{
					statement.close();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				logger.error("error performin maintencance", e);
			}
		}
	}
	
	public Database(File databaseFile) throws Exception {
		super();
		this.databaseFile = databaseFile;
		connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
		Statement statement = connection.createStatement();
		createDefaultTables(statement);
		statement.close();
		
		lastVacuum = getProperty(KEY_LASTVACUUM, getUpdateTime());
		maintenance();
		Backend.getMaintenanceList().addWithWeakReference( this, TIMER_INTERVAL_MAINTENANCE);
	}
	
	public long getProperty( String key, long defaultValue) throws Exception
	{
		return Long.valueOf( getProperty(key, String.valueOf(defaultValue)));
	}
	
	public int getProperty( String key, int defaultValue) throws Exception
	{
		return Integer.valueOf( getProperty(key, String.valueOf(defaultValue)));
	}
	
	public String getProperty( String key, String defaultValue) throws Exception
	{
		Statement stat = connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery("select value from Properties WHERE key = \"" + key + "\";");
			try {
				if (rs.isClosed()) return defaultValue;
				if (rs.getRow() == 0) return defaultValue;
				return rs.getString(1);
			} finally 
			{
				rs.close();
				stat.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return defaultValue;
		}
	}
	
	public void setProperty( String key, int value) throws Exception
	{
		setProperty(key, String.valueOf(value));
	}
	
	public void setProperty( String key, long value) throws Exception
	{
		setProperty(key, String.valueOf(value));
	}
	
	public long getUpdateTime()
	{
		return (new Date()).getTime();
	}
	
	public void setProperty( String key, String value) throws Exception
	{
		try {
			PreparedStatement prep = connection.prepareStatement(
			  "update Properties set value = ?, updateTime = ? WHERE key = \"" + key+ "\";" );

			long updateTime = getUpdateTime();
			prep.setString(1, value);
			prep.setLong(2, updateTime);
			int updateCount;
			try{
				prep.execute();
				updateCount = prep.getUpdateCount();
			}
			finally{
				prep.close();
			}
			
			if (updateCount == 0)
			{
				prep = connection.prepareStatement( "insert into Properties values (?, ?, ?);");
				
				prep.setString(1, key);
				prep.setString(2, value);
				prep.setLong(3, updateTime);
				try{
					prep.execute();
				}
				finally{
					prep.close();
				}
				
				logger.info("added property " + key + " at database " + getName());
			}
			else logger.info("updated property " + key + " at database " + getName());
			
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error setting property " + key + " at database " + getName(), e);			
		}
	}
	
	public void close()
	{
		try {
			if (connection != null)
			{
				connection.close();
				connection = null;
			}
		} catch (SQLException e) {
			logger.error( "Error closing connection at database " + getName(), e);
			e.printStackTrace();
		}
	}
	
	public String getName()
	{
		return databaseFile.getName();
	}
	
}
