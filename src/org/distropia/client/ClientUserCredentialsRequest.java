package org.distropia.client;


@SuppressWarnings("serial")
public class ClientUserCredentialsRequest extends DefaultRequest {

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
	protected boolean deletePicture = false;
	public ClientUserCredentialsRequest(String sessionId) {
		super( sessionId);
	}
	
	public ClientUserCredentialsRequest() {
		super();
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
	public boolean isDeletePicture() {
		return deletePicture;
	}
	public void setDeletePicture(boolean deletePicture) {
		this.deletePicture = deletePicture;
	}
	
}
