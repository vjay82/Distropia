package org.distropia.client.gui;

import org.distropia.client.Distropia;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;

public class UserPictureAndName extends HLayout {
	public UserPictureAndName() {
		Img img = new Img( GWT.getModuleBaseURL() + "userPicture.jpg?sessionId=" + Distropia.getSessionId());
		addMember(img);
		
		Label lblNewLabel = new Label( GWT.getModuleBaseURL() + "userPicture.jpg?sessionId=" + Distropia.getSessionId());
		addMember(lblNewLabel);
		
	}
	

}
