package org.distropia.server.communication;

import java.util.ArrayList;

public class ProxyRequest extends DefaultServerRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7688884687326276833L;
	protected String contractId;
	protected ArrayList<WrappedServerCommandResponse> answerCommands = null;
	protected boolean downloadOneBigRequestMode;
	
	
	public String getContractId() {
		return contractId;
	}
	public void setContractId(String contractId) {
		this.contractId = contractId;
	}
	public ArrayList<WrappedServerCommandResponse> getAnswerCommands() {
		return answerCommands;
	}
	public void setAnswerCommands(
			ArrayList<WrappedServerCommandResponse> answerCommands) {
		this.answerCommands = answerCommands;
	}
	
	public boolean isDownloadOneBigRequestMode() {
		return downloadOneBigRequestMode;
	}
	public void setDownloadOneBigRequestMode(boolean downloadOneBigRequestMode) {
		this.downloadOneBigRequestMode = downloadOneBigRequestMode;
	}
	
	public ProxyRequest(ArrayList<WrappedServerCommandResponse> answerCommands,
			boolean downloadOneBigRequestMode, String contractId) {
		super();
		this.answerCommands = answerCommands;
		this.downloadOneBigRequestMode = downloadOneBigRequestMode;
		this.contractId = contractId;
	}
	public ProxyRequest() {
		super();
	}
	
	
}
