package org.distropia.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface UserCredentialsChangedHandler extends EventHandler {
	public void userCredentialsChanged(UserCredentialsChangedEvent event);
}
