package org.distropia.client;

import java.io.Serializable;


public class UserCredentials implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7753112394362005593L;
	public enum Gender {
	    FEMALE, MALE, BOTH, UNKNOWN 
	}
	
	private String firstName;
	private String surName;
	private String title;
	private byte[] picture;
	private String street;
    private String city;
    private String postcode;
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof UserCredentials)) return false;
		return (Utils.equalsWithNull( firstName, ((UserCredentials)obj).firstName) &&
				Utils.equalsWithNull( surName, ((UserCredentials)obj).surName) &&
				Utils.equalsWithNull( title, ((UserCredentials)obj).title) &&
				(picture == ((UserCredentials)obj).picture) &&
				Utils.equalsWithNull( street, ((UserCredentials)obj).street) &&
				Utils.equalsWithNull( city, ((UserCredentials)obj).city) &&
				Utils.equalsWithNull( postcode, ((UserCredentials)obj).postcode));
	}
    
}
