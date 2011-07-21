package org.distropia.client.gui;

import org.distropia.client.BootstrapRequest;
import org.distropia.client.DefaultUserResponse;
import org.distropia.client.Distropia;
import org.distropia.client.GetAdminSettingsRequest;
import org.distropia.client.GetAdminSettingsResponse;
import org.distropia.client.SetAdminSettingsRequest;
import org.distropia.client.Utils;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.RowSpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

public class AdministrationWindow extends Window {
	private Timer updateTimer = null;
	private boolean statisticsOnly = false;
	private DynamicForm informationForm;
	private DynamicForm settingsForm;
	private TextItem updateFrom;
	private DynamicForm bootstrapForm;
	
	
	private void saveSettings(){
		SetAdminSettingsRequest request = new SetAdminSettingsRequest(Distropia.getSessionId());
		request.setAutomaticUpdates( (Boolean)settingsForm.getValue("automaticUpdates"));
		request.setAutomaticUpdatesFromBranch( (String)settingsForm.getValue("updateFrom"));
		request.setOnlyReachableByLocalhost( (Boolean)settingsForm.getValue("onlyLocalhost"));
		disable();
		Distropia.getRpcService().setAdminSettings( request, new AsyncCallback<DefaultUserResponse>() {
			
			@Override
			public void onSuccess(DefaultUserResponse result) {
				if (Distropia.manageSessionAndErrors( result))
				{
					Scheduler.get().scheduleDeferred(new ScheduledCommand() {
						@Override
						public void execute() {
							destroy();
						}
					});
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				enable();
				Distropia.manageSessionAndErrors( caught);				
			}
		});
		
	}
	
	@Override
	public void destroy() {		
		super.destroy();
		if ( Distropia.getCurrentPage() instanceof LoginPage)
			((LoginPage)Distropia.getCurrentPage()).setCorrectFocus();
	}



	private void doBootstrap(){
		BootstrapRequest request = new BootstrapRequest(Distropia.getSessionId());
		request.setAddress( (String)bootstrapForm.getValue("bootstrapFrom"));
		disable();
		
		Distropia.getRpcService().bootstrap( request, new AsyncCallback<DefaultUserResponse>() {
			
			@Override
			public void onSuccess(DefaultUserResponse result) {
				enable();
				if (Distropia.manageSessionAndErrors( result))
				{
					if (result.isSucceeded()) Distropia.showInformationDialog("Bootstrap erfolgreich.");
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				enable();
				Distropia.manageSessionAndErrors( caught);				
			}
		});
	}
	
	private void updatePage(){
		GetAdminSettingsRequest request = new GetAdminSettingsRequest(Distropia.getSessionId(), statisticsOnly);
		
		
		Distropia.getRpcService().getAdminSettings( request, new AsyncCallback<GetAdminSettingsResponse>() {
			
			@Override
			public void onSuccess(GetAdminSettingsResponse result) {
				if (Distropia.manageSessionAndErrors( result))
				{
					if (!result.isAdmin()) destroy();
					informationForm.setValue("webserverPort", result.getWebserverPort());
					informationForm.setValue("connectedToInternet", Utils.boolToLanguage( result.isConnectedToInternet()));
					informationForm.setValue("contactWith", result.getConnectedToNodes() + " Knoten");
					
					if (Utils.isNullOrEmpty( result.getInternetAddress()))
						informationForm.setValue("internetAddress", "-");
					else informationForm.setValue("internetAddress", result.getInternetAddress());
					
					informationForm.setValue("dhtPort", result.getDhtPort());
					informationForm.setValue("protocolVersion", result.getProtocolVersion());
					informationForm.setValue("reachable", Utils.boolToLanguage( result.isReachable()));
					
					if (result.getExternalWebserverPort() == -1)
					{
						informationForm.setValue("externalWebserverPort", "unbekannt");
						informationForm.setValue("externalDHTPort", "unbekannt");
					}
					else
					{
						informationForm.setValue("externalWebserverPort", result.getExternalWebserverPort());
						informationForm.setValue("externalDHTPort", result.getExternalDHTPort());
					}
					
					bootstrapForm.setVisible( result.getConnectedToNodes() == 0);
					
					if (!statisticsOnly){ // first request						
						settingsForm.setValue("automaticUpdates", result.isAutomaticUpdates());
						settingsForm.setValue("onlyLocalhost", result.isOnlyReachableByLocalhost());
						settingsForm.setValue("updateFrom", result.getAutomaticUpdatesFromBranch());
						statisticsOnly = true;
						enable();
					}
					
					
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				destroy();
				Distropia.manageSessionAndErrors( caught);				
			}
		});
	}

	public AdministrationWindow() {
		disable();
		setShowFooter(false);
		setAutoSize(true);
		setAutoCenter(true);
		setTitle("Server Administration");
		resizeTo(350, 200);
		
		settingsForm = new DynamicForm();
		settingsForm.setAutoFocus( true);
		settingsForm.setAutoWidth();
		
		informationForm = new DynamicForm();
		informationForm.setWidth(481);
		informationForm.setWrapItemTitles(false);
		informationForm.setNumCols(4);
		informationForm.setMargin(8);
		StaticTextItem webServerPort = new StaticTextItem("webserverPort", "Webserver Port");
		webServerPort.setValue("");
		StaticTextItem dhtPort = new StaticTextItem("dhtPort", "DHT Port");
		dhtPort.setOutputAsHTML(false);
		dhtPort.setValue("");
		StaticTextItem upnpWebServerPort = new StaticTextItem("externalWebserverPort", "Externer Webserver Port:");
		upnpWebServerPort.setOutputAsHTML(false);
		StaticTextItem upnpDHTPort = new StaticTextItem("externalDHTPort", "Externer DHT Port");
		upnpDHTPort.setOutputAsHTML(false);
		StaticTextItem connectedToInternet = new StaticTextItem("connectedToInternet", "Mit Internet verbunden");
		connectedToInternet.setOutputAsHTML(false);
		StaticTextItem internetAddress = new StaticTextItem("internetAddress", "Internet Adresse");
		internetAddress.setOutputAsHTML(false);
		StaticTextItem reachable = new StaticTextItem("reachable", "Vom Internet erreichbar");
		reachable.setOutputAsHTML(false);
		StaticTextItem contactWithXNodes = new StaticTextItem("contactWith", "Kontakt mit");
		contactWithXNodes.setOutputAsHTML(false);
		StaticTextItem serverVersion = new StaticTextItem("protocolVersion", "Protokollversion");
		serverVersion.setOutputAsHTML(false);
		HeaderItem headerItem = new HeaderItem("newHeaderItem_10", "New HeaderItem");
		headerItem.setDefaultValue("Informationen");
		informationForm.setFields(new FormItem[] { headerItem, webServerPort, upnpWebServerPort, dhtPort, upnpDHTPort, connectedToInternet, internetAddress, reachable, contactWithXNodes, serverVersion});
		addItem(informationForm);
		settingsForm.setFixedColWidths(false);
		settingsForm.setNumCols(4);
		settingsForm.setWrapItemTitles(false);
		settingsForm.setMargin(8);
		CheckboxItem checkBoxUpdates = new CheckboxItem("automaticUpdates", "Automatische Updates aktivieren");
		checkBoxUpdates.setTabIndex(0);
		checkBoxUpdates.setLabelAsTitle(true);
		checkBoxUpdates.setShowTitle(true);
		CheckboxItem checkBoxOnlyLocalhost = new CheckboxItem("onlyLocalhost", "Nur über Localhost erreichbar");
		checkBoxOnlyLocalhost.setStartRow(true);
		checkBoxOnlyLocalhost.setShowTitle(true);
		checkBoxOnlyLocalhost.setLabelAsTitle(true);
		HeaderItem headerItem_1 = new HeaderItem("newHeaderItem_5", "New HeaderItem");
		headerItem_1.setDefaultValue("Einstellungen");
		updateFrom = new TextItem("updateFrom", "Updates beziehen von");
		updateFrom.setTabIndex(1);
		settingsForm.setFields(new FormItem[] { headerItem_1, checkBoxUpdates, updateFrom, checkBoxOnlyLocalhost});
		addItem(settingsForm);
		
		bootstrapForm = new DynamicForm();
		bootstrapForm.setWidth(481);
		bootstrapForm.setWrapItemTitles(false);
		bootstrapForm.setNumCols(4);
		bootstrapForm.setMargin(8);
		HeaderItem headerItem_2 = new HeaderItem("newHeaderItem_5", "New HeaderItem");
		headerItem_2.setDefaultValue("Bootstrap");
		ButtonItem buttonItem = new ButtonItem("newButtonItem_3", "Bootstrap durchführen");
		buttonItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
			
			@Override
			public void onClick( com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
				doBootstrap();				
			}
		});
		buttonItem.setStartRow(false);
		StaticTextItem staticTextItem = new StaticTextItem("newStaticTextItem_4", "New StaticTextItem");
		staticTextItem.setClipValue(false);
		staticTextItem.setValue("Dieser Server ist nicht mit dem Distropia-Netzwerk verbunden. Ein Bootstrap ist notwendig, um den ersten Kontakt herzustellen. Gib bitte die Adresse eines anderen Distropia-Servers an, dies muss in der Form \"Protokoll://Netzwerkadressse:Port\" erfolgen. Ein Beispiel: http://nrg.metadns.cx:9090");
		staticTextItem.setShowTitle(false);
		staticTextItem.setEndRow(true);
		staticTextItem.setColSpan(4);
		TextItem textItem = new TextItem("bootstrapFrom", "Bootstrap von");
		textItem.setValue("http://nrg.metadns.cx:9090");
		bootstrapForm.setFields(new FormItem[] { headerItem_2, staticTextItem, new RowSpacerItem(), textItem, buttonItem});
		addItem(bootstrapForm);
		
		
		Label lblNewLabel = new Label("");
		addItem(lblNewLabel);
		lblNewLabel.setHeight("50px");
		
		BottomButtons bottomButtons = new BottomButtons();
		bottomButtons.setWidth(500);
		bottomButtons.getButtonOk().addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				saveSettings();
			}
		});
		bottomButtons.getButtonCancel().addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					
					@Override
					public void execute() {
						destroy();
					}
				});
			}
		});
		addItem( bottomButtons);
			
		
		updateTimer = new Timer() {
		      public void run() {
		        updatePage();
		      }
		};
		updateTimer.scheduleRepeating(5000);
		
		addAttachHandler(new AttachEvent.Handler() {
			@Override
			public void onAttachOrDetach(AttachEvent event) {
				if ((!event.isAttached()) && (updateTimer!= null)) 
				{
					updateTimer.cancel();
					updateTimer = null;
				}	
			}
		});
		
		updatePage();
	}
	
	
	protected DynamicForm getInformationForm() {
		return informationForm;
	}
	protected DynamicForm getSettingsForm() {
		return settingsForm;
	}
	protected DynamicForm getBootstrapForm() {
		return bootstrapForm;
	}
}
