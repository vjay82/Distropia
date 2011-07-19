package org.distropia.server.communication;

import java.io.InputStream;
import java.net.URI;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.distropia.client.Utils;
import org.distropia.server.Backend;
import org.distropia.server.database.EncryptableObject;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author vjay
 *
 */
public class KnownHost{
	
	//static

	//private static final long serialVersionUID = 5068079049515408445L;
	protected static Logger logger = LoggerFactory.getLogger(KnownHost.class);
	protected static final int MAX_PROXYCACHE_ITEMS = 40;
	protected static final long MAX_KEYSAVETIME_FOR_UNIMPORTANT_HOST = 60000 * 60 * 24; // 1 day
	protected static final String KEY_SECURING_CONNECTION_LOCK = "securingConnectionLockedUntil";
	protected static final int MAXIMUM_MESSAGE_SIZE = 1024*1024;
	
	// all properties that are not serialized (serialization was removed, but who knows, let that in)
	
	protected transient KeyPair keyPair = null; // $codepro.audit.disable transientFieldInNonSerializable
	protected transient PublicKey foreignPublicKey = null; // $codepro.audit.disable transientFieldInNonSerializable
	protected transient Map<String, Object> stateCache = new HashMap<String, Object>(); // $codepro.audit.disable transientFieldInNonSerializable
	protected transient boolean importantHost = false; // will be true if friend of a user is on this host (it will be saved longer in database) // $codepro.audit.disable transientFieldInNonSerializable
	protected transient boolean securingConnection = false; // $codepro.audit.disable transientFieldInNonSerializable
	protected transient KnownHosts knownHosts = null; // $codepro.audit.disable transientFieldInNonSerializable
	protected transient long nextDHTLookup = 0; // $codepro.audit.disable transientFieldInNonSerializable
	protected transient long lastAskedForBeingMyProxy = 0; // $codepro.audit.disable transientFieldInNonSerializable
	
	
	// other properties
	
	protected String uniqueHostId = null;
	
	/**
	 * Could contain addresses via the uid:// protocol. This says the node is only reachable indirectly through another node (not directly at the network)
	 */
	protected ArrayList<String> addresses = new ArrayList<String>();
	protected long lastAccess = 0;
	protected String lastValidAddress = null;
	
	
	public boolean isAskedForBeingMyProxy() {
		return lastAskedForBeingMyProxy > System.currentTimeMillis() - (6000000); // only ask every 10 minutes
	}

	public void setAskedForBeingMyProxy() {
		this.lastAskedForBeingMyProxy = System.currentTimeMillis();
	}

	protected synchronized void setSecuringConnection( boolean securingConnection)
	{
		this.securingConnection = securingConnection;
		if (securingConnection) stateCache.put(KEY_SECURING_CONNECTION_LOCK, Long.valueOf( System.currentTimeMillis()+20000)); // after 20 sec the lock is released
		else stateCache.remove(KEY_SECURING_CONNECTION_LOCK);
	}
	
	protected synchronized void setSecuringConnectionToFalseInOneSecond()
	{
		securingConnection = true;
		stateCache.put(KEY_SECURING_CONNECTION_LOCK, Long.valueOf( System.currentTimeMillis()+1000)); // after 20 sec the lock is released		
	}
	
	protected synchronized boolean isSecuringConnection()
	{
		if (!securingConnection) return false;
		Long lockUntil = (Long) stateCache.get(KEY_SECURING_CONNECTION_LOCK);
		if (lockUntil == null) securingConnection = false;
		else if (lockUntil.longValue() < System.currentTimeMillis()){
			securingConnection = false;
			stateCache.remove(KEY_SECURING_CONNECTION_LOCK);
		}
		return securingConnection;
	}
	
	public String getLastValidAddress() {
		return lastValidAddress;
	}

	public boolean couldBeReached(){
		return (isInProxyMode() || (getAddresses().size()>0) || isNextDHTLookUpAllowed());
	}
	
	public boolean isInForwardMode()
	{
		synchronized (addresses) {
			for(String address: addresses)
				if (address.startsWith("uid://")) return true;
		}
		return false;
	}
	
