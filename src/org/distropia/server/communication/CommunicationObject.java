package org.distropia.server.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import net.tomp2p.utils.Utils;

import org.apache.xerces.impl.dv.util.Base64;
import org.distropia.server.Backend;

public class EncryptableObject implements Serializable{

	
	private static final long serialVersionUID = -4361694718548726839L;
	protected int protocolVersion = Backend.PROTOCOL_VERSION;
	
	public EncryptableObject() {
		super();
	}
	
	public static EncryptableObject createFrom( boolean encrypted, byte[] data, Key key) throws Exception{
		if (encrypted)
		{
			return EncryptableObject.createFromEncryptedByteArray(key, data);
		}
		else
		{
			return EncryptableObject.createFromByteArray( data);
		}
	}
	
	public static EncryptableObject createFrom( boolean encrypted, String data, Key key) throws Exception{
		return createFrom(encrypted, Base64.decode( data), key);
	}
	
	public String toString( boolean encrypted, Key key) throws Exception{
		if (encrypted) return Base64.encode( encrypt( key));
		else return Base64.encode( toByteArray());
	}
	
	public static EncryptableObject createFromEncryptedByteArray (Key decryptKey, byte[] createFrom) throws Exception{
		ByteArrayInputStream bin = new ByteArrayInputStream( createFrom);
		byte[] encodedAESKey = new byte[256];
		bin.read(encodedAESKey);
		byte[] encodedObject = new byte[ bin.available()];
	    bin.read( encodedObject);
	    bin.close();
	    
	    Cipher cipher = Cipher.getInstance("RSA");
	    cipher.init( Cipher.UNWRAP_MODE, decryptKey);
	    Key aesKey = cipher.unwrap(encodedAESKey, "AES", Cipher.SECRET_KEY);
	    
	    return createFromByteArray( decrypt( encodedObject, aesKey));
	}
	
	public static EncryptableObject createFromByteArray( byte[] createFrom) throws Exception{
		return (EncryptableObject) Utils.decodeJavaObject(createFrom, 0, createFrom.length);
	}
	
	public byte[] toByteArray() throws IOException
	{
	    return Utils.encodeJavaObject( this);
	}
	
	public byte[] encrypt( Key encryptWithKey) throws Exception
	{
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
	    kgen.init( 256);
	    SecretKey aesKey = kgen.generateKey();
	    Cipher cipher = Cipher.getInstance("RSA");
	    cipher.init( Cipher.WRAP_MODE, encryptWithKey);
	    byte[] encodedAESKey = cipher.wrap( aesKey);
	    
	    byte[] encryptedObject = encrypt( toByteArray(), aesKey);
	    
	    ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
	    bos.write( encodedAESKey);
	    bos.write( encryptedObject);
	    byte[] result = bos.toByteArray();
	    
	    return result;
	}
	
	//byte test ;
	public static void main(String[] args) throws Exception{
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize( 2048);
	    KeyPair keyPair = keyPairGenerator.genKeyPair();
		
		EncryptableObject simpleServerCommand = new EncryptableObject();
		//simpleServerCommand.test = 15;
		byte[] enc = simpleServerCommand.encrypt( keyPair.getPublic());
		//SimpleServerCommand simpleServerCommand2 = 
				EncryptableObject.createFromEncryptedByteArray(keyPair.getPrivate(), enc);
		//System.out.println( simpleServerCommand2.test);
	}
	
	protected byte[] encrypt(byte[] in, Key key) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.ENCRYPT_MODE, key);
	    return cipher.doFinal( in);
	}
	
	protected String encrypt(String in, Key key) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
	    return Base64.encode(cipher.doFinal( in.getBytes("UTF-8")));
	}
	
	protected static byte[] decrypt(byte[] in, Key key) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.DECRYPT_MODE, key);
	    return cipher.doFinal( in);
	}
	
	protected static String decrypt(String in, Key key) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.DECRYPT_MODE, key);
	    return new String(cipher.doFinal( Base64.decode( in)), "UTF-8");
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}
	
	
}
