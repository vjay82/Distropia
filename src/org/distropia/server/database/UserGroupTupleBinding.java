package org.distropia.server.database;

import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class UserGroupTupleBinding extends TupleBinding<UserGroup> {

	@Override
	public UserGroup entryToObject(TupleInput ti) {
		UserGroup userGroup = new UserGroup( );
		
		try{
			// public key
			int length = ti.readUnsignedShort();
			
			if (length > 0){
				byte[] data = new byte[length];
				ti.read(data);
				userGroup.setSecretKey( new SecretKeySpec( data, "AES"));
			}
			userGroup.setSecretKey( null);
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		int length = ti.readInt();
		while(length>0){
			length--;
			userGroup.getMembers().add( ti.readString());
		}		
		return userGroup;
	}

	@Override
	public void objectToEntry(UserGroup userGroup, TupleOutput to) {
		if (userGroup.getSecretKey() != null){
			byte[] data = userGroup.getSecretKey().getEncoded();
			to.writeUnsignedShort( data.length);
			to.write( data);
		}
		else to.writeUnsignedShort( 0);
		List<String> members = userGroup.getMembers();
		to.writeInt( members.size());
		for (String address: members)
			to.writeString( address);
	}

}
