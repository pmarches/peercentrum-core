package org.peercentrum.core.nodegossip;

import java.net.InetSocketAddress;

import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.NodeIPEndpoint;
import org.peercentrum.core.PB;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.peercentrum.nodestatistics.NodeStatisticsDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.concurrent.Future;

public class NodeGossipClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipClient.class);
  int reachableListeningPort=0;
  private NetworkClient localClient;
  
  public NodeGossipClient(NetworkClient client, int reachableListeningPort) {
    this.localClient=client;
    this.reachableListeningPort=reachableListeningPort;
  }
  
  public void exchangeNodes(final NetworkClient client, NodeIdentifier peerIdToExchangeWith) throws Exception {
    PB.GossipMessage.Builder gossipReqBuilder=PB.GossipMessage.newBuilder();
    gossipReqBuilder.setRequestMorePeers(PB.GossipRequestMorePeers.getDefaultInstance());
    
    Future<PB.GossipMessage> responseFuture = client.sendRequest(peerIdToExchangeWith, NodeGossipApplication.GOSSIP_ID, gossipReqBuilder.build());
    integrateGossipResponse(responseFuture.get(), client.getNodeDatabase());
  }

  protected void integrateGossipResponse(PB.GossipMessage response, NodeStatisticsDatabase nodeDb) {
    if(response.hasReply()){
      PB.GossipReplyMorePeers gossipReply = response.getReply();
      for(PB.PeerEndpointMsg peer : gossipReply.getPeersList()){
        NodeIdentifier nodeIdentifier = new NodeIdentifier(peer.getIdentity().toByteArray());
        if(nodeIdentifier.equals(this.localClient.getNodeIdentifier())){
          continue;//Ignore echo
        }
        InetSocketAddress ipEndpoint=new InetSocketAddress(peer.getTlsEndpoint().getIpAddress(), peer.getTlsEndpoint().getPort());
        nodeDb.mapNodeIdToAddress(nodeIdentifier, ipEndpoint);
      }
    }
  }

  public void bootstrapGossiping(NodeIPEndpoint bootstrapEndpoint) throws Exception {
    if(bootstrapEndpoint!=null){
      LOGGER.info("Starting bootstrap with {}", bootstrapEndpoint);
      NetworkClientConnection newConnection = new NetworkClientConnection(localClient, bootstrapEndpoint, reachableListeningPort);
      
      PB.GossipMessage.Builder gossipReqBuilder=PB.GossipMessage.newBuilder();
      gossipReqBuilder.setRequestMorePeers(PB.GossipRequestMorePeers.getDefaultInstance());
      PB.GossipMessage response=newConnection.sendRequestMsg(NodeGossipApplication.GOSSIP_ID, gossipReqBuilder.build()).get();
      newConnection.close();

      integrateGossipResponse(response, localClient.getNodeDatabase());
      LOGGER.debug("After bootstrap we have {} peers to try.", localClient.getNodeDatabase().size());
    }
  }

}