	public boolean isInProxyMode()
	{
		synchronized ( stateCache) {
			return stateCache.get("proxyCache") != null;			
		}		
	}
	
	public ProxyCache getProxyCache()
	{
		synchronized ( stateCache) {
			ProxyCache result = (ProxyCache) stateCache.get("proxyCache");
			if (result == null){
				result = new ProxyCache();
				stateCache.put("proxyCache", result);
			}
			return result;
		}		
	}
	
	public void updateLastAccess()
	{
		lastAccess = (new Date()).getTime();
	}
	
	public void addAddress( String address)
	{
		if (address == null) return;
		if ("".equals(address)) return;
		boolean changed = false;
		
		if (address.startsWith("uid://"))
		{
			// it is allowed to add the addresses of the proxy node to speed things up
			String[] uidHeader = address.split("\\|");
			if (knownHosts != null)
			{
				KnownHost knownHost = null;				
				try {
					synchronized (knownHosts) {
						knownHost = knownHosts.getKnownHostOrNull( uidHeader[0]);
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("error resolving uid protocol", e);
				}					
				if (knownHost != null)
				{
					synchronized ( knownHost) {
						for (int index= 1; index<uidHeader.length; index++)
							knownHost.addAddress( uidHeader[ index]);
					}
				}				
			}
			address = uidHeader[0];
		}
		
		synchronized (addresses) {
			if (!addresses.contains( address))
			{
				addresses.add( address);
				changed = true;
			}
		}
		logger.info("knownHost " + getUniqueHostId() + " added address:" + address);
		if (changed) notifyKnownHostsOfChange( false);
	}
	
	public void removeAddress( String address)
	{
		if (Backend.DEBUG_DONT_REMOVE_KNOWNHOST_ADDRESSES) return;
		boolean changed = false;
		synchronized (addresses) {
			if (addresses.contains(address))
			{
				addresses.remove( address);
				changed = true;
			}
		}
		if (changed) notifyKnownHostsOfChange( false);
	}
	
	public boolean isImportantEnoughForDatabase(){
		if (uniqueHostId == null) return false;
		return isImportantHost() || isSecureConnection() || (knownHosts.indexOf( this)<= KnownHosts.MINIMUM_LIST_SIZE);
	}
	
	/**
	 * @param important if set, immediate save is triggered, otherwise it will be cached
	 */
	protected void notifyKnownHostsOfChange( boolean important)
	{
		updateLastAccess();
		
		if (logger.isDebugEnabled())
			logger.info("notifyKnownHostsOfChange called, secureConnection: " + isSecureConnection() + " knownHostsIsNull: " + (knownHosts==null) );
		
		if ((knownHosts != null) && isImportantEnoughForDatabase()){
			knownHosts.onKnownHostModified( this, important);
		}
	}
	
	public boolean isSecureConnection(){
		return (getForeignPublicKey() != null);
	}
	
	/**
	 * Be carefull, if the other host is not doing the same, no more communication will be possible!
	 */
	public void removeSecureConnection(){
		if (foreignPublicKey != null)
		{
			foreignPublicKey = null;
			//notifyKnownHostsOfChange( true); // wouldnt save it anymore, so we have to call directly
			if (knownHosts != null) knownHosts.onKnownHostModified( this, true);
		}
	}
	
	public synchronized boolean startSecureConnection(){
		if (!isSecureConnection())
		{
			setSecuringConnection( true);
							
			try {
				// start secureConnection by sending 2 requests
				// when we have a direct connection it doesn't matter
				// when we have indirect connections both requests go over different relays, so a relay alone never gets the key
				KeyGenerator kgen = KeyGenerator.getInstance("AES");
			    kgen.init( 256);
			    SecretKey aesKey = kgen.generateKey();
			    Cipher cipher = Cipher.getInstance("AES");
			    cipher.init( Cipher.WRAP_MODE, aesKey);
				
			    // first request
			    AesKeyExchangeResponse aesKeyExchangeResponse = (AesKeyExchangeResponse) sendCommand( new AesKeyExchangeRequest( aesKey), true, true);
				Key responseAesKey = aesKeyExchangeResponse.getAesKey();
			    
				// second request
				PublicKeyExchangeResponse publicKeyExchangeResponse = (PublicKeyExchangeResponse) sendCommand( new PublicKeyExchangeRequest( cipher.wrap( getKeyPair().getPublic())), false, true);
				byte[] responseWrappedPublicKey = publicKeyExchangeResponse.getWrappedPublicKey();
				
				cipher = Cipher.getInstance("AES");
			    cipher.init( Cipher.UNWRAP_MODE, responseAesKey);
				foreignPublicKey = (PublicKey) cipher.unwrap( responseWrappedPublicKey, "RSA", Cipher.PUBLIC_KEY);
				
				if (foreignPublicKey != null) 
				{
					notifyKnownHostsOfChange( true); // immediate save to database
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Error starting secure connection", e);				
			}
			finally{
				setSecuringConnection( false);
			}
			return false;
			
		}
		else return true;
	}
	
	public static String buildAddress(String protocol, String host, int port)
	{
		return protocol + "://" + host + ":" + port;
	}
	
	private AesKeyExchangeResponse processEventAesKeyExchangeRequest( AesKeyExchangeRequest aesKeyExchangeRequest) throws Exception{
		setSecuringConnection( true);
		//save aes key for the following PublicKeyExchangeRequest
		synchronized (stateCache) {
			stateCache.put("aeskey1", aesKeyExchangeRequest.getAesKey());
		}
				
		// return own aes key back, we encrypt our public key with it
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
	    kgen.init( 256);
	    SecretKey aesKey = kgen.generateKey();
	    synchronized (stateCache) {
	    	stateCache.put("aeskey2", aesKey);
	    }
		return new AesKeyExchangeResponse( aesKey);
	}
	
	private synchronized PublicKeyExchangeResponse processEventPublicKeyExchangeRequest( PublicKeyExchangeRequest publicKeyExchangeRequest) throws Exception{
		Key aesKey;
		synchronized (stateCache) {
			aesKey = (Key)stateCache.get("aeskey1");
		}
		Cipher cipher = Cipher.getInstance("AES");
	    cipher.init( Cipher.UNWRAP_MODE, aesKey);
		foreignPublicKey = (PublicKey) cipher.unwrap( publicKeyExchangeRequest.getWrappedPublicKey(), "RSA", Cipher.PUBLIC_KEY);
		
		synchronized (stateCache) {
			aesKey = (Key)stateCache.get("aeskey2");
		}
		cipher.init( Cipher.WRAP_MODE, aesKey);
		notifyKnownHostsOfChange( true); // immediate save to database
		setSecuringConnectionToFalseInOneSecond(); // the result takes a moment to the sending host, ensure it won't be overtaken by a sendCommand
		return new PublicKeyExchangeResponse( cipher.wrap( getKeyPair().getPublic()));
	}
	
	public DefaultServerResponse processEvent( boolean encrypted, byte[] data) throws Exception{
		DefaultServerRequest defaultServerRequest = (DefaultServerRequest)EncryptableObject.createFrom(encrypted, data, getKeyPair().getPrivate());
		Backend.getConnectionStatus().addAddressThatCouldBeOurs( defaultServerRequest.getYourAddress());
		return processEvent( defaultServerRequest);
	}
	
	public DefaultServerResponse processEvent(DefaultServerRequest request) throws Exception{
		if (request != null)
		{
			logger.info("processing event of type " + request.getClass().getSimpleName());
			
			if (request instanceof PingRequest)
			{
				return new PingResponse( Backend.getUniqueHostId());
			}
			else if (request instanceof GiveMeYourDHTPortRequest)
			{
				return new GiveMeYourDHTPortResponse( Backend.getDHT().getPort());
			}
			else if (request instanceof AesKeyExchangeRequest)
			{
				return processEventAesKeyExchangeRequest( (AesKeyExchangeRequest) request);
			}
			else if (request instanceof PublicKeyExchangeRequest)
			{
				return processEventPublicKeyExchangeRequest( (PublicKeyExchangeRequest) request);
			}
			else if (request instanceof ProxyRequest)
			{
				if (isSecureConnection())
				{
					return processEventProxyRequest( (ProxyRequest) request);					
				}
				else{
					logger.error(uniqueHostId + " tried ProxyRequest on unsecure connection");
					
				}
			}
			else if (request instanceof WrappedServerCommandRequest)
			{
				if (isSecureConnection())
				{
					return processEventWrappedServerCommandRequest( (WrappedServerCommandRequest) request);				
				}
				else{
					logger.error(uniqueHostId + " tried WrappedServerCommandRequest on unsecure connection");
					
				}
			}
			else if (request instanceof HereAreMyAddressesRequest)
			{
				if (isSecureConnection())
				{
					return processEventHereAreMyAddressesRequest( (HereAreMyAddressesRequest) request);				
				}
				else{
					logger.error(uniqueHostId + " tried WrappedServerCommandRequest on unsecure connection");
					
				}
			}
		}
		return null;
	}
	
	private DefaultServerResponse processEventHereAreMyAddressesRequest( HereAreMyAddressesRequest request) {
		synchronized (addresses) {
			addresses.clear();
			addresses.addAll( request.getAddresses());
		}
		return null;
	}

	public boolean processEvent( HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception{		
		String strEncrypted = httpRequest.getHeader("encrypted");		
		if (strEncrypted == null) throw new Exception("Command - Field encrypted is null.");
		boolean encrypted = "1".equals( strEncrypted);
		
		long contentLength = httpRequest.getContentLength();
		byte[] data = new byte[ (int) Math.min( contentLength, MAXIMUM_MESSAGE_SIZE)];
		InputStream inputStream = httpRequest.getInputStream();
		try
		{
			int readCount = inputStream.read( data);
			//if (readCount != data.length) throw new Exception("content length did not match, got " + readCount + " bytes, expected " + data.length);
		}
		finally{
			inputStream.close();
		}
		
		DefaultServerResponse response = processEvent( encrypted, data);
		
		if (response != null)
		{
			// tell the sender from where he came, if he did not come from a private ip (=our own router)
			if (!Backend.getAutoconf().isThisAPrivateNetwork(httpRequest.getRemoteAddr())) 
				response.setYourAddress( httpRequest.getRemoteAddr());
			
			//httpResponse.setContentType("text/html");
			data = null;
			if (!isSecureConnection()) // no encryption possible
			{
				httpResponse.addHeader( "encrypted", "0");
				data = response.toByteArray();
			}
			else
			{
				httpResponse.addHeader( "encrypted", "1");
				data = response.encrypt(foreignPublicKey);
			}
			httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( data.length));
			httpResponse.setStatus( HttpServletResponse.SC_OK);
			httpResponse.getOutputStream().write( data);
			httpResponse.getOutputStream().flush();
			httpResponse.getOutputStream().close();

			notifyKnownHostsOfChange( false);
			return true;
		}
		
		httpResponse.setStatus( HttpServletResponse.SC_BAD_REQUEST);
		return false;
	}
	
	private WrappedServerCommandResponse processEventWrappedServerCommandRequest( WrappedServerCommandRequest request) throws Exception {
		
		if (!Backend.isProxiedHosts()){
			logger.error("did not found proxied host for uid " + request.getForUniqueHostId() + " because isProxiedHosts() is false");
			return null;
		}
		KnownHost knownHost = Backend.getProxiedHosts().getFromUniqueHostId( request.getForUniqueHostId());
		if (knownHost == null){
			logger.error("did not found proxied host for uid " + request.getForUniqueHostId());
			return null;
		}
		
		ArrayList<ProxyCacheEntry> proxyCache = knownHost.getProxyCache();
		ProxyCacheEntry proxyCacheEntry = null;
		synchronized (proxyCache) {
			if (proxyCache.size() < MAX_PROXYCACHE_ITEMS)
			{
				proxyCacheEntry = new ProxyCacheEntry( request, Thread.currentThread());
				proxyCache.add( proxyCacheEntry);
			}
			else{
				logger.error("proxyCache overloaded for uid " + request.getForUniqueHostId());
				return null;
			}
		}
		
		synchronized (proxyCacheEntry) {
			synchronized ( knownHost.stateCache) {
				Thread dispatchThread = (Thread) knownHost.stateCache.get("proxyDispatchThread");
				if (dispatchThread != null)
				{
					synchronized (dispatchThread) {
						dispatchThread.notifyAll();
					}
				}
			}
			try {
				proxyCacheEntry.wait();
			} catch (InterruptedException e) {
				proxyCache.remove( proxyCacheEntry);
				return null;
			}
		}
		
		if (!proxyCacheEntry.done){
			logger.error("Proxied command was not done.");
			return null;
		}
		else{
			logger.info("Proxied command was successful.");
			return proxyCacheEntry.getWrappedServerCommandResponse();
		}
	}
	
	private DefaultServerResponse processProxyRequest(ProxyRequest proxyRequest) throws Exception
	{
		boolean kickHim;
		synchronized ( stateCache) {
			kickHim = !stateCache.get("proxy_contractId").equals( proxyRequest.getContractId()); // somebody tries to steal the connection
		}
		if (kickHim){
			logger.error("got invalid contractid, kicking client (got " + proxyRequest.getContractId() + ", have " + stateCache.get("proxy_contractId") + ")");
			Thread.sleep(1000);
			return new ProxyResponse( false); // bye
		}
		
		
		getProxyCache().processAnswerCommands(proxyRequest); // process returned events
		
		// send new events
		
		if (proxyRequest.isDownloadOneBigRequestMode())
		{
			ProxyResponse proxyResponse = getProxyCache().getProxyResponse( true);
			if (proxyResponse != null) return proxyResponse;
			return new ProxyResponse( false);
		}
		else
		{	
			Object waitObject = Thread.currentThread();
			synchronized (waitObject)
			{
				
				synchronized ( stateCache) {
					Thread oldThread = (Thread) stateCache.get("proxyDispatchThread");
					if (oldThread != null) // we have already a thread waiting, this should not happen, it seems we had a disconnect, reset every object
					{
						logger.error("Host " + uniqueHostId + " tries to wait for proxyEvents, but he is already waiting for them. Detected error, resetting states");
						getProxyCache().resetAllProcessingFlags();
						try {
							oldThread.interrupt();
						} catch (Exception e) {
							logger.error("exeption while interrupting old proxyDispatchThread", e);
						}
					}
					stateCache.put("proxyDispatchThread", waitObject);
				}
				
				try
				{					
					ProxyResponse proxyResponse = getProxyCache().getProxyResponse( false);
					if (proxyResponse != null){
						return proxyResponse;
					}
					
					// no items in proxyCache, we wait
					try {
						if (Backend.DEBUG_SHORT_PROXYCOMMANDS) waitObject.wait( 1000);//ProxiedHosts.MAX_THREAD_WAITING_TIME);
						else waitObject.wait( ProxiedHosts.MAX_THREAD_WAITING_TIME);
						proxyResponse = getProxyCache().getProxyResponse( false);
						if (proxyResponse != null) return proxyResponse;
					} catch (InterruptedException e1) {
						logger.error("proxyDispatchThread was interrupted");
					}
					
					
				}
				finally
				{
					// always remove
					synchronized ( stateCache) {
						if (stateCache.get("proxyDispatchThread") == waitObject)
							stateCache.remove("proxyDispatchThread");
					}
				
				}
			}
		}
				
				
		
		// no request after waiting, releasing request
		return new ProxyResponse( true);
	}

	private DefaultServerResponse processEventProxyRequest( ProxyRequest request) throws Exception 
	{
		ProxiedHosts proxiedHosts = Backend.getProxiedHosts();
		boolean runProccessProxyRequest = false;
		synchronized (proxiedHosts) {
			proxiedHosts.maintenance();				
			if (proxiedHosts.containsAndUpdateLastAccess( this)) runProccessProxyRequest = true;
			else if ((request.getContractId() == null) && proxiedHosts.add( this)) // try starting proxing
			{
				logger.info( getUniqueHostId() + " is entering proxy - mode");
				ProxyResponse proxyResponse = new ProxyResponse( true);
				proxyResponse.setContractId( java.util.UUID.randomUUID().toString().replaceAll("-", ""));
				synchronized ( stateCache) {
					stateCache.put("proxy_contractId", proxyResponse.getContractId());
				}
				return proxyResponse;
			}			
		}
		if (runProccessProxyRequest) return processProxyRequest(request);
		return new ProxyResponse( false);
	}
	
	public boolean sendCommandAsync( DefaultServerRequest request, AsyncCommandCallback<?> asyncSimpleServerCommandCallback)
	{
		return sendCommandAsync(request, false, asyncSimpleServerCommandCallback);
	}
	
	public boolean sendCommandAsync( DefaultServerRequest request, boolean needsSecureConnection, AsyncCommandCallback<?> asyncSimpleServerCommandCallback)
	{
		if (getAddresses().size() > 0)
		{
			Backend.getThreadPoolExecutor().execute( new AsyncSenderRunnable(asyncSimpleServerCommandCallback, this, request, needsSecureConnection));
			return true;
		}
		return false;
	}
	
	public DefaultServerResponse sendCommand( DefaultServerRequest request) throws Exception{
		// don't send commands while another thread is securing the connection
		// at that state keys are exchanged and there is a time window, where commands could fail
		while (isSecuringConnection()) Thread.sleep(100);
		
		return sendCommand( request, false, false);
	}
	
	
	/**
	 * @param request
	 * @param reverse
	 * @param dontShuffle if is proxyInForwardMode the addresses will be shuffled to not send everything through one host
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private DefaultServerResponse sendCommand( DefaultServerRequest request, boolean reverse, boolean dontShuffle) throws Exception{
		if (isInProxyMode()) // proxied host, the host is comming to us
		{
			logger.info("sending command - via proxylist");
			boolean encrypted;
			byte[] data;
			if (!isSecureConnection()) // no encryption possible
			{
				encrypted = false;
				data = request.toByteArray();
			}
			else
			{
				encrypted = true;
				data = request.encrypt( foreignPublicKey);
			}
			WrappedServerCommandResponse wrappedServerCommandResponse = processEventWrappedServerCommandRequest( new WrappedServerCommandRequest( uniqueHostId, Backend.getUniqueHostId(), encrypted, data));
			if (wrappedServerCommandResponse == null) throw new Exception("No answer.");

			notifyKnownHostsOfChange( false);
			return (DefaultServerResponse) EncryptableObject.createFrom( wrappedServerCommandResponse.getEncrypted(), wrappedServerCommandResponse.getData(), getKeyPair().getPrivate());			
		}
		else logger.info("sending command - normal mode");
		
		ArrayList<String> myAddresses = null;
		synchronized (addresses) {
			myAddresses = (ArrayList<String>) addresses.clone();			
		}
		
		if (isInForwardMode() && !dontShuffle){
			Collections.shuffle( myAddresses);
		}
			
		
		if (reverse) {
			Collections.reverse( myAddresses);
		}
		
		for(String address: myAddresses)
		{
			try {
				EncryptableObject result = sendCommandToAddress( address, request);

				notifyKnownHostsOfChange( false);
				lastValidAddress = address;
				return (DefaultServerResponse) result;
			} catch (Exception e) {
				if (Backend.DEBUG_SHOW_KNOWNHOST_SENDCOMMAND_STACKTRACES) e.printStackTrace();
				if (Backend.getConnectionStatus() != null)
					Backend.getConnectionStatus().removeAddressFromHostIfOnline( this, address);
			}
		}
		
		// so there were no addresses, which let to the host, maybe a DHT lookup could help?
		if (isNextDHTLookUpAllowed()){
			logger.info( "looking up addresses for knownhost " + toString());
			nextDHTLookup = System.currentTimeMillis() + 60000; // only lookup a host once a minute
			myAddresses = Backend.getDHT().getAddressesOfKnownHost(uniqueHostId);
			if ((myAddresses != null) && (myAddresses.size() > 0))
			{
				logger.info( "looking up addresses for knownhost " + toString() + " success, got: " + myAddresses.size());
				synchronized (addresses) {
					for(String address: myAddresses)
					 addAddress( address);	
				}
				return sendCommand(request); // call ourself
			}
			else logger.info( "looking up addresses for knownhost " + toString() + " failed");
		}
		
		throw new Exception("No reachable server address.");
	}
	
	public boolean isNextDHTLookUpAllowed(){
		return (/*Backend.getDHT().isActive() &&*/ nextDHTLookup < System.currentTimeMillis());
	}
	
