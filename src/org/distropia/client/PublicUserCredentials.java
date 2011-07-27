package org.distropia.client;

import java.io.Serializable;
import java.util.Arrays;


public class PublicUserCredentials implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -984586017912861531L;
	protected Gender gender = Gender.NOT_SPECIFIED;
	protected String firstName = null;
	protected String surName = null;
	protected String title = null;
	
	protected byte[] picture = null;
	protected byte[] smallPicture = null;
	
	protected byte birthDay = 0;
	protected byte birthMonth = 0;
	protected int birthYear = 0;
	
	protected String street = null;
    protected String city = null;
    protected String postcode = null;
    
    protected String uniqueUserId = null;
    protected byte[] publicKey = null;
    
    @Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof PublicUserCredentials)) return false;
		
		return (/*Utils.equalsWithNull( firstName, ((PublicUserCredentials)obj).firstName) &&
				Utils.equalsWithNull( surName, ((PublicUserCredentials)obj).surName) &&
				Utils.equalsWithNull( title, ((PublicUserCredentials)obj).title) &&
				Arrays.equals(picture, ((PublicUserCredentials)obj).picture) &&
				Arrays.equals(smallPicture, ((PublicUserCredentials)obj).smallPicture) &&
				Arrays.equals(publicKey, ((PublicUserCredentials)obj).publicKey) &&
				(birthDay == ((PublicUserCredentials)obj).birthDay) &&
				(birthMonth == ((PublicUserCredentials)obj).birthMonth) &&
				(birthYear == ((PublicUserCredentials)obj).birthYear) &&
				Utils.equalsWithNull( street, ((PublicUserCredentials)obj).street) &&
				Utils.equalsWithNull( city, ((PublicUserCredentials)obj).city) &&
				Utils.equalsWithNull( postcode, ((PublicUserCredentials)obj).postcode) &&*/
				Utils.equalsWithNull( uniqueUserId, ((PublicUserCredentials)obj).uniqueUserId) &&
				Arrays.equals(publicKey, ((PublicUserCredentials)obj).publicKey) 
				);
	}
    
    public boolean isOneBirthFieldSet(){
    	if (gender.equals( Gender.ORGANIZATION)) return false;
    	return (getBirthDay()+getBirthMonth()+getBirthYear() > 0);
    }
    
    public String toBirthdayString(){
    	String day, month, year;
    	if (getBirthDay() > 0) day = String.valueOf( getBirthDay());
    	else day = "??";
    	if (getBirthMonth() > 0) month = String.valueOf( getBirthMonth());
    	else month = "??";
    	if (getBirthYear() > 0) year = String.valueOf( getBirthYear());
    	else year = "????";
    	return day+"."+month+"."+year;
    }
    
    public String toAddressBlock(){
    	String result;
    	if (!Utils.isNullOrEmpty( getStreet())) result = getStreet();
    	else result ="";
    	
    	if (!Utils.isNullOrEmpty( getPostcode())) result = result + "<br>" + getPostcode()+ " " + getCity();
    	else if (!Utils.isNullOrEmpty( getCity())) result = result + "<br>" + getCity();
    	
    	return result;
    	
    }
    
    @Override
	public String toString() {
		if (Gender.ORGANIZATION.equals( gender)) return surName;
		
		String result;
		if (!Utils.isNullOrEmpty( firstName)) result = firstName + " " + surName;
		else result = surName;
		
		if (!Utils.isNullOrEmpty( title)) result = title + " " + result;
		
		return result;
	}

    
    public byte getBirthDay() {
		return birthDay;
	}


	public void setBirthDay(byte birthday_Day) {
		this.birthDay = birthday_Day;
	}


	public byte getBirthMonth() {
		return birthMonth;
	}


	public void setBirthMonth(byte birthday_Month) {
		this.birthMonth = birthday_Month;
	}


	public int getBirthYear() {
		return birthYear;
	}


	public void setBirthYear(int birthday_Year) {
		this.birthYear = birthday_Year;
	}


	public byte[] getSmallPicture() {
		return smallPicture;
	}


	public void setSmallPicture(byte[] smallPicture) {
		this.smallPicture = smallPicture;
	}


	public String getUniqueUserId() {
		return uniqueUserId;
	}

	public void setUniqueUserId(String uniqueUserId) {
		this.uniqueUserId = uniqueUserId;
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

	public byte[] getPicture() {
		return picture;
	}

	public void setPicture(byte[] picture) {
		this.picture = picture;
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

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
    
    
}
