package org.distropia.client.gui;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.layout.HLayout;

public class BottomButtons extends HLayout {
	private Button buttonOk;
	private Button buttonCancel;
	public BottomButtons() {
		
		setMembersMargin(8);
		setDefaultLayoutAlign(VerticalAlignment.CENTER);
		setHeight( 51);
		setWidth100();
		
		
		
		buttonOk = new Button("OK");
		addMember(buttonOk);
		
		buttonCancel = new Button("Abbrechen");
		addMember(buttonCancel);
		
		setAlign( Alignment.RIGHT);
		setMargin(16);
		
	}

	public Button getButtonOk() {
		return buttonOk;
	}
	public Button getButtonCancel() {
		return buttonCancel;
	}
}
