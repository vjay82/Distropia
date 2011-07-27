package org.distropia.client.gui;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.distropia.client.Distropia;
import org.distropia.client.PublicUserCredentials;
import org.distropia.client.SearchRequest;
import org.distropia.client.SearchResponse;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.tile.TileGrid;
import com.smartgwt.client.widgets.tile.TileRecord;
import com.smartgwt.client.widgets.viewer.DetailViewerField;

public class MainPage_Search extends Page {
	private DynamicForm dynamicForm;
	private TileGrid tileGrid;
	
	private void search(String searchForName) {
		Distropia.getRpcService().searchUser(new SearchRequest( Distropia.getSessionId(), searchForName), new AsyncCallback<SearchResponse>() {
			@Override
			public void onSuccess(SearchResponse result) {
				if (Distropia.manageSessionAndErrors( result)){
					RecordList recordList = new RecordList();
					for (PublicUserCredentials publicUserCredentials: result.getUsers()){
						Record record = new Record();
						
						if (publicUserCredentials.getPicture() != null){
							try {
								record.setAttribute("picture", new String( publicUserCredentials.getPicture(), "UTF-8" ) );
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
						record.setAttribute("name", publicUserCredentials.toString());
						if (publicUserCredentials.isOneBirthFieldSet())
							record.setAttribute("birthday", publicUserCredentials.getGender().toDisplayString() + ", Geb: " + publicUserCredentials.toBirthdayString());
						else
							record.setAttribute("birthday", publicUserCredentials.getGender().toDisplayString());
						record.setAttribute("address", publicUserCredentials.toAddressBlock());
						record.setAttribute("puc", publicUserCredentials);
						
						
				        recordList.add(record);
					}
					tileGrid.setData( recordList);
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Distropia.manageSessionAndErrors( caught);
			}
		});
	}
	
	public MainPage_Search() {
		super();
		
		dynamicForm = new DynamicForm();
		ButtonItem buttonItem = new ButtonItem("newButtonItem_2", "Suchen");
		buttonItem.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				search( dynamicForm.getValueAsString("name"));
			}
		});
		dynamicForm.setFields(new FormItem[] { new TextItem("name", "Name"), buttonItem});
		addMember(dynamicForm);
		
		tileGrid = new TileGrid();
		tileGrid.setTileWidth(200);
		tileGrid.setTileHeight(200);
		
		DetailViewerField pictureField = new DetailViewerField("picture");
        pictureField.setType("image");
        pictureField.setImageURLPrefix( Distropia.getWebHelperUrl() + "?picture=tmp&sessionId=" + Distropia.getSessionId() + "&filename=");
        
        DetailViewerField nameField = new DetailViewerField("name");
        DetailViewerField birthdayField = new DetailViewerField("birthday");
        DetailViewerField addressField = new DetailViewerField("address");
       
        DetailViewerField spacerField = new DetailViewerField();
        spacerField.setType("spacer");
        spacerField.setHeight(12);
        
        tileGrid.setFields( spacerField, pictureField, nameField, birthdayField, addressField);
        addMember(tileGrid);
	}

}
