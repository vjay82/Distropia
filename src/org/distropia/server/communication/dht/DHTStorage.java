package org.distropia.server.communication.dht;

import java.io.IOException;
import java.security.PublicKey;
import java.util.ArrayList;

import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number480;
import net.tomp2p.storage.Data;
import net.tomp2p.storage.StorageDisk;

import org.distropia.client.PublicUserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DHTStorage extends StorageDisk {
	protected static final Logger logger = LoggerFactory.getLogger(DHTStorage.class);
	protected static final int MAXIMUM_USER_SAVE_TIME = 3600*24; // 1 day
	protected static final int MAXIMUM_HOST_ADDRESS_SAVE_TIME = 600 * 2; // 20 minutes

	public DHTStorage(String homeDirectory) throws Exception {
		super(homeDirectory);
	}
	
	@Override
	public Data get(Number480 key) {
		if (DHT.DOMAIN_USER.equals( key.getDomainKey())) {
			logger.info("getting User: " + key.toString());
			Data data = super.get(key);
			if (data== null) return null;
			try {
				@SuppressWarnings("unchecked")
				ArrayList<ItemWithCreationTime> items = (ArrayList<ItemWithCreationTime>) data.getObject();
				ArrayList<PublicUserCredentials> result = new ArrayList<PublicUserCredentials>( items.size());
				for(ItemWithCreationTime itemWithCreationTime: items)
					result.add( (PublicUserCredentials) itemWithCreationTime.getObject());
				return new Data( result);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}						
			
		}
		else if (DHT.DOMAIN_HOST_ADDRESS.equals( key.getDomainKey())) {
			logger.info("getting host: " + key.toString());
			Data data = super.get(key);
			if (data== null) return null;
			try {
				@SuppressWarnings("unchecked")
				ArrayList<ItemWithCreationTime> items = (ArrayList<ItemWithCreationTime>) data.getObject();
				ArrayList<String> result = new ArrayList<String>( items.size());
				for(ItemWithCreationTime itemWithCreationTime: items)
					result.add( (String) itemWithCreationTime.getObject());
				return new Data( result);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}						
			
		}
		else logger.error("not allowed get operation");
		return null;//super.get(key);
	}



	@Override
	public boolean put(Number480 key, Data newData, PublicKey publicKey,
			boolean putIfAbsent, boolean domainProtection) {
		
		
		if (DHT.DOMAIN_USER.equals( key.getDomainKey())){
			logger.info("storing User: " + key.toString());			
			try{
				Object o = newData.getObject();
				if (o instanceof PublicUserCredentials){
					
					ArrayList<ItemWithCreationTime> itemsToStore = new ArrayList<ItemWithCreationTime>(1);
					itemsToStore.add( ItemWithCreationTime.createWithTimeNow( o));
					
					// merging
					Data oldData = super.get( key);
					if (oldData != null){ 
						@SuppressWarnings("unchecked")
						ArrayList<ItemWithCreationTime> oldItems = (ArrayList<ItemWithCreationTime>) oldData.getObject();						
						oldItems.addAll( itemsToStore);
						
						for(int index = oldItems.size()-1; index>=0; index--)
						{
							if (oldItems.get(index).isOlderThanSeconds( MAXIMUM_USER_SAVE_TIME)) oldItems.remove(index);
							else{ // remove older identical item
								for(int subIndex = index-1; subIndex>=0; subIndex--){
									if ( ((PublicUserCredentials)oldItems.get(subIndex).getObject()).equals(oldItems.get(index).getObject())){
										oldItems.remove( subIndex);
										subIndex--;
										index--;
									}
								}
							}
						}
						
						logger.info("found old values, shrunk them together to " + oldItems.size());
						itemsToStore = oldItems;
					}
					
					Data data = new Data( itemsToStore);
					data.setTTLSeconds( MAXIMUM_USER_SAVE_TIME); // maximum save two days
					super.put( key, data, publicKey, putIfAbsent, domainProtection);
					logger.info("store succeeded");
					return true;
				}
			}
			catch (Exception e) {
				logger.error( "Error while storing data.", e);
			}
		}
		else if (DHT.DOMAIN_HOST_ADDRESS.equals( key.getDomainKey())){
			try{
				Object o = newData.getObject();
				if (o instanceof ArrayList){
					@SuppressWarnings("unchecked")
					ArrayList<String> addresses = (ArrayList<String>) o;
					ArrayList<ItemWithCreationTime> itemsToStore = new ArrayList<ItemWithCreationTime>();
					for (String address : addresses)
						itemsToStore.add( ItemWithCreationTime.createWithTimeNow( address));
					
					// merging
					Data oldData = super.get( key);
					if (oldData != null){ 
						@SuppressWarnings("unchecked")
						ArrayList<ItemWithCreationTime> oldItems = (ArrayList<ItemWithCreationTime>) oldData.getObject();						
						oldItems.addAll( itemsToStore);
						
						for(int index = oldItems.size()-1; index>=0; index--)
						{
							if (oldItems.get(index).isOlderThanSeconds(MAXIMUM_HOST_ADDRESS_SAVE_TIME)){
								oldItems.remove(index);
							}
							else{ // remove older identical item
								for(int subIndex = index-1; subIndex>=0; subIndex--){
									if (oldItems.get(subIndex).getObject().equals(oldItems.get(index).getObject())){
										oldItems.remove( subIndex);
										index--;
									}
								}
							}
						}
						
						itemsToStore = oldItems;
					}
					Data data = new Data( itemsToStore);
					data.setTTLSeconds( MAXIMUM_HOST_ADDRESS_SAVE_TIME); // maximum save time one hour
					super.put( key, data, publicKey, putIfAbsent, domainProtection);
					return true;
				}
			}
			catch (Exception e) {
				logger.error( "Error while storing data.", e);
			}
		}
		else logger.error( "Not allowed store.");
		
		return false;
	}

	@Override
	public void setProtection(ProtectionEnable protectionDomainEnable,
			ProtectionMode protectionDomainMode,
			ProtectionEnable protectionEntryEnable,
			ProtectionMode protectionEntryMode,
			ProtectionEntryInDomain protectionEntryInDomain) {
		// not allowed
	}

	@Override
	public void setProtectionDomainMode(ProtectionMode protectionDomainMode) {
		// not allowed
	}

	@Override
	public void setProtectionDomainEnable(
			ProtectionEnable protectionDomainEnable) {
		// not allowed
	}

	@Override
	public void setProtectionEntryMode(ProtectionMode protectionEntryMode) {
		// not allowed
	}

	@Override
	public void setProtectionEntryEnable(ProtectionEnable protectionEntryEnable) {
		// not allowed
	}

	@Override
	public void setProtectionEntryInDomain(
			ProtectionEntryInDomain protectionEntryInDomain) {
		// not allowed
	}

	@Override
	public void removeDomainProtection(Number160 removeDomain) {
		// not allowed
	}

	
}
