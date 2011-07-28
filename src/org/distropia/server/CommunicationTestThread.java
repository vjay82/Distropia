package org.distropia.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import org.distropia.server.communication.GetUserDatabaseRequest;
import org.distropia.server.database.UserDatabase;
import org.distropia.server.database.UserProfile;



public class CommunicationTestThread extends Thread {

	@Override
	public void run()  {
		setName("THISTHISTHIS");

		
		while (true){
			try {
				FileOutputStream fos = new FileOutputStream( "/tmp/fos");
				UserProfile u = Backend.getUserProfiles().get(0);
				u.login("DebugUser", "1234");
				
				UserProfile userProfile = new UserProfile( new File( "/tmp/user"));
				//userProfile.setUserPublicKey( u.getUserPublicKey());
				
				GetUserDatabaseRequest gudr = userProfile.createGetUserDatabaseRequest( false, false);
				u.getDataBaseDump( gudr.getDatabases(), gudr.getOnlyNewerItemsThan(), fos);
				fos.close();
				
				FileInputStream fin = new FileInputStream( "/tmp/fos");
				userProfile.updateDatabase( fin);
				fin.close();
				
				break;
			} catch (Exception e) {
				System.out.println("bums");
				e.printStackTrace();
			}
			break;
		}
		
	}

}
