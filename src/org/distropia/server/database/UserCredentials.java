package org.distropia.server.database;


public class UserCredentials {
	
	public static enum Gender {
	    ORGANIZATION, FEMALE, MALE, BOTH, UNKNOWN 
	}
	
	protected Gender gender = Gender.UNKNOWN;
	protected String firstName = null;
	protected String surName = null;
	protected String title = null;
	protected boolean namePublicVisible = true;
	
	protected byte[] picture = null;
	protected byte[] smallPicture = null;
	protected boolean picturePublicVisible = false;
	
	protected String street = null;
    protected String city = null;
    protected String postcode = null;
    protected boolean addressPublicVisible = false;
    
    public PublicUserCredentials toPublicUserCredentials(){
    	PublicUserCredentials u = new PublicUserCredentials();
    	
    	if (namePublicVisible){
    		u.setGender(gender);
    		u.setFirstName(firstName);
    		u.setSurName(surName);
    	}
    	
    	if (picturePublicVisible){
    		u.setPicture(picture);
    		u.setSmallPicture( smallPicture);
    	}
    	
    	if (addressPublicVisible){
    		u.setStreet(street);
    		u.setCity(city);
    		u.setPostcode(postcode);
    	}
    	
    	return u;
    }
    
    
	public byte[] getSmallPicture() {
		return smallPicture;
	}


	public void setSmallPicture(byte[] smallPicture) {
		this.smallPicture = smallPicture;
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
	public boolean isNamePublicVisible() {
		return namePublicVisible;
	}
	public void setNamePublicVisible(boolean namePublicVisible) {
		this.namePublicVisible = namePublicVisible;
	}
	public byte[] getPicture() {
		return picture;
	}
	public void setPicture(byte[] picture) {
		this.picture = picture;
	}
	public boolean isPicturePublicVisible() {
		return picturePublicVisible;
	}
	public void setPicturePublicVisible(boolean picturePublicVisible) {
		this.picturePublicVisible = picturePublicVisible;
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
	public boolean isAddressPublicVisible() {
		return addressPublicVisible;
	}
	public void setAddressPublicVisible(boolean addressPublicVisible) {
		this.addressPublicVisible = addressPublicVisible;
	}
    
    
}
