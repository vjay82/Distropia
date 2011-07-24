package org.distropia.client.gui;

import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

public class Settings extends VLayout {
	public Settings() {
		
		TabSet tabSet = new TabSet();
		
		Tab tab = new Tab("Profileinstellungen");
		tab.setPane( new Settings_Profile());
		tabSet.addTab(tab);
		
		
		Tab tab_1 = new Tab("Gruppen");
		tabSet.addTab(tab_1);
		addMember(tabSet);
		
		setPadding(8);
		setHeight100();
	}

}
