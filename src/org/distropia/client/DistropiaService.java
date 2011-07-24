package org.distropia.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("DistropiaService")
public interface DistropiaService extends RemoteService {
	public DefaultResponse createUserAccount( CreateUserAccountRequest createAccountRequest) throws IllegalArgumentException;
	public LoginUserResponse loginUser( LoginUserRequest loginUserRequest) throws IllegalArgumentException;
	public DefaultUserResponse bootstrap( BootstrapRequest bootstrapRequest) throws IllegalArgumentException;
	public GetAdminSettingsResponse getAdminSettings( GetAdminSettingsRequest getAdminSettingsRequest) throws IllegalArgumentException;
	public DefaultUserResponse setAdminSettings( SetAdminSettingsRequest setAdminSettingsRequest) throws IllegalArgumentException;
	public ClientUserCredentialsResponse getUserCredentials( DefaultRequest defaultRequest) throws IllegalArgumentException;
}
