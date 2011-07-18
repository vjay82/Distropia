package org.distropia.server;

import java.io.File;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xerces.impl.dv.util.Base64;
import org.distropia.client.Utils;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;
import org.distropia.server.communication.PingResponse;
import org.distropia.server.communication.ProxiedHosts;
import org.distropia.server.communication.dht.DHT;
import org.distropia.server.communication.lowlevel.Autoconf;
import org.distropia.server.database.CommunicationDatabase;
import org.distropia.server.database.UserProfiles;
import org.distropia.server.platformspecific.PlatformSpecific;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet implementation class Backend
 */

@SuppressWarnings("serial")
public class Backend extends HttpServlet implements ServletContextListener{
	
	// constants
	/**
	 * This is the name of the whole application, it is for defining a workdir path for example.
	 */
	public static final String APPLICATION_NAME = "Distropia";
	
	
	/**
	 * This is the actual branch this application is. It is for automatic updates to let another client
	 * decide if it wants to update from this client and vice versa.
	 */
	public static final String APPLICATION_BRANCH = "main";
	
	
	/**
	 * All encryptable objects (class EncryptableObject) include this version.
	 * So nearly everything contains it. Every network traffic for example. Maybe there will be a version 2, so version 2
	 * clients are able to see the other client doesn't support this or that
	 */
	public static final int PROTOCOL_VERSION = 1;	
	
	// debug
	/**
	 * Moves the workdir to /tmp/ 
	 */
	private static final boolean DEBUG_USEDEBUGFOLDER = false;
	public static final boolean DEBUG_DONT_REMOVE_KNOWNHOST_ADDRESSES = false;
	public static final boolean DEBUG_PRINT_DEBUGPAGE_ON_GET = true;
	public static final boolean DEBUG_DISABLE_PRIVATE_IP_CHECK = false;
	public static final boolean DEBUG_SHORT_PROXYCOMMANDS = false;
	public static final boolean DEBUG_SHOW_KNOWNHOST_SENDCOMMAND_STACKTRACES = false;
	
	
	// static
	protected static final Logger logger = LoggerFactory.getLogger(Backend.class);
	private static transient Backend instance = null;
	
	// instance
	private Configuration configuration = null;
	private PlatformSpecific platformSpecific = null;
	private String workDir = null;
	private UserProfiles userProfiles = null;
	private boolean thickClient = true; // if false (Android?) dont't serve others as proxy
	private CommunicationDatabase communicationDatabase = null;
	private String uniqueHostId = null;
	private String servletContextPath = "/Distropia/" + Backend.class.getSimpleName();
	private KnownHosts myKnownHosts = null;
	private ConnectionStatus connectionStatus = null;
	private ProxiedHosts proxiedHosts = null;
	private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, 5, 20000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	private DHT dht = null;
	private MaintenanceList maintenanceList = new MaintenanceList();
	private Autoconf autoconf = new Autoconf();
	private String connectionTestId = "";
	
	//private enum serverType { THIN, NORMAL, HELPER};
	
	
       
	public void init(ServletConfig config) throws ServletException {
    	super.init(config);
        instance= this;
        platformSpecific = PlatformSpecific.createPlatformSpecific( APPLICATION_NAME);
        
        try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			logger.error( "Error initializing SQLite", e1);
		}
        
		int port = 8080;
		
		boolean isTomcat = (getServletContext().getContextPath().contains("/Distropia"));
		if (isTomcat) //GWT.isProdMode())
		{
			logger.info("loading settings for production mode");
			
			servletContextPath = getServletContext().getContextPath() + "/" + Backend.class.getSimpleName();
			logger.info("getting WEB-INF path from servletContext");
			//String webInfPath = getServletContext().getRealPath("WEB-INF"); // only tomcat
			//workDir = new File(webInfPath).getParentFile().getParentFile().getParentFile().getParentFile().getPath() + fileSeparator + "workDir" + fileSeparator;
		}
		else // settings for GWT development mode
		{
			//String webInfPath = getServletContext().getRealPath("WEB-INF"); // only tomcat
			//logger.info("webInfPath:" + webInfPath);
			
			logger.info("loading settings for development mode");
			port = 8888;
			//workDir = new File(webInfPath).getParentFile().getParentFile().getPath() + fileSeparator + "workDir" + fileSeparator;
		}
		
