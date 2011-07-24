package org.distropia.server.database;

import java.io.Serializable;
import java.util.Arrays;

import org.distropia.client.ClientUserCredentialsResponse;
import org.distropia.client.Utils;
import org.distropia.server.database.UserCredentials.Gender;

public class PublicUserCredentials implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -984586017912861531L;
	protected Gender gender = Gender.UNKNOWN;
	protected String firstName = null;
	protected String surName = null;
	protected String title = null;
	
	protected byte[] picture = null;
	protected byte[] smallPicture = null;
	
	protected String street = null;
    protected String city = null;
    protected String postcode = null;
    
    protected String uniqueUserId = null;
    protected byte[] publicKey = null;
    
    @Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ClientUserCredentialsResponse)) return false;
		return (Utils.equalsWithNull( firstName, ((PublicUserCredentials)obj).firstName) &&
				Utils.equalsWithNull( surName, ((PublicUserCredentials)obj).surName) &&
				Utils.equalsWithNull( title, ((PublicUserCredentials)obj).title) &&
				Arrays.equals(picture, ((PublicUserCredentials)obj).picture) &&
				Arrays.equals(smallPicture, ((PublicUserCredentials)obj).smallPicture) &&
				Arrays.equals(publicKey, ((PublicUserCredentials)obj).publicKey) &&
				Utils.equalsWithNull( street, ((PublicUserCredentials)obj).street) &&
				Utils.equalsWithNull( city, ((PublicUserCredentials)obj).city) &&
				Utils.equalsWithNull( postcode, ((PublicUserCredentials)obj).postcode) &&
				Utils.equalsWithNull( uniqueUserId, ((PublicUserCredentials)obj).uniqueUserId) 
				);
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
