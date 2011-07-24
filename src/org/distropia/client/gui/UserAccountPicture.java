package org.distropia.client.gui;

import org.distropia.client.Distropia;
import org.distropia.client.Utils;
import org.distropia.client.events.UserCredentialsChangedEvent;
import org.distropia.client.events.UserCredentialsChangedHandler;

import com.google.gwt.core.client.GWT;

public class UserAccountPicture extends MyFitImage implements UserCredentialsChangedHandler{
	private static String noCacheItem = "";
	protected String uniqueUserId = null;
	protected boolean bigPicture;
	
	public String getUniqueUserId() {
		return uniqueUserId;
	}
	public void setUniqueUserId(String newUniqueUserId) {
		if (Distropia.getInstance() != null){
			if (uniqueUserId == null) uniqueUserId = Distropia.getLoggedInUniqueUserId();
			else this.uniqueUserId = newUniqueUserId;
			if (Utils.equalsWithNull( this.uniqueUserId, Distropia.getLoggedInUniqueUserId())){
				Distropia.getHandlerManager().addHandler( UserCredentialsChangedEvent.getType(), this);
			}
		}
		else this.uniqueUserId = newUniqueUserId;
		reload();
	}
	public boolean isBigPicture() {
		return bigPicture;
	}
	public void setBigPicture(boolean bigPicture) {
		this.bigPicture = bigPicture;
		if (bigPicture){
			setImgMaxWidth(300);
			setImgMaxHeight(300);
			setStyleName("imagedropshadow");
		}
		else{
			setImgMaxWidth(50);
			setImgMaxHeight(50);
			setStyleName("distropia-userAccountPicture-Small");
		}
		reload();
	}
	public UserAccountPicture(String uniqueUserId, boolean bigPicture) {
		super();
		setUniqueUserId( uniqueUserId);
		setBigPicture(bigPicture);
		reload();
		
	}
	
	@Override
	public void destroy(){
		Distropia.getHandlerManager().removeHandler( UserCredentialsChangedEvent.getType(), this);
		super.destroy();
	}
	
	public void reload(){
		if (Distropia.getInstance() != null){
			GWT.log("loading img");
			setSrc( Distropia.getWebHelperUrl() + "?picture=user&sessionId=" + Distropia.getSessionId() + "&uniqueUserId=" + uniqueUserId + "&bigPicture=" + isBigPicture() + noCacheItem);
		}
			
	}
	@Override
	public void userCredentialsChanged(UserCredentialsChangedEvent event) {
		noCacheItem = "&noCache=" + Math.random();
		reload();
	}
	
}
