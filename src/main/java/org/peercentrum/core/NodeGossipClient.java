package org.peercentrum.core;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeGossipClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipClient.class);
  private NodeIdentifier localNodeId;
  private NodeDatabase nodeDatabase;
  int reachableListeningPort=0;
  
  public NodeGossipClient(NodeIdentifier localNodeId, NodeDatabase nodeDatabase) {
    this.localNodeId=localNodeId;
    this.nodeDatabase=nodeDatabase;
  }

  public int getListeningPort(){
    return reachableListeningPort;
  }
  
  public void exchangeNodes(final NetworkClient client, NodeIdentifier peerIdToExchangeWith) throws InterruptedException, ExecutionException {
    ProtocolBuffer.GossipMessage.Builder gossipReqBuilder=ProtocolBuffer.GossipMessage.newBuilder();
    gossipReqBuilder.setRequestMorePeers(ProtocolBuffer.GossipRequestMorePeers.getDefaultInstance());
    
    Future<ProtocolBuffer.GossipMessage> responseFuture = client.sendRequest(peerIdToExchangeWith, NodeGossipApplication.GOSSIP_ID, gossipReqBuilder.build());
    integrateGossipResponse(responseFuture.get(), client.getNodeDatabase());
  }

  protected void integrateGossipResponse(ProtocolBuffer.GossipMessage response, NodeDatabase nodeDb) {
    if(response.hasReply()){
      ProtocolBuffer.GossipReplyMorePeers gossipReply = response.getReply();
      for(ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint peer : gossipReply.getPeersList()){
        NodeIdentifier NodeIdentifier = new NodeIdentifier(peer.getIdentity().toByteArray());
        InetSocketAddress ipEndpoint=new InetSocketAddress(peer.getIpEndpoint().getIpaddress(), peer.getIpEndpoint().getPort());
        nodeDb.mapNodeIdToAddress(NodeIdentifier, ipEndpoint);
      }
    }
  }

  public void bootstrapGossiping(String bootstrapEndpoint) throws Exception {
    if(bootstrapEndpoint!=null){
      LOGGER.info("Starting bootstrap with {}", bootstrapEndpoint);
      String[] addressAndPort=bootstrapEndpoint.split(":");
      if(addressAndPort==null || addressAndPort.length!=2){
        LOGGER.error("bootstrap entry has not been recognized {}", bootstrapEndpoint);
        return;
      }
      InetSocketAddress bootstrapAddress=new InetSocketAddress(addressAndPort[0], Integer.parseInt(addressAndPort[1]));
      LOGGER.debug("bootstrap is {}", bootstrapAddress);

      NetworkClientConnection newConnection = new NetworkClientConnection(this.localNodeId, bootstrapAddress);
      newConnection.setLocalNodeInfo(localNodeId, reachableListeningPort);
      
      ProtocolBuffer.GossipMessage.Builder gossipReqBuilder=ProtocolBuffer.GossipMessage.newBuilder();
      gossipReqBuilder.setRequestMorePeers(ProtocolBuffer.GossipRequestMorePeers.getDefaultInstance());
      ProtocolBuffer.GossipMessage response=newConnection.sendRequestMsg(NodeGossipApplication.GOSSIP_ID, gossipReqBuilder.build()).get();
      newConnection.close();

      integrateGossipResponse(response, nodeDatabase);
      LOGGER.debug("After bootstrap we have {} peers to try.", nodeDatabase.size());
    }
  }

}
