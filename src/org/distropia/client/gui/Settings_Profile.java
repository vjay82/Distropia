package org.distropia.client.gui;

import java.util.LinkedHashMap;

import org.distropia.client.ClientUserCredentialsRequest;
import org.distropia.client.ClientUserCredentialsResponse;
import org.distropia.client.DefaultRequest;
import org.distropia.client.DefaultUserResponse;
import org.distropia.client.Distropia;
import org.distropia.client.Gender;
import org.distropia.client.JavaScriptMethodCallback;
import org.distropia.client.JavaScriptMethodHelper;
import org.distropia.client.Utils;
import org.distropia.client.events.UserCredentialsChangedEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.BooleanItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

public class Settings_Profile extends VLayout {
	private DynamicForm userCredentials;
	private DynamicForm uploadForm;
	private ComboBoxItem titleItem;
	private TextItem firstNameItem;
	private boolean deletePicture = false;
	private Button btnDeleteUserPicture;
	private SpinnerItem birthMonth;
	private SpinnerItem birthYear;
	private SpinnerItem birthDay;
	private CheckboxItem checkboxBirthDayPublicVisible;
	private CheckboxItem checkboxBirthMonthPublicVisible;
	private CheckboxItem checkboxBirthYearPublicVisible;
	
	
	private void onGenderChanged(Object value) {
		if (value.equals( Gender.ORGANIZATION.toString())){
			titleItem.hide();
			firstNameItem.hide();
			birthMonth.hide();
			birthYear.hide();
			birthDay.hide();
			checkboxBirthDayPublicVisible.hide();
			checkboxBirthMonthPublicVisible.hide();
			checkboxBirthYearPublicVisible.hide();
		}
		else{
			titleItem.show();
			firstNameItem.show();
			birthMonth.show();
			birthYear.show();
			birthDay.show();
			checkboxBirthDayPublicVisible.show();
			checkboxBirthMonthPublicVisible.show();
			checkboxBirthYearPublicVisible.show();
		}
	}
	
	private void setDeleteUserPicture( boolean deletePicture){
		this.deletePicture = deletePicture;
		if (deletePicture){
			btnDeleteUserPicture.setTitle("Lieber doch nicht!");
			uploadForm.setValue("uploadItem", "");
		}
		else btnDeleteUserPicture.setTitle("Bild löschen");
	}
	
