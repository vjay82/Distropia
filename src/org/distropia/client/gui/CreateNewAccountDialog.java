package org.distropia.client.gui;

import org.distropia.client.CreateUserAccountRequest;
import org.distropia.client.DefaultResponse;
import org.distropia.client.Distropia;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;

public class CreateNewAccountDialog extends Window {
	private DynamicForm dynamicForm;
	private PasswordItem password2;
	private PasswordItem password1;
	
	public CreateNewAccountDialog() {
		setAutoSize(true);
		
		setShowFooter(false);
		setShowMinimizeButton(false);
		setShowModalMask(true);
		setAutoCenter(true);
		setIsModal(true);
		setTitle("Neues Profil anlegen");
		
		
		CustomValidator secondPasswordValidator = new CustomValidator() {
			
			@Override
			protected boolean condition(Object value) {
				if (value == null) return (getPassword1().getValue() == null);
		  		return value.equals( getPassword1().getValue());
			}
		};
	    secondPasswordValidator.setErrorMessage("Die Passwörter unterscheiden sich.");
		
		setModalMaskOpacity(10);
		
		dynamicForm = new DynamicForm();
		dynamicForm.setHeight("290");
		dynamicForm.setWidth(470);
		dynamicForm.setNumCols(2);
		StaticTextItem staticTextItem = new StaticTextItem("newStaticTextItem_0", "a");
		staticTextItem.setOutputAsHTML(true);
		staticTextItem.setShowTitle(false);
		staticTextItem.setAlign(Alignment.LEFT);
		staticTextItem.setTextAlign(Alignment.LEFT);
		staticTextItem.setStartRow(true);
		staticTextItem.setEndRow(true);
		staticTextItem.setColSpan(2);
		staticTextItem.setValue("Willkommen beim Distropia.\n\nBitte geben Sie als erstes Ihren Vor- und Zunamen ein.");
		StaticTextItem staticTextItem_1 = new StaticTextItem("newStaticTextItem_1", "a");
		staticTextItem_1.setValue("Bitte wählen Sie Ihren zukünftigen Benutzernamen und Ihr zukünftiges Passwort.");
		staticTextItem_1.setTextAlign(Alignment.LEFT);
		staticTextItem_1.setStartRow(true);
		staticTextItem_1.setShowTitle(false);
		staticTextItem_1.setOutputAsHTML(true);
		staticTextItem_1.setEndRow(true);
		staticTextItem_1.setColSpan(2);
		staticTextItem_1.setAlign(Alignment.LEFT);
		StaticTextItem staticTextItem_2 = new StaticTextItem("newStaticTextItem_2", "a");
		staticTextItem_2.setValue("Bedenken Sie bitte, dass es keine Möglichkeit gibt, Ihre Daten wieder herzustellen, falls Sie Ihr Passwort vergessen. \n\nDas Team wünscht Ihnen eine gute Zeit mit unserer Software.");
		staticTextItem_2.setTextAlign(Alignment.LEFT);
		staticTextItem_2.setStartRow(true);
		staticTextItem_2.setShowTitle(false);
		staticTextItem_2.setOutputAsHTML(true);
		staticTextItem_2.setEndRow(true);
		staticTextItem_2.setColSpan(2);
		staticTextItem_2.setAlign(Alignment.LEFT);
		password2 = new PasswordItem("password2", "Passwort wiederholen");
		password2.setRequired(true);
		password2.setTooltip("Bitte geben Sie Ihr Passwort erneut ein.");
		password2.setHint("");
		password1 = new PasswordItem("password1", "Passwort");
		password1.setRequired(true);
		password1.setTooltip("Wählen Sie Ihr Passwort mit bedacht, wenn Sie es vergessen, wird Ihr Profil nicht mehr zugänglich sein.");
		TextItem textItem = new TextItem("firstName", "Vorname");
		textItem.setRequired(true);
		TextItem textItem_1 = new TextItem("surName", "Nachname");
		textItem_1.setRequired(true);
		TextItem textItem_2 = new TextItem("userName", "Benutzername");
		textItem_2.setRequired(true);
		dynamicForm.setFields(new FormItem[] { staticTextItem, new RowSpacerItem(), textItem, textItem_1, new RowSpacerItem(), staticTextItem_1, new RowSpacerItem(), textItem_2, password1, password2, new RowSpacerItem(), staticTextItem_2});
		addItem(dynamicForm);
		dynamicForm.setPadding(10);
		dynamicForm.setAutoFocus(true);
		password2.setValidators( secondPasswordValidator);
		
		BottomButtons bottomButtons = new BottomButtons();
		bottomButtons.getButtonOk().addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				
				CreateUserAccountRequest createUserAccountRequest = new CreateUserAccountRequest( Distropia.getSessionId(), 
						dynamicForm.getValueAsString("userName"),
						dynamicForm.getValueAsString("password1"),
						dynamicForm.getValueAsString("firstName"),
						dynamicForm.getValueAsString("surName")
						);
				
				
				disable();
				Distropia.getRpcService().createUserAccount(createUserAccountRequest, new AsyncCallback<DefaultResponse>() {
					
					@Override
					public void onSuccess(DefaultResponse result) {
						enable();
						hide();
						if (Distropia.manageSessionAndErrors( result)){
							if (Distropia.getCurrentPage() instanceof LoginPage){
								((LoginPage)Distropia.getCurrentPage()).setUserName( dynamicForm.getValueAsString("userName"));
								((LoginPage)Distropia.getCurrentPage()).setCorrectFocus();
							}
							destroy();
						}
						
					}
					
					@Override
					public void onFailure(Throwable caught) {
						enable();
						Distropia.manageSessionAndErrors( caught);						
					}
				});
				
				
			}
		});
		bottomButtons.getButtonCancel().addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {					
					@Override
					public void execute() {
						destroy();						
					}
				});
			}
		});
		addItem(bottomButtons);
		
		
		
		
	    
	    if (Distropia.getDebug())
	    {
	    	dynamicForm.setValue("userName", "DebugUser");
	    	dynamicForm.setValue("firstName", "Volker");
	    	dynamicForm.setValue("surName", "Gronau");
	    	dynamicForm.setValue("password1", "1234");
	    	dynamicForm.setValue("password2", "1234");
	    }
	    
	}

	protected DynamicForm getDynamicForm() {
		return dynamicForm;
	}
	protected PasswordItem getPassword2() {
		return password2;
	}
	public PasswordItem getPassword1() {
		return password1;
	}
}
