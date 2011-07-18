package org.distropia.client;


public class LoginUserRequest extends DefaultRequest{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3038925842811424335L;
	protected String userName;
	protected String password;
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public LoginUserRequest() {
		super();
	}
	public LoginUserRequest(String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
	}
	
	
}