	private DefaultServerResponse sendCommandToAddress( String address, DefaultServerRequest request) throws Exception
	{
		URI uri = new URI( address);
		request.setYourAddress( uri.getHost());
		
		boolean encrypted;
		byte[] data;
		if (!isSecureConnection()) // no encryption possible
		{
			encrypted = false;
			data = request.toByteArray();
		}
		else
		{
			encrypted = true;
			data = request.encrypt( foreignPublicKey);
		}
	
		if (address.startsWith("uid://")) // this command goes over another knownHost
		{
			logger.info("sending command " + request.getClass().getSimpleName() + " to " + address + " (UID mode!)");
			
			try
			{
				String proxyUID = address.substring(6);
				WrappedServerCommandRequest wrappedServerCommand = new WrappedServerCommandRequest(uniqueHostId, Backend.getUniqueHostId(), encrypted, data);
				KnownHost proxyHost = knownHosts.getKnownHostOrNull(proxyUID);
				if (proxyHost == null){
					throw new Exception("Proxy host " + proxyUID + " was null!");
				}
				if (proxyHost.startSecureConnection())
				{
					WrappedServerCommandResponse wrappedServerCommandResponse = (WrappedServerCommandResponse) proxyHost.sendCommand(wrappedServerCommand);
					if (wrappedServerCommandResponse == null) throw new Exception("Got Null response.");
					return (DefaultServerResponse) EncryptableObject.createFrom( wrappedServerCommandResponse.getEncrypted(), wrappedServerCommandResponse.getData(), getKeyPair().getPrivate());
				}
				else{
					throw new Exception( "error securing connection with proxy " + proxyUID + " for communication with " + uniqueHostId);
				}
			}
			catch (Exception e) {
				Backend.getConnectionStatus().removeAddressFromHostIfOnline( this, address);
				throw e;
			}
		}
		else // normal command
		{
			HttpClient httpClient = new DistributedBookHttpClient();
			HttpPost httpPost = new HttpPost( address + Backend.getServletContextPath());
			httpPost.addHeader( "uid", Backend.getUniqueHostId());
			
			if ( request instanceof PingRequest){
				httpPost.getParams().setIntParameter("http.socket.timeout", 5000);
			}
			
			if (encrypted) httpPost.addHeader( "encrypted", "1");
			else httpPost.addHeader( "encrypted", "0");
			httpPost.setEntity( new ByteArrayEntity( data));
			
			logger.info("sending command " + request.getClass().getSimpleName() + " to " + address + Backend.getServletContextPath() + " encrypted: " + String.valueOf( encrypted));
			
			HttpResponse response = httpClient.execute( httpPost);
			
			if( response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK)
			{
				boolean responseEncrypted = "1".equals( response.getLastHeader("encrypted").getValue());
				HttpEntity entity = response.getEntity();
				long contentLength = entity.getContentLength();
				byte[] responseData = new byte[ (int) Math.min( contentLength, MAXIMUM_MESSAGE_SIZE)];
				InputStream inputStream = entity.getContent();
				try
				{
					inputStream.read( responseData); // throw new Exception("content length did not match, message too big?");
				}
				finally{
					inputStream.close();
				}
				DefaultServerResponse defaultServerResponse = (DefaultServerResponse) EncryptableObject.createFrom(responseEncrypted, responseData, getKeyPair().getPrivate());
				if ((defaultServerResponse != null) && (defaultServerResponse.getYourAddress() != null))
					Backend.getConnectionStatus().addAddressThatCouldBeOurs( defaultServerResponse.getYourAddress());
				return defaultServerResponse;
			}
			throw new Exception( "got no or bad response for request to " + address);
		}
	}
	
