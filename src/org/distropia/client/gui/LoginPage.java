package org.distropia.client.gui;
import java.util.Date;

import org.distropia.client.Distropia;
import org.distropia.client.LoginUserRequest;
import org.distropia.client.LoginUserResponse;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.LayoutResizeBarPolicy;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
import com.smartgwt.client.widgets.layout.HStack;


public class LoginPage extends Page {
	private DynamicForm loginForm;
	private TextItem userNameItem;
	private PasswordItem passwordItem;
	private ButtonItem buttonItem;

	public void setUserName( String userName)
	{
		loginForm.setValue( "userName", userName);
	}
	
	public void setCorrectFocus()
	{
		if ( getUserName().getValue()== null || "".equals( getUserName().getValue())) getUserName().focusInItem();
		else if ( getPasswordItem().getValue()== null || "".equals( getPasswordItem().getValue())) getPasswordItem().focusInItem();
		else getButtonLogin().focusInItem();
	}
	
	public void doLogin(final boolean saveLogin)
	{
		if (loginForm.validate())
		{
			loginForm.disable();
			Cookies.removeCookie("loginPanel_userName");
			Cookies.removeCookie("loginPanel_password");
			
			Distropia.getRpcService().loginUser(new LoginUserRequest( loginForm.getValueAsString("userName"), (String)loginForm.getValueAsString("password")), new AsyncCallback<LoginUserResponse>() {
				
				@Override
				public void onSuccess(LoginUserResponse loginUserResponse) {
					show();
					String password = loginForm.getValueAsString("password");
					loginForm.setValue("password", "");
					loginForm.enable();
					if (Distropia.manageSessionAndErrors( loginUserResponse))
					{
						if (loginUserResponse.getUserDoesNotExistOrWrongPassword())
						{
							Distropia.showErrorDialog("Der Benutzer existiert nicht, oder das Passwort ist leider nicht korrekt.");
							loginForm.setValue("password", "");
							setCorrectFocus();
						}
						else if (!loginUserResponse.isSucceeded())
						{
							Distropia.showErrorDialog("Die Aktion ist leider Fehlgeschlagen", loginUserResponse.getFailReason());
						}
						else if (loginUserResponse.isAdmin()) // adminuser logged in
						{
							AdministrationWindow administrationWindow = new AdministrationWindow();
							administrationWindow.show();
						}
						else // we can log in
						{
							
							if ( saveLogin) // set cookie
							{
								Date expires = new Date();
								expires.setTime(expires.getTime() + 14 * 24 * 60 * 60 * 1000);
								Cookies.setCookie("loginPanel_userName", loginForm.getValueAsString("userName"), expires);
								Cookies.setCookie("loginPanel_password", password, expires);							
							}
							
							Distropia.setLoggedInUniqueUserId( loginUserResponse.getUniqueUserId());
							Distropia.setSessionId( loginUserResponse.getSessionId());
							Distropia.setCurrentPage( new MainPage());
						}
					}
					
				}
				
				@Override
				public void onFailure(Throwable throwable) {
					show();
					loginForm.setValue("password", "");
					loginForm.enable();
					Distropia.manageSessionAndErrors( throwable);
				}
			});
		}
	}
	
	public LoginPage() {
		super();
		
		loginForm = new DynamicForm();
		loginForm.setPadding(8);
		loginForm.setBackgroundColor( "white");
		loginForm.setWidth("275px");
		loginForm.setShowEdges( true);
		loginForm.setAutoFocus( true);
		loginForm.setAutoHeight();
		
		HeaderItem headerItem = new HeaderItem("newHeaderItem_7", "New HeaderItem");
		headerItem.setDefaultValue("Anmeldung");
		userNameItem = new TextItem("userName", "Benutzername");
		userNameItem.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				if ("Enter".equals( event.getKeyName())) getPasswordItem().focusInItem();
			}
		});
		userNameItem.setRequired(true);
		passwordItem = new PasswordItem("password", "Passwort:");
		passwordItem.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				if ("Enter".equals( event.getKeyName())) doLogin( true);
			}
		});
		passwordItem.setSelectOnFocus(true);
		passwordItem.setRequired(true);
		buttonItem = new ButtonItem("newButtonItem_6", "Dauerhaft einloggen");
		buttonItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
			public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
				doLogin( true);
			}
		});
		buttonItem.setStartRow(false);
		buttonItem.setAutoFit(false);
		buttonItem.setWidth("146px");
		buttonItem.setHeight(30);
		ButtonItem buttonItem_1 = new ButtonItem("newButtonItem_4", "Nur einmalig einloggen");
		buttonItem_1.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
			public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
				doLogin( false);
			}
		});
		
		buttonItem_1.setWidth("146px");
		buttonItem_1.setAlign(Alignment.LEFT);
		buttonItem_1.setStartRow(false);
		buttonItem_1.setAutoFit(false);
		StaticTextItem staticTextItem_1 = new StaticTextItem("newStaticTextItem_7", "New StaticTextItem");
		staticTextItem_1.setShowTitle(false);
		StaticTextItem staticTextItem = new StaticTextItem("newStaticTextItem_7", "New StaticTextItem");
		staticTextItem.setShowTitle(false);
		loginForm.setFields(new FormItem[] { headerItem, userNameItem, passwordItem, staticTextItem_1, buttonItem, staticTextItem, buttonItem_1});
		
		HStack hLayout = new HStack();
		hLayout.setDefaultResizeBars(LayoutResizeBarPolicy.NONE);
		hLayout.setDefaultLayoutAlign(VerticalAlignment.CENTER);
		hLayout.setAlign(Alignment.CENTER);
		hLayout.addMember(loginForm);
		addMember(hLayout);
		
		//setAlign( VerticalAlignment.BOTTOM);
		
		Button btnIchBinNeu = new Button("Psst.., ich bin für die Neuen da!");
		btnIchBinNeu.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				CreateNewAccountDialog createNewAccountDialog = new CreateNewAccountDialog();
				createNewAccountDialog.show();
			}
		});
		btnIchBinNeu.setMargin(10);
		btnIchBinNeu.setAutoFit( true);
		addMember(btnIchBinNeu);
		
		setCorrectFocus();
		if (!Distropia.getClientDataStore().containsKey("didAutoLogin"))
		{
			Distropia.getClientDataStore().put("didAutoLogin", "");
			String userName = Cookies.getCookie( "loginPanel_userName");
			if (userName != null) loginForm.setValue("userName", userName);
			String password = Cookies.getCookie( "loginPanel_password");
			if (password != null)
			{
				loginForm.setValue("password", password);
				hide();
				doLogin( true);
			}	
		}
		
		Window.enableScrolling(false);
		
	}

	protected DynamicForm getLoginForm() {
		return loginForm;
	}
	protected TextItem getUserName() {
		return userNameItem;
	}
	protected PasswordItem getPasswordItem() {
		return passwordItem;
	}
	protected ButtonItem getButtonLogin() {
		return buttonItem;
	}
}
