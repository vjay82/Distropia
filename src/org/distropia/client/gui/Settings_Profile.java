package org.distropia.client.gui;

import org.distropia.client.Distropia;
import org.distropia.client.JavaScriptMethodCallback;
import org.distropia.client.JavaScriptMethodHelper;
import org.distropia.client.events.UserCredentialsChangedEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.types.Alignment;

public class Settings_Profile extends VLayout {
	public Settings_Profile() {
		
		VLayout userPictureLayout = new VLayout();
		
		DynamicForm userPictureForm = new DynamicForm();
		HeaderItem headerItem = new HeaderItem("newHeaderItem_2", "");
		headerItem.setDefaultValue("Dein Benutzerbild");
		userPictureForm.setFields(new FormItem[] { headerItem});
		userPictureLayout.addMember(userPictureForm);
		
		final DynamicForm dynamicForm = new DynamicForm();
		dynamicForm.setNumCols(8);
		dynamicForm.setEncoding(Encoding.MULTIPART);
		dynamicForm.setCanSubmit(true);
		dynamicForm.setTarget("hidden_frame");
		dynamicForm.setAction( Distropia.getWebHelperUrl() + "?upload=userPicture&sessionId=" + Distropia.getSessionId());
		
		UploadItem uploadItem = new UploadItem("newUploadItem_2", "New UploadItem");
		uploadItem.setStartRow(false);
		uploadItem.setEndRow(false);
		uploadItem.setShowTitle(false);
		uploadItem.setRequired(true);
		
		final HiddenItem callbackItem = new HiddenItem("callbackName");
		ButtonItem uploadButton = new ButtonItem("uploadButton", "Hochladen");
		uploadButton.setShowTitle(false);
		uploadButton.setStartRow(false);
		uploadButton.addClickHandler( new ClickHandler() {
			public void onClick(ClickEvent event) {
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
					callbackItem.setValue(callbackName); //set hidden item
				}
				
				dynamicForm.submitForm();
			}
		});
		StaticTextItem staticTextItem = new StaticTextItem("newStaticTextItem_4", "New StaticTextItem");
		staticTextItem.setWrap(false);
		staticTextItem.setEndRow(false);
		staticTextItem.setStartRow(true);
		staticTextItem.setValue("Um dein Bild zu ändern zuerst:");
		staticTextItem.setShowTitle(false);
		StaticTextItem staticTextItem_1 = new StaticTextItem("newStaticTextItem_5", "New StaticTextItem");
		staticTextItem_1.setTextAlign(Alignment.RIGHT);
		staticTextItem_1.setWrap(false);
		staticTextItem_1.setShowTitle(false);
		staticTextItem_1.setValue("Und dann:");
		dynamicForm.setFields(new FormItem[] { callbackItem, staticTextItem, uploadItem, staticTextItem_1, uploadButton});
		
		UserAccountPicture userAccountPicture = new UserAccountPicture(null, true);
		
		userPictureLayout.addMember( userAccountPicture);
		userPictureLayout.addMember(dynamicForm);
		addMember(userPictureLayout);
	}
	
	private void uploadFinished(JavaScriptObject obj) {
		if (!"success".equals( obj.toString())) Distropia.showErrorDialog( obj.toString());
		else{
			Distropia.getHandlerManager().fireEvent( new UserCredentialsChangedEvent());
		}
		
	}

}
					