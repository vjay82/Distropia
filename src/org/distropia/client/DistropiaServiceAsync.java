package org.distropia.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The client side stub for the RPC service.
 */
public interface DistropiaServiceAsync {
	public void createUserAccount( CreateUserAccountRequest createAccountRequest, AsyncCallback<DefaultResponse> callback);
	public void loginUser( LoginUserRequest loginUserRequest, AsyncCallback<LoginUserResponse> callback);
	public void bootstrap( BootstrapRequest bootstrapRequest, AsyncCallback<DefaultUserResponse> callback) throws IllegalArgumentException;
	public void getAdminSettings( GetAdminSettingsRequest getAdminSettingsRequest, AsyncCallback<GetAdminSettingsResponse> callback) throws IllegalArgumentException;
	public void setAdminSettings( SetAdminSettingsRequest setAdminSettingsRequest, AsyncCallback<DefaultUserResponse> callback) throws IllegalArgumentException;
	public void getUserCredentials( DefaultRequest defaultRequest, AsyncCallback<ClientUserCredentialsResponse> callback) throws IllegalArgumentException;
	public void setUserCredentials( ClientUserCredentialsRequest clientUserCredentialsRequest, AsyncCallback<DefaultUserResponse> callback) throws IllegalArgumentException;
	public void searchUser( SearchRequest searchRequest, AsyncCallback<SearchResponse> callback) throws IllegalArgumentException;
}
