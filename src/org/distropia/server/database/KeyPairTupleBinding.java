package org.distropia.server.database;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class KeyPairTupleBinding extends TupleBinding<KeyPair> {
	
	@Override
	public KeyPair entryToObject(TupleInput ti) {
		
		try{
			KeyFactory kf = KeyFactory.getInstance("RSA");
			int length = ti.readUnsignedShort();
			PKCS8EncodedKeySpec privateKey;
			if (length > 0){
				byte[] data = new byte[length];
				ti.read(data);
				privateKey = new PKCS8EncodedKeySpec( data);
			}
			else privateKey = null;
			length = ti.readUnsignedShort();
			X509EncodedKeySpec publicKey;
			if (length > 0){
				byte[] data = new byte[length];
				ti.read(data);
				publicKey = new X509EncodedKeySpec( data);
			}
			else publicKey = null;
			return new KeyPair(kf.generatePublic( publicKey), kf.generatePrivate( privateKey));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}

	@Override
	public void objectToEntry(KeyPair keyPair, TupleOutput to) {
		if (keyPair.getPrivate() != null){
			byte[] data = keyPair.getPrivate().getEncoded();
			to.writeUnsignedShort( data.length);
			to.write( data);
		}
		else to.writeUnsignedShort( 0);
		if (keyPair.getPublic() != null){
			byte[] data = keyPair.getPublic().getEncoded();
			to.writeUnsignedShort( data.length);
			to.write( data);
		}
		else to.writeUnsignedShort( 0);
	}
}
