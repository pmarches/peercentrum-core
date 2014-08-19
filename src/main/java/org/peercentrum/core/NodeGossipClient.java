package org.peercentrum.core;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeGossipClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipClient.class);
  int reachableListeningPort=0;
  private NetworkClient client;
  
  public NodeGossipClient(NetworkClient client) {
    this.client=client;
  }

  public int getListeningPort(){
    return reachableListeningPort;
  }
  
  public void exchangeNodes(final NetworkClient client, NodeIdentifier peerIdToExchangeWith) throws Exception {
    PB.GossipMessage.Builder gossipReqBuilder=PB.GossipMessage.newBuilder();
    gossipReqBuilder.setRequestMorePeers(PB.GossipRequestMorePeers.getDefaultInstance());
    
    Future<PB.GossipMessage> responseFuture = client.sendRequest(peerIdToExchangeWith, NodeGossipApplication.GOSSIP_ID, gossipReqBuilder.build());
    integrateGossipResponse(responseFuture.get(), client.getNodeDatabase());
  }

  protected void integrateGossipResponse(PB.GossipMessage response, NodeDatabase nodeDb) {
    if(response.hasReply()){
      PB.GossipReplyMorePeers gossipReply = response.getReply();
      for(PB.PeerEndpointMsg peer : gossipReply.getPeersList()){
        NodeIdentifier nodeIdentifier = new NodeIdentifier(peer.getIdentity().toByteArray());
        if(nodeIdentifier.equals(this.client.getNodeIdentifier())){
          continue;//Ignore echo
        }
        InetSocketAddress ipEndpoint=new InetSocketAddress(peer.getIpEndpoint().getIpAddress(), peer.getIpEndpoint().getPort());
        nodeDb.mapNodeIdToAddress(nodeIdentifier, ipEndpoint);
      }
    }
  }

  public void bootstrapGossiping(String bootstrapEndpoint) throws Exception {
    if(bootstrapEndpoint!=null){
      LOGGER.info("Starting bootstrap with {}", bootstrapEndpoint);
      String[] nodeIDAddressAndPort=bootstrapEndpoint.split(":");
      if(nodeIDAddressAndPort==null || nodeIDAddressAndPort.length!=3){
        LOGGER.error("bootstrap entry has not been recognized {}", bootstrapEndpoint);
        return;
      }
      
      NodeIdentifier bootStrapNodeId=new NodeIdentifier(nodeIDAddressAndPort[0]);
      InetSocketAddress bootstrapAddress=new InetSocketAddress(nodeIDAddressAndPort[1], Integer.parseInt(nodeIDAddressAndPort[2]));
      LOGGER.debug("bootstrap is {}", bootstrapAddress);

      NetworkClientConnection newConnection = new NetworkClientConnection(client, bootStrapNodeId, bootstrapAddress, reachableListeningPort);
      
      PB.GossipMessage.Builder gossipReqBuilder=PB.GossipMessage.newBuilder();
      gossipReqBuilder.setRequestMorePeers(PB.GossipRequestMorePeers.getDefaultInstance());
      PB.GossipMessage response=newConnection.sendRequestMsg(NodeGossipApplication.GOSSIP_ID, gossipReqBuilder.build()).get();
      newConnection.close();

      integrateGossipResponse(response, client.getNodeDatabase());
      LOGGER.debug("After bootstrap we have {} peers to try.", client.getNodeDatabase().size());
    }
  }

}
