package org.distropia.server.communication;

import java.security.Key;

public class AesKeyExchangeRequest extends DefaultServerResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5321516396572820329L;
	private Key aesKey;

	public Key getAesKey() {
		return aesKey;
	}

	public void setAesKey(Key aesKey) {
		this.aesKey = aesKey;
	}

	public AesKeyExchangeRequest(Key aesKey) {
		super();
		this.aesKey = aesKey;
	}

	public AesKeyExchangeRequest() {
		super();
	}
	
}
