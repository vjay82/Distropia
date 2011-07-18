package org.distropia.server.communication;

import java.util.ArrayList;

public class ProxyResponse extends DefaultServerResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2324048713704573123L;
	protected boolean accepted;
	protected ArrayList<WrappedServerCommandRequest> receievedCommands = null;
	protected boolean hasABigRequestPending;
	protected String contractId;
	
	
	public String getContractId() {
		return contractId;
	}
	public void setContractId(String contractId) {
		this.contractId = contractId;
	}
	public boolean isHasABigRequestPending() {
		return hasABigRequestPending;
	}
	public void setHasABigRequestPending(boolean hasABigRequestPending) {
		this.hasABigRequestPending = hasABigRequestPending;
	}
	

	public boolean isAccepted() {
		return accepted;
	}

	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

	public ArrayList<WrappedServerCommandRequest> getReceievedCommands() {
		if (receievedCommands == null) receievedCommands = new ArrayList<WrappedServerCommandRequest>();
		return receievedCommands;
	}

	public void setReceievedCommands(
			ArrayList<WrappedServerCommandRequest> receievedCommands) {
		this.receievedCommands = receievedCommands;
	}

	public ProxyResponse(boolean accepted) {
		this(accepted, false);
	}

	public ProxyResponse(boolean accepted, boolean hasABigRequestPending) {
		super();
		this.accepted = accepted;
		this.hasABigRequestPending = hasABigRequestPending;
	}
	
	public ProxyResponse() {
		super();
	}
}
