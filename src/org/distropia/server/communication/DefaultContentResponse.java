package org.distropia.server.communication;

import java.io.OutputStream;

public class DefaultContentResponse extends DefaultServerResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9036669098004347476L;
	OutputStream outputStream;
	public OutputStream getOutputStream() {
		return outputStream;
	}
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	public DefaultContentResponse(OutputStream outputStream) {
		super();
		this.outputStream = outputStream;
	}
	public DefaultContentResponse() {
		super();
	}
	
}