	public void saveSettings(){
		if (userCredentials.validate() && uploadForm.validate()){
			
			ClientUserCredentialsRequest c = new ClientUserCredentialsRequest( Distropia.getSessionId());
			
			c.setGender( Gender.valueOf( userCredentials.getValueAsString( "gender")));
			c.setTitle( userCredentials.getValueAsString( "title"));
			c.setFirstName( userCredentials.getValueAsString("firstName"));
			c.setSurName( userCredentials.getValueAsString("surName"));
			c.setNamePublicVisible( (Boolean) userCredentials.getValue("namePublicVisible"));
			c.setBirthDay( ((Integer) userCredentials.getValue("birthDay")).byteValue());
			c.setBirthMonth( ((Integer) userCredentials.getValue("birthMonth")).byteValue());
			c.setBirthYear( (Integer) userCredentials.getValue("birthYear"));
			c.setBirthDayPublicVisible((Boolean) userCredentials.getValue("birthDayPublicVisible"));
			c.setBirthMonthPublicVisible((Boolean) userCredentials.getValue("birthMonthPublicVisible"));
			c.setBirthYearPublicVisible((Boolean) userCredentials.getValue("birthYearPublicVisible"));
			c.setStreet( userCredentials.getValueAsString("street"));
			c.setAddressPublicVisible((Boolean) userCredentials.getValue("addressPublicVisible"));
			c.setPostcode( userCredentials.getValueAsString("postcode"));
			c.setCity( userCredentials.getValueAsString("city"));
			c.setPicturePublicVisible( (Boolean) uploadForm.getValue("picturePublicVisible"));
			
			if (!Utils.isNullOrEmpty( uploadForm.getValue("uploadItem"))) {
				
				if (Distropia.getDebug()){
					Scheduler.get().scheduleFixedPeriod( new Scheduler.RepeatingCommand() {
						@Override
						public boolean execute() {
							Distropia.getHandlerManager().fireEvent( new UserCredentialsChangedEvent());
							return false;
						}
					}, 1000);
				}else{
					String callbackName = JavaScriptMethodHelper.registerCallbackFunction(new JavaScriptMethodCallback() {
						public void execute(JavaScriptObject obj) {
							uploadFinished(obj);
						}
					});
					uploadForm.setValue("callbackName", callbackName); //set hidden item
				}
				uploadForm.submitForm();	
			}
			else{
				c.setDeletePicture( deletePicture);
				if (deletePicture){
					Scheduler.get().scheduleFixedPeriod( new Scheduler.RepeatingCommand() {
						@Override
						public boolean execute() {
							Distropia.getHandlerManager().fireEvent( new UserCredentialsChangedEvent());
							return false;
						}
					}, 1000);
				}
			}
			
			Distropia.getRpcService().setUserCredentials(c, new AsyncCallback<DefaultUserResponse>() {

				@Override
				public void onFailure(Throwable caught) {
					Distropia.manageSessionAndErrors( caught);
				}

				@Override
				public void onSuccess(DefaultUserResponse result) {
					if (Distropia.manageSessionAndErrors( result)){
						setDeleteUserPicture( false);
						Distropia.showInformationDialog("Speichern erfolgreich");
					}
					
				}
			});
		}
	}
	