	public PingResponse ping() throws Exception{
		PingResponse pingResponse = (PingResponse) sendCommand( new PingRequest());
		if (Utils.isNullOrEmpty(uniqueHostId)){
			setUniqueHostId( pingResponse.getPingResponseFromUniqueHostID());
			if (!pingResponse.getPingResponseFromUniqueHostID().equals( Backend.getUniqueHostId())) notifyKnownHostsOfChange( true);
		}
		else if ( !uniqueHostId.equals( pingResponse.getPingResponseFromUniqueHostID())) throw new Exception("Answer from wrong host.");
		return pingResponse;
	}
	
	public void requestForeignPublicKey() throws Exception
	{
		sendCommand( new PublicKeyExchangeRequest());
	}
	
	public KeyPair getKeyPair() throws NoSuchAlgorithmException
	{
		if (keyPair== null)
		{		
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize( 2048);
		    keyPair = keyPairGenerator.genKeyPair();
		    notifyKnownHostsOfChange( true);
		}
		return keyPair;	    
	}
	
	public String getAddressOrNull( int index)
	{
		if (index >= getAddresses().size()) return null;
		if (index < 0) return null;
		return getAddresses().get(index);
	}
	
	public String getUniqueHostId() {
		return uniqueHostId;
	}
	public void setUniqueHostId(String uniqueHostId) {
		this.uniqueHostId = uniqueHostId;
	}
	
