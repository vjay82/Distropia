package org.distropia.client;



@SuppressWarnings("serial")
public class ClientUserCredentialsResponse extends DefaultUserResponse {
	protected Gender gender;
	protected String firstName;
	protected String surName;
	protected String title;
	protected String street;
	protected String city;
	protected String postcode;
	
	protected byte birthDay;
	protected byte birthMonth;
	protected int birthYear;
	protected boolean birthDayPublicVisible = false;
	protected boolean birthMonthPublicVisible = false;
	protected boolean birthYearPublicVisible = false;
    
    protected boolean namePublicVisible = true;
	protected boolean picturePublicVisible = false;
	protected boolean addressPublicVisible = false;
	
	
	@Override
	public String toString() {
		if (Gender.ORGANIZATION.equals( gender)) return surName;
		
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
	public boolean isBirthDayPublicVisible() {
		return birthDayPublicVisible;
	}
	public void setBirthDayPublicVisible(boolean birthdayDayPublicVisible) {
		this.birthDayPublicVisible = birthdayDayPublicVisible;
	}
	public boolean isBirthMonthPublicVisible() {
		return birthMonthPublicVisible;
	}
	public void setBirthMonthPublicVisible(boolean birthdayMonthPublicVisible) {
		this.birthMonthPublicVisible = birthdayMonthPublicVisible;
	}
	public boolean isBirthYearPublicVisible() {
		return birthYearPublicVisible;
	}
	public void setBirthYearPublicVisible(boolean birthdayYearPublicVisible) {
		this.birthYearPublicVisible = birthdayYearPublicVisible;
	}
    
    
    
}