	public Settings_Profile() {
		LinkedHashMap<String, String> genderMap = new LinkedHashMap<String, String>();
		genderMap.put(Gender.NOT_SPECIFIED.toString(), Gender.NOT_SPECIFIED.toDisplayString());
		genderMap.put(Gender.ORGANIZATION.toString(), Gender.ORGANIZATION.toDisplayString());
		genderMap.put(Gender.MALE.toString(), Gender.MALE.toDisplayString());
		genderMap.put(Gender.FEMALE.toString(), Gender.FEMALE.toDisplayString());
		genderMap.put(Gender.BOTH.toString(), Gender.BOTH.toDisplayString());

		uploadForm = new DynamicForm();
		uploadForm.setNumCols(8);
		uploadForm.setEncoding(Encoding.MULTIPART);
		uploadForm.setCanSubmit(true);
		uploadForm.setTarget("hidden_frame");
		uploadForm.setAction( Distropia.getWebHelperUrl() + "?upload=userPicture&sessionId=" + Distropia.getSessionId());
		
		UploadItem uploadItem = new UploadItem("uploadItem", "New UploadItem");
		
		uploadItem.setStartRow(false);
		uploadItem.setEndRow(false);
		uploadItem.setShowTitle(false);
		
		HiddenItem callbackItem = new HiddenItem("callbackName");
		CheckboxItem checkboxItem = new CheckboxItem("picturePublicVisible", "Dein Bild ist öffentlich sichtbar.");
		checkboxItem.setShowTitle(false);
		checkboxItem.setStartRow(true);
		uploadForm.setFields(new FormItem[] { callbackItem, uploadItem, new RowSpacerItem(), checkboxItem});
		
		UserAccountPicture userAccountPicture = new UserAccountPicture(null, true);
		userAccountPicture.setExtraSpace( 20);
		
		Label userCredentialsHeader = new Label("Deine persönlichen Daten");
		userCredentialsHeader.setWidth100();
		userCredentialsHeader.setAutoHeight();
		userCredentialsHeader.setExtraSpace( 20);
		userCredentialsHeader.setStyleName("distropia-settings-caption");
		
		
		VLayout userCredentialsLayout = new VLayout();
		userCredentialsLayout.addMember(userCredentialsHeader);
		
		userCredentials = new DynamicForm();
		userCredentials.setWrapItemTitles(false);
		userCredentials.setNumCols(4);
		userCredentials.setMargin(8);
		userCredentials.setWidth(500);
		
		firstNameItem = new TextItem("firstName", "Vorname");
		firstNameItem.setStartRow(true);
		firstNameItem.setTabIndex(1);
		BooleanItem booleanItem = new BooleanItem();
		booleanItem.setName("namePublicVisible");
		booleanItem.setTitle("Dein Name ist öffentlich sichtbar (Du bist suchbar)");
		TextItem textItem_1 = new TextItem("surName", "Nachname");
		textItem_1.setStartRow(true);
		birthDay = new SpinnerItem("birthDay", "Der Tag im Monat, an dem du Geburtstag hast");
		birthDay.setStartRow(true);
		birthMonth = new SpinnerItem("birthMonth", "Dein Geburtsmonat");
		birthMonth.setStartRow(true);
		birthYear = new SpinnerItem("birthYear", "Dein Geburtsjahr");
		birthYear.setStartRow(true);
		TextItem textItem_2 = new TextItem("street", "Straße");
		textItem_2.setStartRow(true);
		TextItem textItem_3 = new TextItem("city", "Ort");
		textItem_3.setStartRow(true);
		SelectItem selectItem = new SelectItem("gender", "Geschlecht / Profiltyp");
		selectItem.addChangedHandler(new ChangedHandler() {
			public void onChanged(ChangedEvent event) {
				onGenderChanged( event.getValue());
			}				
		});
		selectItem.setMultiple(false);
		selectItem.setValueMap( genderMap);
		titleItem = new ComboBoxItem("title", "Titel");
		titleItem.setStartRow(true);
		titleItem.setValueMap( "", "Dr.", "Dipl. Ing.");
		checkboxBirthDayPublicVisible = new CheckboxItem("birthDayPublicVisible", "Tag öffentlich sichtbar");
		checkboxBirthMonthPublicVisible = new CheckboxItem("birthMonthPublicVisible", "Monat öffentlich sichtbar");
		checkboxBirthYearPublicVisible = new CheckboxItem("birthYearPublicVisible", "Jahr öffentlich sichtbar");
		userCredentials.setFields(new FormItem[] { selectItem, titleItem, firstNameItem, textItem_1, booleanItem, birthDay, checkboxBirthDayPublicVisible, birthMonth, checkboxBirthMonthPublicVisible, birthYear, checkboxBirthYearPublicVisible, textItem_2, new CheckboxItem("addressPublicVisible", "Adresse ist öffentlich einsehbar"), new TextItem("postcode", "Postleitzahl"), textItem_3});
		userCredentialsLayout.addMember(userCredentials);
		this.addMember( userCredentialsLayout);
		
		
		Label userPictureHeader = new Label("Dein Benutzerbild");
		userPictureHeader.setWidth100();
		userPictureHeader.setAutoHeight();
		userPictureHeader.setExtraSpace( 20);
		userPictureHeader.setStyleName("distropia-settings-caption");
		
		
		VLayout userPictureLayout = new VLayout();
		userPictureLayout.addMember(userPictureHeader);
		HLayout userPictureLayout2 = new HLayout();
		userPictureLayout2.setMargin(8);
		userPictureLayout2.addMember( userAccountPicture);
		VLayout userPictureLayout3 = new VLayout();
		
		Label lblNewLabel = new Label("Um dein Bild zu verändern, verwende bitte die folgenden Buttons:");		
		lblNewLabel.setWidth100();
		lblNewLabel.setAutoHeight();
		userPictureLayout3.addMember(lblNewLabel);
		
		LayoutSpacer layoutSpacer_2 = new LayoutSpacer();
		layoutSpacer_2.setHeight(20);
		userPictureLayout3.addMember(layoutSpacer_2);
		
		btnDeleteUserPicture = new Button("Bild löschen");
		btnDeleteUserPicture.setAutoFit( true);
		btnDeleteUserPicture.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setDeleteUserPicture (!deletePicture);				
			}
		});
		userPictureLayout3.addMember(btnDeleteUserPicture);
		
		LayoutSpacer layoutSpacer = new LayoutSpacer();
		layoutSpacer.setHeight(20);
		userPictureLayout3.addMember(layoutSpacer);
		userPictureLayout3.addMember( uploadForm);
		
		LayoutSpacer layoutSpacer_1 = new LayoutSpacer();
		userPictureLayout3.addMember(layoutSpacer_1);
		userPictureLayout2.addMember( userPictureLayout3);
		userPictureLayout.addMember( userPictureLayout2);
		this.addMember( userPictureLayout);
	
		
		this.setPadding( 8);
		
		Button btnSave = new Button("Einstellungen speichern");
		btnSave.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				saveSettings();
			}
		});
		btnSave.setPadding(20);
		
		LayoutSpacer layoutSpacer_4 = new LayoutSpacer();
		layoutSpacer_4.setHeight(20);
		addMember(layoutSpacer_4);
		btnSave.setWidth(150);
		btnSave.setHeight(50);
		VLayout vLayout = new VLayout();
		vLayout.setDefaultLayoutAlign( Alignment.CENTER);
		vLayout.addMember(btnSave);
		vLayout.setWidth100();
		addMember( vLayout);
		
		LayoutSpacer layoutSpacer_3 = new LayoutSpacer();
		layoutSpacer_3.setHeight(30);
		addMember(layoutSpacer_3);
		
		Distropia.getRpcService().getUserCredentials(new DefaultRequest( Distropia.getSessionId()), new AsyncCallback<ClientUserCredentialsResponse>() {
			
			@Override
			public void onSuccess(ClientUserCredentialsResponse result) {
				if (Distropia.manageSessionAndErrors(result)){
					userCredentials.setValue("gender", result.getGender().toString());
					userCredentials.setValue("title", result.getTitle());
					userCredentials.setValue("firstName", result.getFirstName());
					userCredentials.setValue("surName", result.getSurName());
					userCredentials.setValue("namePublicVisible", result.isNamePublicVisible());
					userCredentials.setValue("birthDay", result.getBirthDay());
					userCredentials.setValue("birthMonth", result.getBirthMonth());
					userCredentials.setValue("birthYear", result.getBirthYear());
					userCredentials.setValue("birthDayPublicVisible", result.isBirthDayPublicVisible());
					userCredentials.setValue("birthMonthPublicVisible", result.isBirthMonthPublicVisible());
					userCredentials.setValue("birthYearPublicVisible", result.isBirthYearPublicVisible());
					userCredentials.setValue("street", result.getStreet());
					userCredentials.setValue("city", result.getCity());
					userCredentials.setValue("postcode", result.getPostcode());
					userCredentials.setValue("addressPublicVisible", result.isAddressPublicVisible());
					uploadForm.setValue("picturePublicVisible", result.isPicturePublicVisible());
					
					onGenderChanged( result.getGender().toString());
				}
				
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Distropia.manageSessionAndErrors(caught);				
			}
		});
	}
	
	private void uploadFinished(JavaScriptObject obj) {
		if (!"success".equals( obj.toString())) Distropia.showErrorDialog( obj.toString());
		else{
			Distropia.getHandlerManager().fireEvent( new UserCredentialsChangedEvent());
		}
		
	}

	public DynamicForm getUserCredentials() {
		return userCredentials;
	}
	public SpinnerItem getBirthDay() {
		return birthDay;
	}
	public SpinnerItem getBirthMonth() {
		return birthMonth;
	}
	public SpinnerItem getBirthYear() {
		return birthYear;
	}
}
					