package org.peercentrum.dht;

import java.util.ArrayList;
import java.util.List;

import org.peercentrum.core.NodeMetaData;
import org.peercentrum.network.NetworkClient;

public class DHTClient {
  KIdentifier localNodeId;
  KBuckets buckets;
  ArrayList<NodeMetaData> replacementCache;
  protected NetworkClient networkClient;

  public DHTClient(NetworkClient networkClient) {
    this.localNodeId=new KIdentifier(networkClient.getNodeIdentifier().getBytes());
    this.networkClient=networkClient;
    this.buckets=new KBuckets(localNodeId);
  }

  public void receivedMessageFrom(KIdentifier remoteNodeIdentifier) {
    // TODO Auto-generated method stub
    
  }

  public List<KIdentifier> getClosestNodeTo(final KIdentifier idToSearch, int numberOfNodesRequested) {
    KBucket oneBucket = buckets.getBucketForIdentifier(idToSearch);
    List<KIdentifier> closest=oneBucket.getClosest(idToSearch, numberOfNodesRequested);
    return closest;
  }
}
