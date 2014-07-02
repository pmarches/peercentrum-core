package org.castaconcord.network;

import io.netty.util.concurrent.Future;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.castaconcord.core.BazarroApplicationIdentifier;
import org.castaconcord.core.BazarroNodeDatabase;
import org.castaconcord.core.BazarroNodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

public class BazarroNetworkClient extends BazarroNetworkClientOrServer implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(BazarroNetworkClient.class);
	
	//TODO maybe use a good caching library for this cache?
	//FIXME This cache will work only for a small number of nodes! 
	protected HashMap<BazarroNodeIdentifier, BazarroNetworkClientConnection> connectionCache=new HashMap<>();
	public BazarroNetworkClient(BazarroNodeIdentifier thisNodeId, BazarroNodeDatabase nodeDatabase) {
		super(thisNodeId, nodeDatabase);
	}

	@Override
	public void close() {
		synchronized (connectionCache) {
			for(BazarroNetworkClientConnection conn : connectionCache.values()){
				conn.close();
			}
			connectionCache.clear();
		}
	}
	
	public BazarroNetworkClientConnection createConnectionToPeer(BazarroNodeIdentifier remoteId){
		InetSocketAddress remoteEndpoint=nodeDatabase.getEndpointByIdentifier(remoteId);
		if(remoteEndpoint==null){
			throw new RuntimeException("No endpoint found for peer "+remoteId);
		}
		BazarroNetworkClientConnection newConnection = new BazarroNetworkClientConnection(remoteEndpoint);
		return newConnection;
	}

	public BazarroNetworkClientConnection maybeOpenConnectionToPeer(BazarroNodeIdentifier remoteId) {
		if(remoteId.equals(thisNodeId)){
			return null;
		}
		synchronized(connectionCache){
			BazarroNetworkClientConnection connectionToPeer=connectionCache.get(remoteId);
			if(connectionToPeer==null){
				connectionToPeer = createConnectionToPeer(remoteId);
				if(connectionToPeer==null){
					return null; //FIXME this is ugly
				}
				connectionCache.put(remoteId, connectionToPeer);
			}
			return connectionToPeer;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends MessageLite> Future<T> sendRequest(BazarroNodeIdentifier peerIdToExchangeWith, BazarroApplicationIdentifier applicationId, final T protobufMessage) {
		return (Future<T>) sendRequest(peerIdToExchangeWith, applicationId, protobufMessage, protobufMessage.getClass());
	}
	
	public <T extends MessageLite> Future<T> sendRequest(BazarroNodeIdentifier peerIdToExchangeWith, BazarroApplicationIdentifier applicationId, final MessageLite protobufRequest, Class<T> appSpecificResponseClass) {
		if(peerIdToExchangeWith.equals(thisNodeId)){
			LOGGER.warn("Trying to send network request to our own node?");
			return null;
		}
		BazarroNetworkClientConnection connection = maybeOpenConnectionToPeer(peerIdToExchangeWith);
		if(connection==null){
			return null;
		}
		Future<T> responseFuture = connection.sendRequestMsg(applicationId, protobufRequest, appSpecificResponseClass);
		return responseFuture;
	}	

}
