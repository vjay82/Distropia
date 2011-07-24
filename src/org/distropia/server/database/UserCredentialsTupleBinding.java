package org.distropia.server.database;

import org.distropia.server.database.UserCredentials.Gender;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class UserCredentialsTupleBinding extends TupleBinding<UserCredentials> {
	
	@Override
	public UserCredentials entryToObject(TupleInput ti) {
		
			UserCredentials userCredentials = new UserCredentials();
			
			userCredentials.setGender( Gender.valueOf( ti.readString()));
			userCredentials.setFirstName( ti.readString());
			userCredentials.setSurName( ti.readString());
			userCredentials.setTitle( ti.readString());
			
			int length = ti.readInt();
			if (length > 0){
				userCredentials.setSmallPicture( new byte[ length]);
				ti.read( userCredentials.getSmallPicture());
			}
			
			length = ti.readInt();
			if (length > 0){
				userCredentials.setPicture( new byte[ length]);
				ti.read( userCredentials.getPicture());
			}
			
			userCredentials.setNamePublicVisible( ti.readBoolean());
			userCredentials.setPicturePublicVisible( ti.readBoolean());
			userCredentials.setAddressPublicVisible( ti.readBoolean());
			
			userCredentials.setStreet( ti.readString());
			userCredentials.setCity( ti.readString());
			userCredentials.setPostcode( ti.readString());
			
			return userCredentials;
	}

	@Override
	public void objectToEntry(UserCredentials userCredentials, TupleOutput to) {
		to.writeString( userCredentials.getGender().toString());
		to.writeString( userCredentials.getFirstName());
		to.writeString( userCredentials.getSurName());
		to.writeString( userCredentials.getTitle());
		
		if (userCredentials.getSmallPicture() != null){
			to.writeInt( userCredentials.getSmallPicture().length);
			to.write( userCredentials.getSmallPicture());
		}
		else to.writeInt( 0);
		
		if (userCredentials.getPicture() != null){
			to.writeInt( userCredentials.getPicture().length);
			to.write( userCredentials.getPicture());
		}
		else to.writeInt( 0);
		
		to.writeBoolean( userCredentials.isNamePublicVisible());
		to.writeBoolean( userCredentials.isPicturePublicVisible());
		to.writeBoolean( userCredentials.isAddressPublicVisible());
		to.writeString( userCredentials.getStreet());
		to.writeString( userCredentials.getCity());
		to.writeString( userCredentials.getPostcode());
	}
}
