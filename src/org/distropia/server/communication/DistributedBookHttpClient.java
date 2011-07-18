package org.distropia.server.communication;

import java.io.IOException;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

public class DistributedBookHttpClient extends DefaultHttpClient {

	public DistributedBookHttpClient() {
		super();
		this.setHttpRequestRetryHandler( new HttpRequestRetryHandler() {
			
			@Override
			public boolean retryRequest(IOException arg0, int arg1, HttpContext arg2) {
				return false;
			}
		});
		this.getParams().setIntParameter("http.socket.timeout", ProxiedHosts.MAX_THREAD_WAITING_TIME + 10000);
	}

}
