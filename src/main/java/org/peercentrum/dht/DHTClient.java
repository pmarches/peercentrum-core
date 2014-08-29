package org.peercentrum.dht;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.NodeMetaData;
import org.peercentrum.core.PB;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DHTClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(DHTClient.class);
  
  KIdentifier localNodeId;
  KBuckets buckets;
  ArrayList<NodeMetaData> replacementCache;
  protected NetworkClient networkClient;
  protected ApplicationIdentifier dhtApplicationID;

  public DHTClient(NetworkClient networkClient, ApplicationIdentifier dhtApplicationID) {
    this.localNodeId=new KIdentifier(networkClient.getNodeIdentifier().getBytes());
    this.networkClient=networkClient;
    this.buckets=new KBuckets(localNodeId);
    buckets.populateFromNodeDatabase(networkClient.getNodeDatabase()); //FIXME replace with self lookup
    this.dhtApplicationID=dhtApplicationID;
  }

  public void receivedMessageFrom(KIdentifier remoteNodeIdentifier) {
    // TODO Update the node's statistics
  }

  public DefaultPromise<DHTSearch> searchNetwork(final KIdentifier idToSearch) {
    DHTSearch search=new DHTSearch(idToSearch);
    DefaultPromise<DHTSearch> promise = new DefaultPromise<DHTSearch>(){  
    };
    
    //TODO run this in another thread
    search.addClosestNodes(buckets.getClosestNodeTo(search.searchedKey, 3)); //Prime the search with the local nodes..
    while(search.isDone()==false){
      performOneSearchIteration(search);
    }

    promise.setSuccess(search);
    return promise;
  }

  protected void performOneSearchIteration(DHTSearch search) {
    List<KIdentifier> thisIterationOfClosest=search.getNextIterationOfClosestNodes();
    if(thisIterationOfClosest.isEmpty()){
      search.failed();
      return;
    }

    PB.DHTFindMsg.Builder findMsg=PB.DHTFindMsg.newBuilder();
    findMsg.setKeyCriteria(search.searchedKey.toByteString());
    PB.DHTTopLevelMsg.Builder topMsg=PB.DHTTopLevelMsg.newBuilder();
    topMsg.addFind(findMsg);

    //TODO Make this parallel
    for(KIdentifier node:thisIterationOfClosest){
      try (NetworkClientConnection conn=networkClient.createConnectionToPeer(new NodeIdentifier(node.getBytes()))) {
        Future<PB.DHTTopLevelMsg> found = conn.sendRequestMsg(dhtApplicationID, topMsg.build());
        PB.DHTTopLevelMsg response=found.get();
        receivedMessageFrom(node);
        
        if(response.getFoundCount()>1){
          LOGGER.warn("Multiple found not implemented yet");
        }
        PB.DHTFoundMsg foundMsg=response.getFound(0);
        if(foundMsg.hasValue()){
          search.successFullyFoundValue(foundMsg.getValue().toByteArray());
          break;
        }
        for(PB.PeerEndpointMsg closeNode: foundMsg.getClosestNodesList()){
          KIdentifier closeId=new KIdentifier(closeNode.getIdentity().toByteArray());
          if(closeNode.hasIpEndpoint()){
            PB.PeerEndpointMsg.IPEndpointMsg ipEndpointMsg=closeNode.getIpEndpoint();
            InetSocketAddress ipEndpoint=InetSocketAddress.createUnresolved(ipEndpointMsg.getIpAddress(), ipEndpointMsg.getPort());
            networkClient.getNodeDatabase().mapNodeIdToAddress(closeId.asNodeId(), ipEndpoint);
          }
          search.addClosestNodes(closeId);
        }
      } catch (Exception e) {
        LOGGER.error("exception in performOneSearchIteration while talking with node "+node, e);
      }
    }
  }
}
