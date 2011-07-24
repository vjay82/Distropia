package org.distropia.client.gui;

import org.distropia.client.ClientUserCredentialsResponse;
import org.distropia.client.DefaultRequest;
import org.distropia.client.Distropia;
import org.distropia.client.events.UserCredentialsChangedEvent;
import org.distropia.client.events.UserCredentialsChangedHandler;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;

public class UserPictureAndName extends HLayout implements UserCredentialsChangedHandler {
	Img img = null;
	Label label;
	
	private void reload(){
		if (Distropia.getInstance() != null){
			Distropia.getRpcService().getUserCredentials( new DefaultRequest( Distropia.getSessionId()), new AsyncCallback<ClientUserCredentialsResponse>() {
				@Override
				public void onSuccess(ClientUserCredentialsResponse result) {
					if (Distropia.manageSessionAndErrors( result)){
						label.setContents( result.toString());
					}
				}
				@Override
				public void onFailure(Throwable caught) {
					Distropia.manageSessionAndErrors( caught);
				}
			});
		}
	}
	
	public UserPictureAndName() {
		super();
		Distropia.getHandlerManager().addHandler( UserCredentialsChangedEvent.getType(), this);
		
		if (Distropia.getInstance() != null) img = new UserAccountPicture( Distropia.getLoggedInUniqueUserId(), false);
		else img = new Img();
		img.setExtraSpace(8);
		addMember( img);
		
		label = new Label();
		//label.setHeight(50);
		label.setWrap( false);
		label.setAutoWidth();
		addMember(label);
		
		this.setAutoHeight();
		this.setAutoWidth();
		reload();	
	}

	@Override
	public void destroy(){
		Distropia.getHandlerManager().removeHandler( UserCredentialsChangedEvent.getType(), this);
		super.destroy();
	}

	@Override
	public void userCredentialsChanged(UserCredentialsChangedEvent event) {
		reload();
	}
	
}
