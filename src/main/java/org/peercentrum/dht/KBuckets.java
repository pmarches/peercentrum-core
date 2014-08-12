package org.peercentrum.dht;

import java.util.Comparator;

import org.peercentrum.core.NodeMetaData;


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

  public KBucket getBucketForIdentifier(KIdentifier identifier) {
    int bucketNumber=localId.getKDistance(identifier);
    if(bucketNumber==0){
      throw new Error("KIdentifier "+identifier+" is same as the local id"+localId);
    }
    bucketNumber--; //Distance is one based, index is zero based
    KBucket distanceBucket=kBucketPerBitsOfDifference[bucketNumber];
    if(distanceBucket==null){
      distanceBucket=new KBucket();
      kBucketPerBitsOfDifference[bucketNumber]=distanceBucket;
    }
    return distanceBucket;
  }

  public KBucket maybeAdd(KIdentifier nodeId) throws Exception {
    KBucket destinationBucket=getBucketForIdentifier(nodeId);
    destinationBucket.maybeAdd(nodeId);
    return destinationBucket;
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
