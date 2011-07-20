package org.distropia.server.communication;

import java.util.ArrayList;

import org.apache.xerces.impl.dv.util.Base64;
import org.distropia.server.Backend;
import org.distropia.server.database.EncryptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vjay
 *This threads searches and manages ProxyKnownHosts, if this Host is not directly reachable from the network
 */
public class ProxyConnectionThread extends Thread {
	
	private KnownHost proxyHost = null;
	private volatile boolean accepted = false;
	protected static transient Logger logger = LoggerFactory.getLogger(ProxyConnectionThread.class); // $codepro.audit.disable transientFieldInNonSerializable
	protected String contractId = null;
	
	
	public boolean isAccepted() {
		return accepted;		
	}


	public KnownHost getProxyHost() {
		return proxyHost;
	}



	public void setProxyHost(KnownHost proxyHost) {
		this.proxyHost = proxyHost;
	}



	public ProxyConnectionThread(KnownHost proxyHost) {
		super();
		this.proxyHost = proxyHost;
		this.setName( "Proxy connection thread to " + proxyHost.getUniqueHostId());
	}
	
	class DownloadBigRequestThread extends Thread{
		ArrayList<WrappedServerCommandResponse> answerCommands;
		@Override
		public void run() {
			try {				
				
				ProxyResponse proxyResponse = null;
				do {
					proxyResponse = (ProxyResponse) proxyHost.sendCommand( new ProxyRequest( answerCommands, true, contractId));
					if (proxyResponse.isAccepted()) answerCommands = processRecievedCommands( proxyResponse);
				}
				while (proxyResponse.isAccepted());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public DownloadBigRequestThread( ArrayList<WrappedServerCommandResponse> answerCommands) {
			super();
			setName("Download big request from " + proxyHost.getUniqueHostId());
			setDaemon( true);
			this.answerCommands = answerCommands;			
		}
		
	}

	protected ArrayList<WrappedServerCommandResponse> processRecievedCommands( ProxyResponse proxyResponse) throws Exception{
		if (proxyResponse.getReceievedCommands() != null)
		{
			KnownHosts knownHosts = Backend.getMyKnownHosts();
			ArrayList<WrappedServerCommandResponse> answerCommands = new ArrayList<WrappedServerCommandResponse>( proxyResponse.getReceievedCommands().size());
			for( WrappedServerCommandRequest wrappedServerCommandRequest: proxyResponse.getReceievedCommands())
			{
				KnownHost knownHost = knownHosts.getKnownHostOrNull( wrappedServerCommandRequest.getFromUniqueHostId());
				
				if (knownHost == null)
				{
					knownHost = new KnownHost( knownHosts, wrappedServerCommandRequest.getFromUniqueHostId());
					if (knownHost != proxyHost) knownHost.addAddress("uid://" + proxyHost.getUniqueHostId());
					synchronized (knownHosts) {
						knownHosts.add( knownHost);
					}
					
				}							
				
				if (knownHost != proxyHost) knownHost.addAddress("uid://" + proxyHost.getUniqueHostId());
				
				try {
					/*
					String remoteAddr = wrappedServerCommandRequest.getRemoteAddr();
					if (remoteAddr == null) // if the wrappedCommand is directly from the other node this may null, because it doesn't know its own address
					{
						// we fill it with the address used to send this command, it succeeded, so it should be valid
						String lastValidAddress = proxyHost.getLastValidAddress();
						if (lastValidAddress != null)
						{
							URI uri = new URI( lastValidAddress);
							remoteAddr = uri.getHost();
						}
					}
					*/
					DefaultServerRequest requestCommand = (DefaultServerRequest) EncryptableObject.createFrom(wrappedServerCommandRequest.getEncrypted(), wrappedServerCommandRequest.getData(), knownHost.getKeyPair().getPrivate());
					
					if ((requestCommand != null) && (!(requestCommand instanceof ProxyRequest)))
					{
						DefaultServerResponse ssc = knownHost.processEvent(/*remoteAddr, */requestCommand);
						if (ssc == null) answerCommands.add( new WrappedServerCommandResponse( false, null, wrappedServerCommandRequest.getEntryNumber()));
						else{
							
							boolean encrypted;
							byte[] data;
							if (knownHost.getForeignPublicKey() == null) // no encryption possible
							{
								encrypted = false;
								data = ssc.toByteArray();
							}
							else
							{
								encrypted = true;
								data = ssc.encrypt( knownHost.getForeignPublicKey());
							}
							if ((data != null) && (data.length > ProxyCache.MAX_REQUEST_SIZE_FOR_NORMAL_PROXYRESPONSE))
							{ // using a BigRequest fetch thread to upload the response
								logger.info("sending via DownloadBigRequestThread because response " + ssc.getClass().getSimpleName() + " is too big");
								ArrayList<WrappedServerCommandResponse> tmpAnswer = new ArrayList<WrappedServerCommandResponse>(1);
								tmpAnswer.add( new WrappedServerCommandResponse( encrypted, data, wrappedServerCommandRequest.getEntryNumber()));
								DownloadBigRequestThread downloadBigRequestThread = new DownloadBigRequestThread( tmpAnswer);
								downloadBigRequestThread.start();
							}
							else answerCommands.add( new WrappedServerCommandResponse( encrypted, data, wrappedServerCommandRequest.getEntryNumber()));
						}
					}
				} catch (Exception e) {
					logger.error("error processing command", e);
					answerCommands.add( new WrappedServerCommandResponse( false, null, wrappedServerCommandRequest.getEntryNumber())); 
				}
				
			
				
			}
			return answerCommands;
		}
		return null;
	}

	@Override
	public void run() {
		try {
			if (!proxyHost.startSecureConnection()) throw new Exception("could not start secure connection with " + proxyHost.getUniqueHostId());
			
			ArrayList<WrappedServerCommandResponse> answerCommands = null;
			
			while( !isInterrupted())
			{
				ProxyResponse proxyResponse = (ProxyResponse) proxyHost.sendCommand( new ProxyRequest( answerCommands, false, contractId));
				if (proxyResponse == null) throw new Exception("no answer");
				if (!proxyResponse.accepted) throw new Exception("not accepted");
				if (!accepted) {
					accepted = true;
					contractId = proxyResponse.getContractId();
					Backend.getConnectionStatus().onConnectionStatusChanged();					
				}
				
				if (proxyResponse.isHasABigRequestPending()){ // one big download is waiting, start another thread, to not block message processing
					DownloadBigRequestThread downloadBigRequestThread = new DownloadBigRequestThread( null);
					downloadBigRequestThread.start();
				}
				
				answerCommands = processRecievedCommands( proxyResponse);
			}
		} catch (Exception e) { // kaputt
			e.printStackTrace();
		}
		Backend.getConnectionStatus().onProxyConnectionThreadFinished( this);
	}

}
