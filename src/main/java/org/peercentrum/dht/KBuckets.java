package org.peercentrum.dht;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.peercentrum.core.NodeMetaData;
import org.peercentrum.nodestatistics.NodeStatisticsDatabase;


public class KBuckets {
  KIdentifier localId;
  KBucket[] kBucketPerBitsOfDifference=new KBucket[KIdentifier.NB_BITS];
  
  public KBuckets(KIdentifier localId) {
    this.localId=localId;
  }

  private static final Comparator<NodeMetaData> leastRecentlySeenFirstComparator=new Comparator<NodeMetaData>(){
    @Override
    public int compare(NodeMetaData o1, NodeMetaData o2) {
      return 0;
    }
  };

  public List<KIdentifier> getClosestNodeTo(final KIdentifier idToSearch, int numberOfNodesRequested) {
    KBucket bestBucket = getBucketForIdentifier(idToSearch);
    KBucket candidates=new KBucket();
    candidates.maybeAddAll(bestBucket.getClosest(idToSearch, numberOfNodesRequested));
    int nbMissing=numberOfNodesRequested-candidates.size();
    if(nbMissing>0){
      //Search the other buckets
      for(KBucket bucket : this.kBucketPerBitsOfDifference){
        if(bucket==null || bucket==bestBucket){
          continue;
        }
        candidates.maybeAddAll(bucket.getClosest(idToSearch, nbMissing));
      }
    }
    return candidates.getClosest(idToSearch, numberOfNodesRequested);
  }

  public KBucket getBucketForIdentifier(KIdentifier identifier) {
    int bucketNumber = getBucketIndexForId(identifier);
    KBucket distanceBucket=kBucketPerBitsOfDifference[bucketNumber];
    if(distanceBucket==null){
      distanceBucket=new KBucket();
      kBucketPerBitsOfDifference[bucketNumber]=distanceBucket;
    }
    return distanceBucket;
  }

  private int getBucketIndexForId(KIdentifier identifier) throws Error {
    int bucketNumber=localId.getKDistance(identifier);
    if(bucketNumber==0){
      throw new Error("KIdentifier "+identifier+" is same as the local id"+localId);
    }
    bucketNumber--; //Distance is one based, index is zero based
    return bucketNumber;
  }

  public KBucket maybeAdd(KIdentifier nodeId) {
    KBucket destinationBucket=getBucketForIdentifier(nodeId);
    destinationBucket.maybeAdd(nodeId);
    return destinationBucket;
  }

  public List<KIdentifier> getClosestMatch(int numberOfMatchesToFind) {
    ArrayList<KIdentifier> closestMatches=new ArrayList<>(numberOfMatchesToFind);
    for(KBucket currentBucket : kBucketPerBitsOfDifference){
      if(currentBucket==null){
        continue;
      }
      int numberOfNodesRequested=numberOfMatchesToFind-closestMatches.size();
      closestMatches.addAll(currentBucket.getClosest(localId, numberOfNodesRequested));
      if(closestMatches.size()>=numberOfMatchesToFind){
        break;
      }
    }
    return closestMatches;
  }

  public void populateFromNodeDatabase(NodeStatisticsDatabase nodeDatabase) {
    List<NodeMetaData> startingNodes = nodeDatabase.getAllNodeInformation(KIdentifier.NB_BITS*KBucket.K_BUCKET_SIZE);
    for(NodeMetaData node : startingNodes){
      KIdentifier idToAdd=new KIdentifier(node.nodeIdentifier.getBytes());
      if(idToAdd.equals(this.localId)==false){
        maybeAdd(idToAdd);
      }
    }
  }

  public int size() {
    int nbKeys=0;
    for(KBucket currentBucket : kBucketPerBitsOfDifference){
      if(currentBucket==null){
        continue;
      }
      nbKeys+=currentBucket.size();
    }
    return nbKeys;
  }

  //  public void getBucketForIdentifier(KIdentifier newId){
//    int bucketNumber=MOCK_LOCAL_ID.getKDistance(newId);
//    KBucket distanceBucket=kBucketPerBitsOfDifference[bucketNumber];
//    if(distanceBucket!=null){
//      distanceBucket.maybeAdd(newId);
//    }
//    else{
//      distanceBucket=new KBucket();
//      kBucketPerBitsOfDifference[bucketNumber]=distanceBucket;
//      distanceBucket.add(newId);
//    }
//  }
}
