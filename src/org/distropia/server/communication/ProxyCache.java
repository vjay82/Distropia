package org.distropia.server.communication;

import java.util.ArrayList;

public class ProxyCache extends ArrayList<ProxyCacheEntry> {
	
	protected static final long MAX_REQUEST_SIZE_FOR_NORMAL_PROXYRESPONSE = 1024 * 100;
	protected static final long MAX_OVERALL_REQUEST_COUNT_FOR_PROXYRESPONSE = 10;
	protected static final long MAX_OVERALL_REQUEST_SIZE_FOR_PROXYRESPONSE = 1024 * 10;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5822832484304054899L;
	volatile int entryNumber = 0;
	
	private synchronized int getNextEntryNumber(){
		return ++entryNumber;
	}
	@Override
	public synchronized boolean add(ProxyCacheEntry e) {
		e.getWrappedServerCommand().setEntryNumber( getNextEntryNumber());
		return super.add(e);
	}
	
	@Override
	public synchronized boolean remove(Object o) {
		return super.remove(o);
	}
	
	public ProxyCacheEntry getFromEntryNumber(int entryNumber)
	{
		for(ProxyCacheEntry proxyCacheEntry: this)
			if (proxyCacheEntry.getWrappedServerCommand().getEntryNumber() == entryNumber)
				return proxyCacheEntry;
		return null;
	}
	
	public synchronized void processAnswerCommands(ProxyRequest proxyRequest){
		if (proxyRequest.getAnswerCommands() != null)
		{			
			for(WrappedServerCommandResponse wrappedServerCommandResponse: proxyRequest.getAnswerCommands())
			{
				int entry = wrappedServerCommandResponse.getEntryNumber();
				ProxyCacheEntry proxyCacheEntry = getFromEntryNumber(entry);
				
				if (proxyCacheEntry != null)
				{
					synchronized (proxyCacheEntry) {					
						remove( proxyCacheEntry);
						proxyCacheEntry.setWrappedServerCommandResponse(wrappedServerCommandResponse);
						proxyCacheEntry.setDone( true);
						proxyCacheEntry.notifyAll();
					}
				}
			}					
			
			proxyRequest.setAnswerCommands( null); // save memory :)
		}
	}

	public synchronized ProxyResponse getProxyResponse(boolean bigRequest){
		ProxyResponse proxyResponse = new ProxyResponse( true);		
		if (size() >0) // we can return directly
		{
			long responseSize = 0;
			int count = 1;
			for( ProxyCacheEntry proxyCacheEntry: this)
			{
				if (proxyCacheEntry.isProcessing()) continue;
				
				long requestSize = proxyCacheEntry.getRequestSize();
				
				if (requestSize > MAX_REQUEST_SIZE_FOR_NORMAL_PROXYRESPONSE)
				{
					if (bigRequest)
					{
						proxyCacheEntry.setProcessing( true);
						proxyResponse.getReceievedCommands().add( proxyCacheEntry.getWrappedServerCommand());
						return proxyResponse;
					}
					else proxyResponse.setHasABigRequestPending( true);
				}
				else if (!bigRequest)
				{
					proxyCacheEntry.setProcessing( true);
					proxyResponse.getReceievedCommands().add( proxyCacheEntry.getWrappedServerCommand());
					responseSize = responseSize + requestSize;
					if (count > MAX_OVERALL_REQUEST_COUNT_FOR_PROXYRESPONSE) break;
					if (responseSize > MAX_OVERALL_REQUEST_SIZE_FOR_PROXYRESPONSE) break; // concatenate some events, but also guarantee fast processing
					count++;
				}
			}
			if (proxyResponse.getReceievedCommands().size() > 0) return proxyResponse;
		}
		return null;
	}
	public synchronized void resetAllProcessingFlags() {
		for(ProxyCacheEntry proxyCacheEntry: this)
			proxyCacheEntry.setProcessing( false);		
	}
	
}
