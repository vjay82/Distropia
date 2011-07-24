package org.distropia.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class UserCredentialsChangedEvent extends GwtEvent<UserCredentialsChangedHandler> {
	private static final Type<UserCredentialsChangedHandler> TYPE = new Type<UserCredentialsChangedHandler>();

	@Override
	protected void dispatch(UserCredentialsChangedHandler handler) {
		handler.userCredentialsChanged( this);
	}

	@Override
	public Type<UserCredentialsChangedHandler> getAssociatedType() {
		return TYPE;
	}

	public static Type<UserCredentialsChangedHandler> getType() {
		return TYPE;
	}
		

}
