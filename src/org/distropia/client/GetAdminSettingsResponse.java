package org.distropia.client;

public class GetAdminSettingsResponse extends SetAdminSettingsRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3039106369021659792L;
	int webserverPort, externalWebserverPort, dhtPort, externalDHTPort, protocolVersion, connectedToNodes;
	boolean connectedToInternet, reachable;
	String internetAddress;
	

	public int getConnectedToNodes() {
		return connectedToNodes;
	}

	public void setConnectedToNodes(int connectedToNodes) {
		this.connectedToNodes = connectedToNodes;
	}

	public int getWebserverPort() {
		return webserverPort;
	}

	public void setWebserverPort(int webserverPort) {
		this.webserverPort = webserverPort;
	}

	public int getExternalWebserverPort() {
		return externalWebserverPort;
	}

	public void setExternalWebserverPort(int upnpWebserverPort) {
		this.externalWebserverPort = upnpWebserverPort;
	}

	public int getDhtPort() {
		return dhtPort;
	}

	public void setDhtPort(int dhtPort) {
		this.dhtPort = dhtPort;
	}

	public int getExternalDHTPort() {
		return externalDHTPort;
	}

	public void setExternalDHTPort(int upnpDHTPort) {
		this.externalDHTPort = upnpDHTPort;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int version) {
		this.protocolVersion = version;
	}

	public boolean isConnectedToInternet() {
		return connectedToInternet;
	}

	public void setConnectedToInternet(boolean connectedToInternet) {
		this.connectedToInternet = connectedToInternet;
	}

	public boolean isReachable() {
		return reachable;
	}

	public void setReachable(boolean reachable) {
		this.reachable = reachable;
	}

	public String getInternetAddress() {
		return internetAddress;
	}

	public void setInternetAddress(String internetAddress) {
		this.internetAddress = internetAddress;
	}

	public boolean isOnlyReachableByLocalhost() {
		return onlyReachableByLocalhost;
	}

	public void setOnlyReachableByLocalhost(boolean onlyReachableAtLocalHost) {
		this.onlyReachableByLocalhost = onlyReachableAtLocalHost;
	}

	public GetAdminSettingsResponse() {
		super();
	}

	public GetAdminSettingsResponse(String sessionId, Boolean succeeded,
			String failReason, String uniqueUserId, boolean admin) {
		super(sessionId, succeeded, failReason, uniqueUserId, admin);
	}

	public GetAdminSettingsResponse(String sessionId, Boolean succeeded,
			String failReason) {
		super(sessionId, succeeded, failReason);
	}
	
	
}
