package org.distropia.server.communication;

import java.security.Key;

public class AesKeyExchangeResponse extends AesKeyExchangeRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5667273525821022963L;

	public AesKeyExchangeResponse() {
		super();
	}

	public AesKeyExchangeResponse(Key aesKey) {
		super(aesKey);
	}

}
