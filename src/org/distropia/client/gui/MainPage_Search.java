package org.distropia.client.gui;
import org.distropia.client.Distropia;
import org.distropia.client.PublicUserCredentials;
import org.distropia.client.SearchRequest;
import org.distropia.client.SearchResponse;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

public class MainPage_Search extends Page {
	private DynamicForm dynamicForm;
	private VLayout searchResults;
	
	private void search(String searchForName) {
		searchResults.clear();
		Distropia.getRpcService().searchUser(new SearchRequest( Distropia.getSessionId(), searchForName), new AsyncCallback<SearchResponse>() {
			
			@Override
			public void onSuccess(SearchResponse result) {
				searchResults.draw();
				if (Distropia.manageSessionAndErrors( result)){
					for (PublicUserCredentials publicUserCredentials: result.getUsers()){
						SearchResponseItem searchResponseItem = new SearchResponseItem();
						searchResponseItem.setPublicUserCredentials(publicUserCredentials);
						searchResults.addMember( searchResponseItem);
						searchResponseItem.draw();
					}					
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
		searchResults = new VLayout();
		addMember( searchResults);
		searchResults.show();
	}

}
