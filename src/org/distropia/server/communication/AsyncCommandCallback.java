package org.distropia.server.communication;


public interface AsyncCommandCallback<T> {
	  void onFailure(Throwable caught);
	  void onSuccess(T result);
}
