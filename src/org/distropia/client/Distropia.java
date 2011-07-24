package org.distropia.client;



import java.util.Map;

import org.distropia.client.gui.LoginPage;
import org.distropia.client.gui.Page;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Dialog;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Distropia implements EntryPoint {
	
	private String sessionId = null;
	private String loggedInUniqueUserId = null;
	private Page currentPage = null;
	private static Distropia instance = null;
	private static boolean debug = !GWT.isProdMode();
	private Map<String, String> clientDataStore = new java.util.HashMap<String, String>();
	private String servletUrl = null;
	private String webHelperUrl = null;
	private com.google.gwt.event.shared.HandlerManager handlerManager = new com.google.gwt.event.shared.HandlerManager( null);
	
	
	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private static final DistropiaServiceAsync rpcService = GWT
			.create(DistropiaService.class);
	
	public void onModuleLoad() {
		instance = this;
		if (GWT.isProdMode()) servletUrl = GWT.getModuleBaseURL() + "/org.distropia.Distropia/DistropiaService";
		else servletUrl = GWT.getModuleBaseURL() + "DistropiaService";
		webHelperUrl = GWT.getHostPageBaseURL() + "WebHelper";
		
		setCurrentPage( new LoginPage());
	}

	public static void showInformationDialog( String text)
	{
		SC.say("Information", text, null);
	}
	
	public static void showErrorDialog( String text, String reason)
	{
		showErrorDialog( text + "<br><br>Grund:<br>" + reason);
	}
	
	public static void showErrorDialog( Throwable throwable)
	{
		showErrorDialog( "Die Aktion ist leider Fehlgeschlagen", throwable.toString());
	}
	
	public static void showErrorDialog( String text, Throwable throwable)
	{
		showErrorDialog( text, throwable.toString());
	}
	
	public static void showErrorDialog( String text)
	{
		Dialog dialog = new Dialog();
		dialog.setShowModalMask(true);
		SC.warn("Fehler", text, null, dialog);
	}
	
	public static Distropia getInstance() {
		return instance;
	}

	public static Page getCurrentPage() {
		return getInstance().currentPage;
	}

	public static void setCurrentPage(Page currentPage) {
		RootLayoutPanel rootPanel = RootLayoutPanel.get();
		if (getInstance().currentPage != null) 
		{
			getInstance().currentPage.destroy();
			rootPanel.remove( getInstance().currentPage);
		}
		getInstance().currentPage = currentPage;
		rootPanel.add( currentPage);
	}

	public static DistropiaServiceAsync getRpcService() {
		return rpcService;
	}
	
	public static boolean getDebug()
	{
		return debug;
	}

	public static Map<String, String> getClientDataStore() {
		return getInstance().clientDataStore;
	}

	public static String getSessionId() {
		return getInstance().sessionId;
	}

	public static void setSessionId(String sessionId) {
		getInstance().sessionId = sessionId;
	}
	
	int errorsInARow = 0;
	
	public static void manageSessionAndErrors( Throwable throwable){
		instance.errorsInARow++;
		if ((instance.errorsInARow>5)) Window.Location.reload();
		Distropia.showErrorDialog( throwable);
	}
	
	public static boolean manageSessionAndErrors( DefaultResponse defaultResponse)
	{
		if ((instance.errorsInARow>5) || (defaultResponse.getSessionId() == null)) Window.Location.reload();
		setSessionId( defaultResponse.getSessionId());
		if (defaultResponse.isSucceeded()){
			instance.errorsInARow = 0;
			return true;
		}
		instance.errorsInARow++;
		Distropia.showErrorDialog("Die Aktion ist leider Fehlgeschlagen", defaultResponse.getFailReason());
		return false;
	}

	public static String getLoggedInUniqueUserId() {
		return instance.loggedInUniqueUserId;
	}

	public static void setLoggedInUniqueUserId(String loggedInUniqueUserId) {
		instance.loggedInUniqueUserId = loggedInUniqueUserId;
	}
	
	public static String getServletUrl(){
		return instance.servletUrl;
	}
	
	public static String getWebHelperUrl(){
		return instance.webHelperUrl;
	}
	
	
	public static com.google.gwt.event.shared.HandlerManager getHandlerManager( ){
		return instance.handlerManager;
	}
}
