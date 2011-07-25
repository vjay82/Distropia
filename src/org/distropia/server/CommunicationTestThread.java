package org.distropia.server;

import java.util.ArrayList;
import java.util.Arrays;



public class CommunicationTestThread extends Thread {

	@Override
	public void run()  {
		setName("THISTHISTHIS");

		
		while (true){
			try {
				sleep(1000);
				if(Backend.getDHT().getContacts()>0){
					Backend.getDHT().searchUser("Volker Gronau");
				}
				
			} catch (Exception e) {
				System.out.println("bums");
			}
		}
		
	}

}
