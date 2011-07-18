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
					//Backend.getDHT().publishMyAddresses( Backend.getConnectionStatus().getReachableAt());
					ArrayList<String> adr = Backend.getDHT().getAddressesOfKnownHost( Backend.getUniqueHostId());
					System.out.println("addresses: " + Arrays.toString( adr.toArray()));
				}
				
			} catch (Exception e) {
				System.out.println("bums");
			}
		}
		
	}

}
