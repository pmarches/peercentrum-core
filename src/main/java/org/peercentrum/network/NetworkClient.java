package org.peercentrum.network;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.NodeIPEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import io.netty.util.concurrent.Future;

public class NetworkClient implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkClient.class);
	
	//TODO maybe use a good caching library for this cache?
	//FIXME This cache will work only for a small number of nodes! 
	protected HashMap<NodeIdentifier, NetworkClientConnection> connectionCache=new HashMap<>();
	protected NodeDatabase nodeDatabase;
	protected int localListeningPort=0;
  public boolean useEncryption=true;
  protected NodeIdentity localIdentity;
	
  public NetworkClient(NodeIdentity localIdentity, NodeDatabase nodeDatabase) throws Exception {
    this.localIdentity=localIdentity;
		this.nodeDatabase=nodeDatabase;
	}

	@Override
	public void close() {
		synchronized (connectionCache) {
			for(NetworkClientConnection conn : connectionCache.values()){
				conn.close();
			}
			connectionCache.clear();
		}
	}
	
	public NetworkClientConnection createConnectionToPeer(NodeIdentifier remoteId) throws Exception{
		InetSocketAddress remoteEndpoint=nodeDatabase.getEndpointByNodeIdentifier(remoteId);
		if(remoteEndpoint==null){
			throw new RuntimeException("No endpoint found for peer "+remoteId);
		}
		NetworkClientConnection newConnection = new NetworkClientConnection(this, new NodeIPEndpoint(remoteId, remoteEndpoint), localListeningPort);
		return newConnection;
	}

	public NetworkClientConnection maybeOpenConnectionToPeer(NodeIdentifier remoteId) throws Exception {
		if(remoteId.equals(this.localIdentity.getIdentifier())){
			return null;
		}
		synchronized(connectionCache){
			NetworkClientConnection connectionToPeer=connectionCache.get(remoteId);
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
	public <T extends MessageLite> Future<T> sendRequest(NodeIdentifier peerIdToExchangeWith, ApplicationIdentifier applicationId, final T protobufMessage) throws Exception {
		return (Future<T>) sendRequest(peerIdToExchangeWith, applicationId, protobufMessage, protobufMessage.getClass());
	}
	
	public <T extends MessageLite> Future<T> sendRequest(NodeIdentifier peerIdToExchangeWith, ApplicationIdentifier applicationId, final MessageLite protobufRequest, Class<T> appSpecificResponseClass) throws Exception {
		if(peerIdToExchangeWith.equals(this.localIdentity.getIdentifier())){
			LOGGER.warn("Trying to send network request to our own node?");
			return null;
		}
		NetworkClientConnection connection = maybeOpenConnectionToPeer(peerIdToExchangeWith);
		if(connection==null){
			return null;
		}
		Future<T> responseFuture = connection.sendRequestMsg(applicationId, protobufRequest, appSpecificResponseClass);
		return responseFuture;
	}	

	public NodeDatabase getNodeDatabase(){
	  return nodeDatabase;
	}

  public void setLocalListeningPort(int listeningPort) {
    this.localListeningPort=listeningPort;
  }

  public NodeIdentifier getNodeIdentifier() {
    return localIdentity.getIdentifier();
  }

  public int getNumberOfCachedConnections() {
    return connectionCache.size();
  }
}
