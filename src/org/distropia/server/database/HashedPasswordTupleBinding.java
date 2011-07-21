package org.distropia.server.database;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class HashedPasswordTupleBinding extends TupleBinding<HashedPassword> {

	@Override
	public HashedPassword entryToObject(TupleInput ti) {
		HashedPassword hashedPassword = new HashedPassword();
		
		byte[] data = new byte[8];
		ti.read( data);
		hashedPassword.setSalt( data);
		
		data = new byte[20];
		ti.read( data);
		hashedPassword.setHash( data);
		
		return hashedPassword;
	}

	@Override
	public void objectToEntry(HashedPassword hashedPassword, TupleOutput to) {		
		to.write( hashedPassword.getSalt());
		to.write( hashedPassword.getHash());		
	}

}
