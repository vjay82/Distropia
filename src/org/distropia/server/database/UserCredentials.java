package org.distropia.server.database;

import org.distropia.client.Gender;
import org.distropia.client.PublicUserCredentials;



public class UserCredentials {

	protected Gender gender = Gender.NOT_SPECIFIED;
	protected String firstName = null;
	protected String surName = null;
	protected String title = null;
	protected boolean namePublicVisible = true;
	
	protected byte[] picture = null;
	protected byte[] smallPicture = null;
	protected boolean picturePublicVisible = false;
	
	protected byte birthDay;
	protected byte birthMonth;
	protected int birthYear;
	protected boolean birthDayPublicVisible = false;
	protected boolean birthMonthPublicVisible = false;
	protected boolean birthYearPublicVisible = false;
	
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
    	
    	if (birthDayPublicVisible) u.setBirthDay(birthDay);
    	if (birthMonthPublicVisible) u.setBirthMonth(birthMonth);
    	if (birthYearPublicVisible) u.setBirthYear(birthYear);
    	
    	if (addressPublicVisible){
    		u.setStreet(street);
    		u.setCity(city);
    		u.setPostcode(postcode);
    	}
    	
    	return u;
    }
    
    


	public byte getBirthDay() {
		return birthDay;
	}




	public void setBirthDay(byte birthDay) {
		this.birthDay = birthDay;
	}




	public byte getBirthMonth() {
		return birthMonth;
	}




	public void setBirthMonth(byte birthMonth) {
		this.birthMonth = birthMonth;
	}




	public int getBirthYear() {
		return birthYear;
	}




	public void setBirthYear(int birthYear) {
		this.birthYear = birthYear;
	}




	public boolean isBirthDayPublicVisible() {
		return birthDayPublicVisible;
	}




	public void setBirthDayPublicVisible(boolean birthDayPublicVisible) {
		this.birthDayPublicVisible = birthDayPublicVisible;
	}




	public boolean isBirthMonthPublicVisible() {
		return birthMonthPublicVisible;
	}




	public void setBirthMonthPublicVisible(boolean birthMonthPublicVisible) {
		this.birthMonthPublicVisible = birthMonthPublicVisible;
	}




	public boolean isBirthYearPublicVisible() {
		return birthYearPublicVisible;
	}




	public void setBirthYearPublicVisible(boolean birthYearPublicVisible) {
		this.birthYearPublicVisible = birthYearPublicVisible;
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
