package org.distropia.client.gui;

import com.google.gwt.user.client.ui.FlexTable;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.VLayout;
import com.google.gwt.user.client.ui.Hyperlink;

public class MainPage_LeftPart extends FlexTable {
	public MainPage_LeftPart() {
		
		HStack hStack = new HStack();
		
		Img img = new Img();
		img.setHeight(50);
		img.setWidth(50);
		hStack.addMember(img);
		
		VLayout layout = new VLayout();
		
		Hyperlink hprlnkNewHyperlink = new Hyperlink("New hyperlink", false, "newHistoryToken");
		layout.addMember(hprlnkNewHyperlink);
		hStack.addMember(layout);
		setWidget(0, 0, hStack);
	}

}
