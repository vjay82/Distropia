package org.distropia.client;



@SuppressWarnings("serial")
public class ClientUserCredentialsResponse extends DefaultUserResponse {

	public enum Gender {
	    FEMALE, MALE, BOTH, UNKNOWN 
	}
	
	protected Gender gender;
	protected String firstName;
	protected String surName;
	protected String title;
	protected String street;
	protected String city;
	protected String postcode;
    
    protected boolean namePublicVisible = true;
	protected boolean picturePublicVisible = false;
	protected boolean addressPublicVisible = false;
	
	
	@Override
	public String toString() {
		String result;
		
		if (!Utils.isNullOrEmpty( firstName)) result = firstName + " " + surName;
		else result = surName;
		
		if (!Utils.isNullOrEmpty( title)) result = title + " " + result;
		
		return result;
	}
	public Gender getGender() {
		return gender;
	}
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getSurName() {
		return surName;
	}
	public void setSurName(String surName) {
		this.surName = surName;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getPostcode() {
		return postcode;
	}
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
	public boolean isNamePublicVisible() {
		return namePublicVisible;
	}
	public void setNamePublicVisible(boolean namePublicVisible) {
		this.namePublicVisible = namePublicVisible;
	}
	public boolean isPicturePublicVisible() {
		return picturePublicVisible;
	}
	public void setPicturePublicVisible(boolean picturePublicVisible) {
		this.picturePublicVisible = picturePublicVisible;
	}
	public boolean isAddressPublicVisible() {
		return addressPublicVisible;
	}
	public void setAddressPublicVisible(boolean addressPublicVisible) {
		this.addressPublicVisible = addressPublicVisible;
	}
    
    
    
}
