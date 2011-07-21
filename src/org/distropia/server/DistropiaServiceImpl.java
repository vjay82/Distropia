package org.distropia.server;

import javax.servlet.http.HttpServlet;

import org.distropia.client.BootstrapRequest;
import org.distropia.client.CreateUserAccountRequest;
import org.distropia.client.DefaultResponse;
import org.distropia.client.DefaultUserResponse;
import org.distropia.client.DistropiaService;
import org.distropia.client.GetAdminSettingsRequest;
import org.distropia.client.GetAdminSettingsResponse;
import org.distropia.client.LoginUserRequest;
import org.distropia.client.LoginUserResponse;
import org.distropia.client.SetAdminSettingsRequest;
import org.distropia.client.Utils;
import org.distropia.server.communication.KnownHost;
import org.distropia.server.communication.KnownHosts;
import org.distropia.server.communication.PingResponse;
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
		DistropiaService {
	static transient Logger logger = LoggerFactory.getLogger(DistropiaServiceImpl.class);
	protected SessionCache sessionCache = null;

	 /**
     * @see HttpServlet#HttpServlet()
     */
    public DistropiaServiceImpl() {
        super();
        logger.info("loaded DistributedBookServiceImpl");
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
				userProfile = userProfiles.login( createAccountRequest.getUserName(), createAccountRequest.getPassword());
				if (userProfile != null) return new DefaultResponse( session.getSessionId(), false, "User " + createAccountRequest.getUserName() + " already exists.");				
			
				userProfile = (UserProfile) userProfiles.createNewUser();
				if (userProfile == null) throw new Exception( "Error userProfile is null.");
				userProfile.initializeUser( createAccountRequest.getPassword());
				userProfile.setUserName( createAccountRequest.getUserName());
				userProfiles.add( userProfile);
				
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
		Session session = sessionCache.getSessionForRequest( bootstrapRequest);
		
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
		if (sessionCache == null) sessionCache = new SessionCache();
		return sessionCache;
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
}
