package org.distropia.server.communication;

public class ProxyCacheEntry {
	protected WrappedServerCommandRequest wrappedServerCommand;
	protected WrappedServerCommandResponse wrappedServerCommandResponse;
	protected Thread sendingThread;
	protected boolean done = false;
	protected boolean processing = false;
	
	public long getRequestSize(){
		if (wrappedServerCommand == null) return 0;
		if (wrappedServerCommand.data == null) return 0;
		return wrappedServerCommand.data.length;
	}
	
	public boolean isProcessing() {
		return processing;
	}
	public void setProcessing(boolean processing) {
		this.processing = processing;
	}
	public ProxyCacheEntry(WrappedServerCommandRequest wrappedServerCommand,
			Thread sendingThread) {
		super();
		this.wrappedServerCommand = wrappedServerCommand;
		this.sendingThread = sendingThread;
	}
	public WrappedServerCommandRequest getWrappedServerCommand() {
		return wrappedServerCommand;
	}
	public void setWrappedServerCommand(
			WrappedServerCommandRequest wrappedServerCommand) {
		this.wrappedServerCommand = wrappedServerCommand;
	}
	public WrappedServerCommandResponse getWrappedServerCommandResponse() {
		return wrappedServerCommandResponse;
	}
	public void setWrappedServerCommandResponse(
			WrappedServerCommandResponse wrappedServerCommandResponse) {
		this.wrappedServerCommandResponse = wrappedServerCommandResponse;
	}
	public Thread getSendingThread() {
		return sendingThread;
	}
	public void setSendingThread(Thread sendingThread) {
		this.sendingThread = sendingThread;
	}
	public ProxyCacheEntry() {
		super();
	}
	public boolean isDone() {
		return done;
	}
	public void setDone(boolean done) {
		this.done = done;
	}
}
