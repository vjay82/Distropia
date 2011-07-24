package org.distropia.client.gui;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;



public class MainPage extends Page {
	
	private Canvas page = null;
	private HLayout hLayout;
	
	public MainPage() {
		super();
		
		hLayout = new HLayout();
		hLayout.setAlign( VerticalAlignment.TOP);
		
		addMember(hLayout);
		
		MainPage_Menu mainpageMenu = new MainPage_Menu( this);
		mainpageMenu.setStyleName("distropia-mainPage-menu");
		hLayout.addMember( mainpageMenu);
		
		this.setAlign( VerticalAlignment.TOP);
	}
	
	public void setPage( Canvas canvas){
		if (page != null){
			hLayout.removeMember( page);
			page.destroy();
		}
		hLayout.addMember( canvas);
		page = canvas;
		page.setWidth100();		
	}

}