		if (DEBUG_USEDEBUGFOLDER){
			workDir = System.getenv("TMPDIR");
			if (Utils.isNullOrEmpty( workDir)) workDir = System.getenv("TEMP");
			if (Utils.isNullOrEmpty( workDir)) workDir = System.getenv("TMP");
			workDir = workDir + File.pathSeparator + "/tmp/Distropia/";
		}
		else workDir = platformSpecific.getMyApplicationSupportFolder();
		
		try {
			File userProfileDirectory = new File( workDir + "userProfiles" + File.separator + "local");
			if ((!userProfileDirectory.exists()) && (!userProfileDirectory.mkdirs())) throw new Exception("Could not create path " + userProfileDirectory.getAbsolutePath());			
			logger.info("Loading user profiles from " + userProfileDirectory.getAbsolutePath());
			userProfiles = new UserProfiles( userProfileDirectory);
			maintenanceList.addWithWeakReference( userProfiles, 30000);
			
			
			File communicationDirectory = new File( workDir + "communication");
			if ((!communicationDirectory.exists()) && (!communicationDirectory.mkdirs())) throw new Exception("Could not create path " + communicationDirectory.getAbsolutePath());
			
			configuration = new Configuration( new File( workDir + "configuration.xml"));
			communicationDatabase = new CommunicationDatabase( new File( communicationDirectory.getAbsolutePath() + File.separator + "communicationDatabase.db"));
			uniqueHostId = communicationDatabase.getUniqueHostId();
			
			//dhtDatabase = new DHTDatabase( new File( communicationDirectory.getAbsolutePath() + getFileSeparator() + "dhtDatabase.db"));
			String dhtStorageDirectory = communicationDirectory.getAbsolutePath() + File.separator + "dht";
			File dhtStorageFile = new File(dhtStorageDirectory);
			if ((!dhtStorageFile.exists()) && (!dhtStorageFile.mkdirs()))  throw new Exception("Could not create path " + dhtStorageDirectory);
			dht = new DHT( communicationDatabase, dhtStorageDirectory);
			
			myKnownHosts = new KnownHosts( communicationDatabase);
			connectionStatus = new ConnectionStatus( port);
			
			getDHT().manageDHT( true);
		} catch (Throwable e) {
			if (logger != null) logger.error("Error while loading.", e);
			e.printStackTrace();
			throw new javax.servlet.ServletException("Error initializing", e);
		}
		
