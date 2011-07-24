package org.distropia.client.gui;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.ClickEvent;


public class MainPage_Menu extends VLayout {
	private MainPage mainPage;

	/**
	 * @wbp.parser.constructor
	 */
	public MainPage_Menu() {
		setLayoutAlign( Alignment.CENTER);
		setDefaultLayoutAlign(Alignment.CENTER);
		//setDefaultLayoutAlign( VerticalAlignment.TOP);
		
		
		Label lblNewLabel = new Label("Hey, willkommen");
		lblNewLabel.setWrap(false);
		lblNewLabel.setHeight(50);
		lblNewLabel.setExtraSpace(8);
		lblNewLabel.setStyleName("distropia-welcomeLabel");
		addMember(lblNewLabel);
		
		UserPictureAndName userPictureAndName = new UserPictureAndName();
		userPictureAndName.setExtraSpace(30);
		addMember( userPictureAndName);
		
		
		Button btnNewButton = new Button("Neuigkeiten");
		btnNewButton.setAlign(Alignment.CENTER);
		btnNewButton.setExtraSpace(8);
		addMember(btnNewButton);
		
		Button btnNewButton_2 = new Button("Nachrichten");
		btnNewButton_2.setExtraSpace(8);
		addMember(btnNewButton_2);
		
		Button btnNewButton_1 = new Button("Freunde");
		btnNewButton_1.setExtraSpace(8);
		addMember(btnNewButton_1);
		
		Button btnEinstellungen = new Button("Einstellungen");
		btnEinstellungen.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				mainPage.setPage( new Settings());
			}
		});
		btnEinstellungen.setExtraSpace(8);
		addMember(btnEinstellungen);
		
		LayoutSpacer layoutSpacer = new LayoutSpacer();
		addMember(layoutSpacer);
		
		this.setPadding(8);
		this.setAutoWidth();
		
		
		
	}

	public MainPage_Menu(MainPage mainPage) {
		this();
		this.mainPage = mainPage;
	}

}
