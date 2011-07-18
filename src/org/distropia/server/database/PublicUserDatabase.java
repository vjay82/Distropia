package org.distropia.server.database;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;

public class PublicUserDatabase extends UserDatabase {
	private static final String TABLE_NEWSBOX = "NewsBox";
	
	@Override
	protected void createDefaultTables(Statement statement) throws SQLException {
		super.createDefaultTables(statement);
		statement.executeUpdate("create table if not exists " + TABLE_NEWSBOX + " (key INTEGER PRIMARY KEY, dependsOnKey INTEGER, newsType BYTE, content BLOB, password BLOB, updateTime INTEGER);");		
	}
	
	public PublicUserDatabase(File databaseFile) throws Exception {
		super(databaseFile);		
	}

}
