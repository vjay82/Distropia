package org.distropia.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.io.IOUtils;
import org.distropia.client.BootstrapRequest;
import org.distropia.client.ClientUserCredentialsRequest;
import org.distropia.client.ClientUserCredentialsResponse;
import org.distropia.client.CreateUserAccountRequest;
import org.distropia.client.DefaultRequest;
import org.distropia.client.DefaultResponse;
import org.distropia.client.DefaultUserResponse;
import org.distropia.client.DistropiaService;
import org.distropia.client.Gender;
import org.distropia.client.GetAdminSettingsRequest;
import org.distropia.client.GetAdminSettingsResponse;
import org.distropia.client.LoginUserRequest;
import org.distropia.client.LoginUserResponse;
import org.distropia.client.PublicUserCredentials;
import org.distropia.client.SearchRequest;
import org.distropia.client.SearchResponse;
import org.distropia.client.SetAdminSettingsRequest;
import org.distropia.client.Utils;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;
import org.distropia.server.communication.PingResponse;
import org.distropia.server.database.UserCredentials;
import org.distropia.server.database.UserProfile;
import org.distropia.server.database.UserProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.rpc.client.impl.RemoteException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class DistropiaServiceImpl extends RemoteServiceServlet implements
		DistropiaService, Maintenanceable {
	static transient Logger logger = LoggerFactory.getLogger(DistropiaServiceImpl.class);
	protected List<FileToDelete> filesToDelete = new ArrayList<FileToDelete>();
	protected File tmpDirectory = null;
	
	private File getTmpDirectory(){
		if (tmpDirectory == null){
			tmpDirectory = new File( Backend.getWorkDir() + "tmpfiles");    	
	    	if (!tmpDirectory.exists() && !tmpDirectory.mkdirs()) logger.error("Could not create tmp directory " + tmpDirectory);
	    	
	    	// clear old tmp files
	    	for (File file: Arrays.asList( tmpDirectory.listFiles())){
	    		file.delete();
	    	}
		}
		return tmpDirectory;
	}
	
	public void init(ServletConfig config) throws ServletException {
    	super.init(config);
    	logger.info("loaded DistributedBookServiceImpl");        
	}
	
	 /**
     * @see HttpServlet#HttpServlet()
     */
    public DistropiaServiceImpl() {
        super();
    }
    
	@Override
	public DefaultResponse createUserAccount(
			CreateUserAccountRequest createAccountRequest)
			throws RemoteException {
		
		Session session = getSessionCache().getSessionForRequest( createAccountRequest);
		UserProfiles userProfiles = Backend.getInstance().getUserProfiles();
		if (userProfiles == null) throw new RemoteException("UserProfiles are null", null);
		
		synchronized (userProfiles) {
			UserProfile userProfile;
			try{
				logger.info("creating user " + createAccountRequest.getFirstName() + " " + createAccountRequest.getSurName());
				
				userProfile = userProfiles.login( createAccountRequest.getUserName(), createAccountRequest.getPassword());
				if (userProfile != null) return new DefaultResponse( session.getSessionId(), false, "User " + createAccountRequest.getUserName() + " already exists.");				
			
				userProfile = (UserProfile) userProfiles.createNewUser();
				if (userProfile == null) throw new Exception( "Error userProfile is null.");
				userProfile.initializeUser( createAccountRequest.getPassword());
				userProfile.setUserName( createAccountRequest.getUserName());
				
				UserCredentials userCredentials = new UserCredentials();
				userCredentials.setSurName( createAccountRequest.getSurName());
				userCredentials.setFirstName( createAccountRequest.getFirstName());
				userProfile.setUserCredentials(userCredentials);
				userProfiles.add( userProfile);
				
				// immediate push
				Backend.getConnectionStatus().setNextUserPushToImmediate();
				
				return new DefaultResponse( session.getSessionId(),true, null);
			}
			catch (Exception e) {
				e.printStackTrace();
				logger.error("Error creating user " + createAccountRequest.getUserName());
			}
		}
		return new DefaultResponse( session.getSessionId(), false, "Error, see server logfiles.");
					
	}

	@Override
	public LoginUserResponse loginUser(LoginUserRequest loginUserRequest)
			throws IllegalArgumentException {
		
		Session session = getSessionCache().getSessionForRequest( loginUserRequest);
		
		// we look if a user could log in with this credentials
		UserProfiles userProfiles = Backend.getInstance().getUserProfiles();
		if (userProfiles == null) throw new RemoteException("UserProfiles are null", null);
		
		synchronized (userProfiles) {
			UserProfile userProfile;
			try {
				userProfile = userProfiles.login( loginUserRequest.getUserName(), loginUserRequest.getPassword());
				if (userProfile != null){
					session.setUserProfile(userProfile);
					return new LoginUserResponse( session.getSessionId(), true, null, session.userProfile.getUniqueUserID(), false, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Error log in user " + loginUserRequest.getUserName());
				return new LoginUserResponse( session.getSessionId(), false, "Exception, please take a look at the server logfiles.", null, false, false);
			}		 
		}
		
		// we look if the adminUser could be logged in
		if (Backend.getConfiguration().isAdminAccountEnabled()){
			logger.info("login, checking for admin user");
			if ( Utils.equalsWithNull(Backend.getConfiguration().getAdminAccountUserName(), loginUserRequest.getUserName()) &&
				 Utils.equalsWithNull(Backend.getConfiguration().getAdminAccountPassword(), loginUserRequest.getPassword())){
				session.setAdmin( true);
				return new LoginUserResponse( session.getSessionId(), true, null, null, true, false);
			}
		}
		return new LoginUserResponse( session.getSessionId(), true, null, null, false, true);
	}

	@Override
	public DefaultUserResponse bootstrap(BootstrapRequest bootstrapRequest)
			throws IllegalArgumentException {
		Session session = getSessionCache().getSessionForRequest( bootstrapRequest);
		
		if (session.isAdmin())
		{
			try {
				logger.info("trying to bootstrap from " + bootstrapRequest.getAddress());
				KnownHost knownHost = new KnownHost( Backend.getMyKnownHosts());
				knownHost.getAddresses().add( bootstrapRequest.getAddress());
				PingResponse pingResponse = knownHost.ping();
				knownHost.setUniqueHostId( pingResponse.getPingResponseFromUniqueHostID());
				
				KnownHosts knownHosts = Backend.getMyKnownHosts();
				synchronized ( knownHosts) {
					knownHosts.add( knownHost);
					/*for( int index = 1; index < requestUniqueHostIdResponse.getUniqueHostIds().size(); index++){
						String uniqueHostId = requestUniqueHostIdResponse.getUniqueHostIds().get(index);
						KnownHost proxiedHost = new KnownHost( Backend.getMyKnownHosts(), uniqueHostId);
						proxiedHost.addAddress("uid://" + knownHost.getUniqueHostId());
						knownHosts.add( proxiedHost);
					}*/
				}
				
				return new DefaultUserResponse( session.getSessionId(), true, null, null, true);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error bootstrapping", e);
				return new DefaultUserResponse( session.getSessionId(), false, e.toString(), null, true);
			}
		}
		return new DefaultUserResponse( session.getSessionId(), true, null, null, false);
	}

	public SessionCache getSessionCache(){
		return Backend.getSessionCache();
	}

	@Override
	public GetAdminSettingsResponse getAdminSettings(
			GetAdminSettingsRequest getAdminSettingsRequest)
			throws IllegalArgumentException {
		Session session = getSessionCache().getSessionForRequest( getAdminSettingsRequest);
		
		if (session.isAdmin()){
			GetAdminSettingsResponse response = new GetAdminSettingsResponse( session.getSessionId(), true, null, null, true);
			
			response.setWebserverPort( Backend.getConnectionStatus().getInternetPort( true));
			response.setDhtPort( Backend.getDHT().getPort( true));
			response.setExternalWebserverPort( Backend.getConnectionStatus().getExternalInternetPort());
			response.setExternalDHTPort( Backend.getDHT().getExternalPort());
		
			response.setConnectedToInternet( Backend.getConnectionStatus().isConnectedToInternet());
			response.setInternetAddress( Backend.getConnectionStatus().getInternetAddress());
			response.setReachable( Backend.getConnectionStatus().isReachable());
			response.setProtocolVersion( Backend.PROTOCOL_VERSION);
			response.setConnectedToNodes( Backend.getDHT().getContacts());
			
			if (!getAdminSettingsRequest.isStatisticsOnly()){				
				response.setAutomaticUpdates( Backend.getConfiguration().isUpdatesEnabled());
				response.setAutomaticUpdatesFromBranch( Backend.getConfiguration().getUpdatesBranch());
				response.setOnlyReachableByLocalhost( Backend.getConfiguration().isOnlyReachableByLocalhost());
			}
			
			return response;
		}
		
		return new GetAdminSettingsResponse( session.getSessionId(), true, null, null, false);
	}

	@Override
	public DefaultUserResponse setAdminSettings(
			SetAdminSettingsRequest setAdminSettingsRequest)
			throws IllegalArgumentException {
		Session session = getSessionCache().getSessionForRequest( setAdminSettingsRequest);
		
		if (session.isAdmin()){
			Backend.getConfiguration().setUpdatesEnabled( setAdminSettingsRequest.isAutomaticUpdates());
			Backend.getConfiguration().setOnlyReachableByLocalhost( setAdminSettingsRequest.isOnlyReachableByLocalhost());
			Backend.getConfiguration().setUpdatesBranch( setAdminSettingsRequest.getAutomaticUpdatesFromBranch());
			return new DefaultUserResponse( session.getSessionId(), true, null, null, true);
		}
		
		return new DefaultUserResponse( session.getSessionId(), true, null, null, false);
	}

	@Override
	public ClientUserCredentialsResponse getUserCredentials(
			DefaultRequest defaultRequest) throws IllegalArgumentException {
		ClientUserCredentialsResponse response = new ClientUserCredentialsResponse();
		Session session = getSessionCache().getSessionForRequest( defaultRequest, false, response);
		
		if (session.getUserProfile() != null){
			try {
				UserCredentials uc = session.getUserProfile().getUserCredentials();
				
				response.setGender( uc.getGender());
				response.setFirstName(uc.getFirstName());
				response.setSurName(uc.getSurName());
				response.setTitle(uc.getTitle());
				response.setNamePublicVisible( uc.isNamePublicVisible());
				response.setPicturePublicVisible(uc.isPicturePublicVisible());
				response.setStreet( uc.getStreet());
				response.setCity( uc.getCity());
				response.setPostcode(uc.getPostcode());
				response.setAddressPublicVisible(uc.isAddressPublicVisible());
				response.setBirthDay( uc.getBirthDay());
				response.setBirthMonth( uc.getBirthMonth());
				response.setBirthYear( uc.getBirthYear());
				response.setBirthDayPublicVisible( uc.isBirthDayPublicVisible());
				response.setBirthMonthPublicVisible( uc.isBirthMonthPublicVisible());
				response.setBirthYearPublicVisible( uc.isBirthYearPublicVisible());
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error getting user credentials", e);
				response.setSucceeded( false);
				response.setFailReason("Exception while getting your credentials, please see the server logfiles");
			}
		}
		return response;
	}

	@Override
	public DefaultUserResponse setUserCredentials(
			ClientUserCredentialsRequest clientUserCredentialsRequest)
			throws IllegalArgumentException {
		ClientUserCredentialsResponse response = new ClientUserCredentialsResponse();
		Session session = getSessionCache().getSessionForRequest( clientUserCredentialsRequest, false, response);
		
		if (session.getUserProfile() != null){
			try {
				UserCredentials uc = session.getUserProfile().getUserCredentials();
				
				uc.setGender( clientUserCredentialsRequest.getGender());
				if (uc.getGender().equals( Gender.ORGANIZATION)){
					uc.setTitle( "");
					uc.setFirstName( "");
				}
				else{
					uc.setTitle( clientUserCredentialsRequest.getTitle());
					uc.setFirstName( clientUserCredentialsRequest.getFirstName());
				}
				uc.setSurName( clientUserCredentialsRequest.getSurName());
				uc.setNamePublicVisible( clientUserCredentialsRequest.isNamePublicVisible());
				uc.setPicturePublicVisible( clientUserCredentialsRequest.isPicturePublicVisible());
				uc.setStreet( clientUserCredentialsRequest.getStreet());
				uc.setPostcode( clientUserCredentialsRequest.getPostcode());
				uc.setCity( clientUserCredentialsRequest.getCity());
				uc.setAddressPublicVisible(clientUserCredentialsRequest.isAddressPublicVisible());
				uc.setBirthDay( clientUserCredentialsRequest.getBirthDay());
				uc.setBirthMonth( clientUserCredentialsRequest.getBirthMonth());
				uc.setBirthYear( clientUserCredentialsRequest.getBirthYear());
				uc.setBirthDayPublicVisible( clientUserCredentialsRequest.isBirthDayPublicVisible());
				uc.setBirthMonthPublicVisible( clientUserCredentialsRequest.isBirthMonthPublicVisible());
				uc.setBirthYearPublicVisible( clientUserCredentialsRequest.isBirthYearPublicVisible());
				
				if (clientUserCredentialsRequest.isDeletePicture()){
					uc.setPicture( null);
					uc.setSmallPicture( null);
				}
				
				session.getUserProfile().setUserCredentials( uc);
				
				Backend.getDHT().pushUsers();
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error getting user credentials", e);
				response.setSucceeded( false);
				response.setFailReason("Exception while getting your credentials, please see the server logfiles");
			}
		}
		return response;
	}

	protected File writeTmpPicture( byte[] picture, int maxWidth, int maxHeight) throws Exception{
		File tmpFile = File.createTempFile("userPicture_", ".png", getTmpDirectory());
		
		if (maxWidth == 0 && maxHeight == 0){
			FileOutputStream fos = new FileOutputStream( tmpFile);
			fos.write( picture);
			fos.close();
		}
		else{
			FileOutputStream fos = new FileOutputStream( tmpFile);
			try{
				WebHelper.scalePictureToMax(new ByteArrayInputStream( picture), fos, maxWidth, maxHeight);
			}
			finally{
				fos.close();
			}			
		}
		return tmpFile;
	}
	
	@Override
	public SearchResponse searchUser(SearchRequest searchRequest)
			throws IllegalArgumentException {
		
		SearchResponse response = new SearchResponse();
		Session session = getSessionCache().getSessionForRequest( searchRequest, false, response);
		
		if (session.getUserProfile() != null){
			try {
				List<PublicUserCredentials> result = Backend.getDHT().searchUser( searchRequest.getSearchForName());
				
				for(PublicUserCredentials puc: result){
					
					if (puc.getPicture() == null){
						FileInputStream fi = new FileInputStream( Backend.getWebContentFolder().getAbsolutePath() + File.separator + "images" + File.separator + "replacement_user_image.png");
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						IOUtils.copy(fi, out);
						fi.close();
						puc.setPicture( out.toByteArray());
					}
					
					if (puc.getPicture() != null){
						File tmpFile = writeTmpPicture( puc.getPicture(), 186, 120);
						synchronized (filesToDelete) {
							filesToDelete.add( new FileToDelete( System.currentTimeMillis() + 600000, tmpFile));
						}
						Backend.getMaintenanceList().addWithWeakReference( this, 60000);
						
						puc.setPicture( tmpFile.getName().getBytes("UTF-8"));
					}
					puc.setSmallPicture( null);
				}
				response.setUsers( result);
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error getting user credentials", e);
				response.setSucceeded( false);
				response.setFailReason("Exception while getting your credentials, please see the server logfiles");
			}
		}
		
		
		
		return response;
	}

	@Override
	public void maintenance() {
		long currentMillis = System.currentTimeMillis();
		synchronized ( filesToDelete) {
			for(int index = filesToDelete.size()-1; index>=0 ; index--){
				FileToDelete fileToDelete = filesToDelete.get(index);
				if (fileToDelete.getDeleteAt() < currentMillis){
					if (fileToDelete.getFile().delete()) filesToDelete.remove(index);
				}
			}
		}
	}
}
