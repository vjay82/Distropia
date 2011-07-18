package org.distropia.client;


public class CreateUserAccountRequest extends DefaultRequest {
	/**
	 * 
	 */
	private static final long serialVersionUID = -820675521018367178L;
	protected String userName;
	protected String password;
	protected String firstName;
	protected String surName;
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
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return surName;
	}
	public void setLastName(String lastName) {
		this.surName = lastName;
	}
	
	public CreateUserAccountRequest(String sessionId, String userName,
			String password, String firstName, String surName) {
		super(sessionId);
		this.userName = userName;
		this.password = password;
		this.firstName = firstName;
		this.surName = surName;
	}
	public CreateUserAccountRequest() {
		super();
	}
	
	
}
