package org.distropia.server.communication;

import java.io.InputStream;

/**
 * @author vjay
 *
 *Not implemented sending big content stuff, yet
 */
public class DefaultContentRequest extends DefaultServerRequest{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3186638229844938392L;
	InputStream inputStream;
	public InputStream getInputStream() {
		return inputStream;
	}
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	public DefaultContentRequest() {
		super();
	}
	
}
