package org.distropia.server;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;

public class Configuration {
	protected final static int VERSION = 1;
	XMLConfiguration xmlConfiguration;

	public Configuration( File configurationFile) throws ConfigurationException {
		super();
		xmlConfiguration = new XMLConfiguration( configurationFile);
		xmlConfiguration.setExpressionEngine(new XPathExpressionEngine());
		xmlConfiguration.setAutoSave( true);
		xmlConfiguration.setReloadingStrategy( new FileChangedReloadingStrategy());
		
		int version = xmlConfiguration.getInt("config/version", 0);
		if (version == 0) writeDefaultConfigFile(); // no configfile yet, write defaults		
	}
	
	private void writeDefaultConfigFile() {
		xmlConfiguration.addProperty("/ config/version", VERSION);
		
		xmlConfiguration.addProperty("/ admin/enabled", false);
		xmlConfiguration.addProperty("/admin username", "admin");
		xmlConfiguration.addProperty("/admin password", "admin");
		
		xmlConfiguration.addProperty("/ updates/enabled", true);
		xmlConfiguration.addProperty("/updates branch", "main");
		xmlConfiguration.addProperty("/ security/onlyLocalhost", true);
		
		xmlConfiguration.addProperty("/ connection/upnp/active", true);
		xmlConfiguration.addProperty("/connection manualExternalHost", "");
		xmlConfiguration.addProperty("/connection manualExternalPort", "-1");
		xmlConfiguration.addProperty("/connection manualExternalDHTPort", "-1");
		
		xmlConfiguration.addProperty("/connection bootstrap/address", "http://nrg.metadns.cx:9090");
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getBootstrapAddresses(){
		return (ArrayList<String>) xmlConfiguration.getList("connection/bootstrap/address");
	}

	public boolean isUPNPActive(){
		return xmlConfiguration.getBoolean("connection/upnp/active", true);
	}
	
	public int getExternalPort(){
		return xmlConfiguration.getInteger("connection/manualExternalPort", -1);
	}
	
	public int getExternalDHTPort(){
		return xmlConfiguration.getInteger("connection/manualExternalDHTPort", -1);
	}
	
	public String getManualExternalHost(){
		return xmlConfiguration.getString("connection/manualExternalHost", "");
	}
	
	public boolean isOnlyReachableByLocalhost(){
		return xmlConfiguration.getBoolean("security/onlyLocalhost", true);
	}
	
	public void setOnlyReachableByLocalhost(boolean localhostEnabled){
		xmlConfiguration.setProperty("/security onlyLocalhost", localhostEnabled);
	}
	
	public boolean isUpdatesEnabled(){
		return xmlConfiguration.getBoolean("updates/enabled", true);
	}
	
	public void setUpdatesEnabled(boolean updatesEnabled){
		xmlConfiguration.setProperty("/updates enabled", updatesEnabled);
	}
	
	public String getUpdatesBranch(){
		return xmlConfiguration.getString("updates/branch", "main");
	}
	
	public void setUpdatesBranch(String updatesBranch){
		xmlConfiguration.setProperty("/updates branch", updatesBranch);
	}
	
	public boolean isAdminAccountEnabled(){
		return xmlConfiguration.getBoolean("admin/enabled", false);
	}
	
	public String getAdminAccountUserName(){
		return xmlConfiguration.getString("admin/username", null);
	}
	
	public String getAdminAccountPassword(){
		return xmlConfiguration.getString("admin/password", null);
	}
	
	

}
