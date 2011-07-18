package org.distropia.server.communication;



public class PublicKeyExchangeResponse extends PublicKeyExchangeRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8239294003627615387L;

	public PublicKeyExchangeResponse() {
		super();
	}

	public PublicKeyExchangeResponse(byte[] wrappedPublicKey) {
		super(wrappedPublicKey);
	}
	
	
}
