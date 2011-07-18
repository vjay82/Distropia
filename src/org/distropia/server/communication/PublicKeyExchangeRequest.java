package org.distropia.server.communication;



/**
 * @author vjay
 *
 * 
 */
public class PublicKeyExchangeRequest extends DefaultServerResponse{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2567968717440751146L;
	byte[] wrappedPublicKey;

	
	public byte[] getWrappedPublicKey() {
		return wrappedPublicKey;
	}


	public void setWrappedPublicKey(byte[] wrappedPublicKey) {
		this.wrappedPublicKey = wrappedPublicKey;
	}
	


	public PublicKeyExchangeRequest(byte[] wrappedPublicKey) {
		super();
		this.wrappedPublicKey = wrappedPublicKey;
	}


	public PublicKeyExchangeRequest() {
		super();
	}
	
}
