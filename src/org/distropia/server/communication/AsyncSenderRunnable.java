package org.distropia.server.communication;


public class AsyncSenderRunnable implements Runnable 
{
	@SuppressWarnings("rawtypes")
	protected AsyncCommandCallback asyncSimpleServerCommandCallback;
	protected KnownHost knownHost = null;
	protected DefaultServerRequest request;
	protected boolean needsSecureConnection;
	
	

	public AsyncSenderRunnable(
			AsyncCommandCallback<?> asyncSimpleServerCommandCallback,
			KnownHost knownHost, DefaultServerRequest request,
			boolean needsSecureConnection) {
		super();
		this.asyncSimpleServerCommandCallback = asyncSimpleServerCommandCallback;
		this.knownHost = knownHost;
		this.request = request;
		this.needsSecureConnection = needsSecureConnection;
	}



	@SuppressWarnings("unchecked")
	public void run() {
		try {
			if (needsSecureConnection && !knownHost.startSecureConnection()) throw new Exception("could not establish a secure connection");
			
			DefaultServerResponse result = knownHost.sendCommand( request);
			if (asyncSimpleServerCommandCallback != null) asyncSimpleServerCommandCallback.onSuccess( result);
		} catch (Exception e) {
			if (asyncSimpleServerCommandCallback != null) asyncSimpleServerCommandCallback.onFailure( e);
		}
	}
}