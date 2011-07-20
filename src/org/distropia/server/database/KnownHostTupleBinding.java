package org.distropia.server.database;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import org.distropia.server.communication.KnownHost;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class KnownHostTupleBinding extends TupleBinding<KnownHost> {

	@Override
	public KnownHost entryToObject(TupleInput ti) {
		KnownHost knownHost = new KnownHost( );
		
		knownHost.setLastAccess( ti.readLong());
		knownHost.setUniqueHostId( ti.readString());
		
		try{			
			KeyFactory kf = KeyFactory.getInstance("RSA");
			int length = ti.readInt();
			byte[] data = new byte[length];
			ti.read(data);
			PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec( data);
			length = ti.readInt();
			data = new byte[length];
			ti.read(data);
			X509EncodedKeySpec publicKey = new X509EncodedKeySpec( data);
			knownHost.setKeyPair( new KeyPair(kf.generatePublic( publicKey), kf.generatePrivate( privateKey)));
			
			length = ti.readInt();
			data = new byte[length];
			ti.read(data);
			if (data.length>0){				
				X509EncodedKeySpec foreignPublicKey = new X509EncodedKeySpec( data);
				knownHost.setForeignPublicKey( kf.generatePublic( foreignPublicKey));
			}
			
			length = ti.readInt();
			while(length>0){
				length--;
				knownHost.getAddresses().add( ti.readString());
			}
				
			return knownHost;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return knownHost;
	}

	@Override
	public void objectToEntry(KnownHost knownHost, TupleOutput to) {
		
		try {
			to.writeLong( knownHost.getLastAccess());
			to.writeString( knownHost.getUniqueHostId());
			byte[] data = knownHost.getKeyPair().getPrivate().getEncoded();
			to.writeInt( data.length);
			to.write( data);
			data = knownHost.getKeyPair().getPublic().getEncoded();
			to.writeInt( data.length);
			to.write( data);
			if (knownHost.getForeignPublicKey() != null) data = knownHost.getForeignPublicKey().getEncoded();
			else data = new byte[0];
			
			to.writeInt( data.length);
			to.write( data);
			
			List<String> addresses = knownHost.getAddresses();
			to.writeInt( addresses.size());
			for (String address: addresses)
				to.writeString( address);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
