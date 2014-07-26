package org.peercentrum.network;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.Future;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

public class NetworkClient implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkClient.class);
	
	//TODO maybe use a good caching library for this cache?
	//FIXME This cache will work only for a small number of nodes! 
	protected HashMap<NodeIdentifier, NetworkClientConnection> connectionCache=new HashMap<>();
	protected NodeIdentifier thisNodeId;
	protected NodeDatabase nodeDatabase;
	protected int localListeningPort=0;
	
  public NetworkClient(NodeIdentifier thisNodeId, NodeDatabase nodeDatabase) {
		this.thisNodeId=thisNodeId;
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
	
	public NetworkClientConnection createConnectionToPeer(NodeIdentifier remoteId){
		InetSocketAddress remoteEndpoint=nodeDatabase.getEndpointByIdentifier(remoteId);
		if(remoteEndpoint==null){
			throw new RuntimeException("No endpoint found for peer "+remoteId);
		}
		NetworkClientConnection newConnection = new NetworkClientConnection(thisNodeId, remoteEndpoint, localListeningPort);
		return newConnection;
	}

	public NetworkClientConnection maybeOpenConnectionToPeer(NodeIdentifier remoteId) {
		if(remoteId.equals(thisNodeId)){
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
	public <T extends MessageLite> Future<T> sendRequest(NodeIdentifier peerIdToExchangeWith, ApplicationIdentifier applicationId, final T protobufMessage) {
		return (Future<T>) sendRequest(peerIdToExchangeWith, applicationId, protobufMessage, protobufMessage.getClass());
	}
	
	public <T extends MessageLite> Future<T> sendRequest(NodeIdentifier peerIdToExchangeWith, ApplicationIdentifier applicationId, final MessageLite protobufRequest, Class<T> appSpecificResponseClass) {
		if(peerIdToExchangeWith.equals(thisNodeId)){
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

  public NodeIdentifier getLocalNodeId() {
    return this.thisNodeId;
  }

  public void ping(NodeIdentifier nodeToPing) throws InterruptedException {
    NetworkClientConnection connection = maybeOpenConnectionToPeer(nodeToPing);
    Future<ByteBuf> pingResponseFuture = connection.sendRequestBytes(NetworkApplication.NETWORK_APPID, NetworkApplication.pingMessageBytes);
    pingResponseFuture.await(); //TODO add timeout
  }

  public void setLocalListeniongPort(int listeningPort) {
    this.localListeningPort=listeningPort;
  }
}
