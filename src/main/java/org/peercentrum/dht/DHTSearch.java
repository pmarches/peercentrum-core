package org.peercentrum.dht;

import java.util.List;

public class DHTSearch {
  protected KIdentifier searchedKey;
  KBuckets searchBuckets;
  byte[] foundValue;
  int lastBestBucket;
  private boolean isSearchDone=false;
  public boolean foundNode=false;
  
  public DHTSearch(KIdentifier searchedId) {
    this.searchedKey=searchedId;
    searchBuckets=new KBuckets(searchedId);
  }

  public void addClosestNodes(KIdentifier nodeId) {
    if(this.searchedKey.equals(nodeId)){
      successFullyFoundNode();
      return;
    }
    searchBuckets.maybeAdd(nodeId);
  }

  public void addClosestNodes(List<KIdentifier> closestNodeTo) {
    for(KIdentifier nodeId : closestNodeTo){
      addClosestNodes(nodeId);
    }
  }

  public List<KIdentifier> getNextIterationOfClosestNodes() {
    //FIXME remove the nodes from the bucket
    //FIXME This works ok with lots of nodes, but in the test scenario we have less than KBucket.K_BUCKET_SIZE nodes, hence
    List<KIdentifier> currentBestNodes=searchBuckets.getClosestMatch(KBucket.K_BUCKET_SIZE);
    return currentBestNodes;
  }

  public void successFullyFoundNode() {
    this.isSearchDone=true;
    this.foundNode=true;
  }
  
  public void successFullyFoundValue(byte[] valueFound) {
    this.isSearchDone=true;
    this.foundValue=valueFound;
  }

  public boolean isDone() {
    return isSearchDone;
  }

  public void failed() {
    isSearchDone=true;
  }

  
  
}
