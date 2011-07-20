package org.distropia.server.database;

import java.io.File;

public class PublicUserDatabase extends UserDatabase {
	
	public PublicUserDatabase(File databaseFile) throws Exception {
		super(databaseFile);		
	}

}