		logger.info("loaded BookService, contextpath:" + servletContextPath + " workDir:" + workDir);
		
//        CommunicationTestThread communicationTestThread = new CommunicationTestThread();
//        communicationTestThread.setDaemon( true);
//        communicationTestThread.start();
    }
    
	public void close() {
		if (maintenanceList != null)
		{
			logger.info("closing - maintenancelist");
			maintenanceList.close();
		}
		
		if (threadPoolExecutor != null){
			logger.info("closing - threadPoolExecutor");
			threadPoolExecutor.shutdown();
		}
		
		logger.info("closing - dht");
		dht.close();		
		
		if (isProxiedHosts()){
			logger.info("closing - proxiedHosts");
			proxiedHosts.close();
		}
		
		if (connectionStatus != null)
		{
			logger.info("closing - connectionStatus");
			connectionStatus.close();
			connectionStatus = null;
		}
		if (userProfiles != null)
		{
			logger.info("closing - userProfiles");
			userProfiles.close();
			userProfiles = null;
		}
		if (myKnownHosts != null)
		{
			logger.info("closing - knownHosts");
			myKnownHosts.close();
			myKnownHosts = null;
		} 
		if (communicationDatabase != null)
		{
			logger.info("closing - communicationDatabase");
			communicationDatabase.close();
			communicationDatabase = null;
		}
		
	}

	public static Backend getInstance() {
		return instance;
	}



	public UserProfiles getUserProfiles() {
		return userProfiles;
	}
	
	ArrayList<String> myLastTenRequests = new ArrayList<String>();

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if ("getConnectionTestId".equals(request.getHeader("command"))){
			response.setContentLength( connectionTestId.length());
			ServletOutputStream servletOutputStream = response.getOutputStream();
			servletOutputStream.print( connectionTestId);
			servletOutputStream.flush();
			servletOutputStream.close();
			return;
		}
		
		
		if (DEBUG_PRINT_DEBUGPAGE_ON_GET){
			StringBuilder body = new StringBuilder("<html><body><br>");
			
			body.append("My uniqueId: " + getUniqueHostId()+"<br>");
			body.append("Connected to Internet: " + getConnectionStatus().isConnectedToInternet()+"<br>");
			body.append("Reachable: " + getConnectionStatus().isReachable() + " InternetIP: " + getConnectionStatus().getInternetAddress()+"<br>");
			body.append("Port: " + getConnectionStatus().getInternetPort(true) + " External Port: " + getConnectionStatus().getExternalInternetPort() + "<br>");
			body.append("DHTPort: " + getDHT().getPort(true) + " External Port: " + getDHT().getExternalPort() +" Contact count:" + getDHT().getContacts() + "<br>");
			
			body.append("PossibleInternetIPS to check: " + Arrays.toString( getConnectionStatus().addressesToCheckIfOurs.toArray() )+"<br>");
			body.append("PossibleInternetIPS to already checked: " + Arrays.toString( getConnectionStatus().alreadyCheckedAddressesToBeOurs.toArray() )+"<br>");
			
			body.append("<br>");
			body.append("Last ten:<br>");
			for (String string: myLastTenRequests)
				body.append(string + "<br>");
			
			body.append("<br>");
			body.append("Local network addresses: " + Arrays.toString( getAutoconf().getLocalNetworkAddresses().toArray() )+"<br>");
			
			body.append("<br>");
			body.append("Known Hosts:<br>");
			ArrayList<KnownHost> knownHosts = getMyKnownHosts();
			synchronized (knownHosts) {
				for(KnownHost knownHost : knownHosts)
					body.append(" uid: " + knownHost.getUniqueHostId() + " Addresses: " + Arrays.toString( knownHost.getAddresses().toArray())+"<br>");
			}
			body.append("</body></html>");
			ServletOutputStream servletOutputStream = response.getOutputStream();
			servletOutputStream.print( body.toString());
			servletOutputStream.flush();
			servletOutputStream.close();
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/*
		String type = request.getHeader("type");
		
		if ("dht".equals( type))
		{
			if (transport != null)
				transport.processData( request.getRemoteAddr(), Integer.parseInt( request.getHeader("port")), org.apache.xerces.impl.dv.util.Base64.decode( request.getHeader("data")), response);			
			return;
		}
		*/
		String hostNameForLogging = request.getRemoteAddr();
		try {
			connectionStatus.setNextConnectionCheckToImmediateIfNotOnlineAndReachable(); // makes sense i think
			
			String uniqueHostId = request.getHeader("uid");
			if (uniqueHostId != null) 
			{
				synchronized (myKnownHosts) {
					myLastTenRequests.add( "IP:" + request.getRemoteAddr() + " UID: " + uniqueHostId);
					if (myLastTenRequests.size()>10) myLastTenRequests.remove(0);					
				}
				
				if ( uniqueHostId.equals( getUniqueHostId())) // nice try
				{
					logger.error("got request from identical unique host id");
					PingResponse pingResponse = new PingResponse( uniqueHostId);
					response.addHeader( "encrypted", "false");
					response.addHeader( "data", Base64.encode( pingResponse.toByteArray()));
					return;
				}
				
				hostNameForLogging = uniqueHostId;
				logger.info("got command from " + hostNameForLogging);
				// matching uid to knownHost
				KnownHost knownHost = myKnownHosts.getKnownHostOrNull(uniqueHostId);
				if (knownHost == null)
				{
					knownHost = new KnownHost( uniqueHostId);
					// process event before adding, if it is crap, we can forget about this contact (bruteforce?)
					if ( knownHost.processEvent( request, response))
					{
						synchronized (myKnownHosts) {
							if (!myKnownHosts.contains( knownHost))
								myKnownHosts.add( knownHost);
						}
					}
				}
				else knownHost.processEvent( request, response);
				
				
			}
			else logger.error("Got a request, but uid is missing!");
		} catch (Exception e) {
			logger.error("Error handling request from " + hostNameForLogging, e);
		}
	}

	/**
	 * Triggered from Connectionstatus if knownHosts is empty
	 */
	public void onNeedsBootstrap()
	{
		logger.info("onNeedsBootstrap - loading from database");
		try {
			communicationDatabase.loadKnownHostsForBootstap( myKnownHosts);
			logger.info("onNeedsBootstrap - success, loaded " + myKnownHosts.size() + " entries.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("onNeedsBootstrap - failed", e);
		}
		logger.info("onNeedsBootstrap - loading from config");
		try {
			// we can access knownHosts without syncing, because nothing is running at this stage
			ArrayList<String> hardcodedAddresses = getConfiguration().getBootstrapAddresses();
			boolean alreadyInThere = false;
			for(String address: hardcodedAddresses){
				for(KnownHost knownHost: getMyKnownHosts()){
					if (knownHost.getAddresses().contains( address)){
						alreadyInThere = true;
						break;
					}
				}
				if (!alreadyInThere){
					logger.info("adding hardcoded bootstrap address " + address);
					KnownHost knownHost = new KnownHost();
					knownHost.addAddress( address);
					getMyKnownHosts().add( knownHost);
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("onNeedsBootstrap - failed", e);
		}
	}
	
	public void setThickClient(boolean thickClient) {
		this.thickClient = thickClient;
	}

	public boolean isThickClient() {
		return thickClient;
	}

	public static CommunicationDatabase getCommunicationDatabase() {
		return getInstance().communicationDatabase;
	}

	public static String getUniqueHostId() {
		return getInstance().uniqueHostId;
	}

	public static String getServletContextPath() {
		return getInstance().servletContextPath;
	}
	
	public static KnownHosts getMyKnownHosts() {
		return getInstance().myKnownHosts;
	}
	
	public static ConnectionStatus getConnectionStatus() {
		return getInstance().connectionStatus;
	}
	
	public static ProxiedHosts getProxiedHosts(){
		if (instance.proxiedHosts == null) instance.proxiedHosts = new ProxiedHosts();
		return instance.proxiedHosts;
	}
	
	public static boolean isProxiedHosts(){
		return (instance.proxiedHosts != null);
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		if (instance != null) 
		{
			logger.info("starting closing");
			instance.close();
			// This manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrto this class
	        Enumeration<Driver> drivers = DriverManager.getDrivers();
	        while (drivers.hasMoreElements()) {
	            Driver driver = drivers.nextElement();
	            try {
	                DriverManager.deregisterDriver(driver);
	                logger.info(String.format("deregistering jdbc driver: %s", driver));
	            } catch (SQLException e) {
	            	logger.error(String.format("Error deregistering driver %s", driver), e);
	            } 
	        }
			logger.info("finished closing");
		}
		else logger.error("failed closing, because instance is null");
	}

	public static ThreadPoolExecutor getThreadPoolExecutor(){
		return getInstance().threadPoolExecutor;
	}
	
	public static MaintenanceList getMaintenanceList(){
		return getInstance().maintenanceList;
	}
	
	public static DHT getDHT(){
		return getInstance().dht;
	}
	
	public static Configuration getConfiguration(){
		return getInstance().configuration;
	}
	
	public static Autoconf getAutoconf(){
		return getInstance().autoconf;
	}
	
	public static void setConnectionTestId(String connectionTestId){
		getInstance().connectionTestId = connectionTestId;
	}
}
