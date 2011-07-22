package org.distropia.server.database;

import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class KnownUserTupleBinding extends TupleBinding<KnownUser> {
	
	@Override
	public KnownUser entryToObject(TupleInput ti) {
		
		try{
			KnownUser knownUser = new KnownUser();
			knownUser.setUniqueUserID( ti.readString());
			
			// public key
			int length = ti.readUnsignedShort();
			
			if (length > 0){
				byte[] data = new byte[length];
				ti.read(data);
				knownUser.setPublicKey( (PublicKey) new X509EncodedKeySpec( data));
			}
			knownUser.setPublicKey( null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}

	@Override
	public void objectToEntry(KnownUser knownUser, TupleOutput to) {
		to.writeString( knownUser.getUniqueUserID());
		if (knownUser.getPublicKey() != null){
			byte[] data = knownUser.getPublicKey().getEncoded();
			to.writeUnsignedShort( data.length);
			to.write( data);
		}
		else to.writeUnsignedShort( 0);
	}
}
