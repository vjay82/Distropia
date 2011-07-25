package org.distropia.client.gui;

import org.distropia.client.PublicUserCredentials;

import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.Label;

public class SearchResponseItem extends HLayout {
	MyFitImage myFitImage;
	Label userName;
	
	public void setPublicUserCredentials( PublicUserCredentials publicUserCredentials){
		if (publicUserCredentials.getPicture() != null){
			
		}
		userName.setContents( publicUserCredentials.getFirstName() + " " + publicUserCredentials.getSurName());
	}
	
	public SearchResponseItem() {
		
		myFitImage = new MyFitImage();
		myFitImage.setImgMaxWidth( 200);
		myFitImage.setImgMaxHeight( 200);
		addMember( myFitImage);
		
		userName = new Label("New Label");
		addMember(userName);
		
		setHeight(200);
		setWidth100();
	}

}