	public List<String> getAddresses() {
		return addresses;
	}

	public void setKeyPair(KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	public PublicKey getForeignPublicKey() {
		if ((!isImportantHost()) && (getLastAccess() + MAX_KEYSAVETIME_FOR_UNIMPORTANT_HOST < (new Date()).getTime()))
		{
			removeSecureConnection(); // remove key
		}		
		return foreignPublicKey;
	}

	public void setForeignPublicKey(PublicKey foreignPublicKey) {
		this.foreignPublicKey = foreignPublicKey;
	}

	public KnownHost() {
		super();
	}

	public KnownHost(KnownHosts knownHosts, String uniqueHostId) {
		super();
		this.knownHosts = knownHosts;
		this.uniqueHostId = uniqueHostId;
	}

	public KnownHost(String uniqueHostId) {
		super();
		this.uniqueHostId = uniqueHostId;
	}

	public KnownHost( KnownHosts knownHosts) {
		super();
		this.knownHosts = knownHosts;
	}

	public long getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(long lastUpdate) {
		this.lastAccess = lastUpdate;
	}

	public boolean isImportantHost() {
		return importantHost;
	}

	public void setImportantHost(boolean importantHost) {
		this.importantHost = importantHost;
	}

	public KnownHosts getKnownHosts() {
		return knownHosts;
	}

	public void setKnownHosts(KnownHosts knownHosts) {
		this.knownHosts = knownHosts;
	}

	public void closeProxyCache(){
		logger.info("closing proxy cache");
		
		if (isInProxyMode()){
			ProxyCache proxyCache = getProxyCache();
			synchronized (proxyCache) {
				for(ProxyCacheEntry proxyCacheEntry: proxyCache)
					if (proxyCacheEntry.getSendingThread() != null)
						proxyCacheEntry.getSendingThread().interrupt();
			}
			synchronized ( stateCache) {
				Thread proxyDispatchThread = (Thread) stateCache.get("proxyDispatchThread");
				if ( proxyDispatchThread != null) // we have already a thread waiting, this should not happen, DOS attack?
				{
					proxyDispatchThread.interrupt();
				}
				stateCache.remove("proxy_contractId");
			}
		}
	}
	
	public void close() {
		if (isInProxyMode())
		{
			if (Backend.isProxiedHosts())
				Backend.getProxiedHosts().remove( this);
		}
		
	}
	
	@Override
	public String toString() {
		if (uniqueHostId != null) return uniqueHostId;
		return "No unique ID";
	}
	
}
