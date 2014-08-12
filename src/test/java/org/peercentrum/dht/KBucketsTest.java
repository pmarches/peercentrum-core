package org.peercentrum.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

public class KBucketsTest {
  private KBucket firstBucket;
  private KBucket secondBucket;
  private KBucket thirdBucket;
  KIdentifier localId=new KIdentifier(0b110);
  private KBuckets buckets;

//  @Before
//  public void setup() throws Exception{
//    buckets=new KBuckets(localId);
//    firstBucket=buckets.maybeAdd(new RemoteNode(new KIdentifier(0b000)));
//    buckets.maybeAdd(new RemoteNode(new KIdentifier(0b001)));
//    buckets.maybeAdd(new RemoteNode(new KIdentifier(0b010)));
//    
//    secondBucket=buckets.maybeAdd(new RemoteNode(new KIdentifier(0b100)));
//    buckets.maybeAdd(new RemoteNode(new KIdentifier(0b101)));
//
//    thirdBucket=buckets.maybeAdd(new RemoteNode(new KIdentifier(0b111)));
//  }
//
//  @Test
//  public void testMaybeAdd() throws Exception {
//    assertEquals(3, firstBucket.size());
//    assertEquals(2, secondBucket.size());
//    assertEquals(1, thirdBucket.size());
//    assertSame(secondBucket, buckets.getBucketForIdentifier(new KIdentifier(0b100)));
////    buckets.findClosest();
//  }
  
  @Test
  public void testDistances(){
    assertEquals(9, new KIdentifier(0b101).getKDistance(new KIdentifier(0b100)));
    assertEquals(0, new KIdentifier(0b101).getKDistance(new KIdentifier(0b101)));
    assertEquals(9, new KIdentifier(0b1).getKDistance(new KIdentifier(0b0)));
    assertEquals(11, new KIdentifier(0b100).getKDistance(new KIdentifier(0b000)));
  }
  
  @Test
  public void testGet(){
    KBucket aBigBucket=new KBucket();
    for(byte i=0; i<8; i++){
      if(i==5){
        continue;
      }
      aBigBucket.maybeAdd(new KIdentifier(i));
    }
    assertEquals("[0004000000000000000000000000000000000000000000000000000000000000, 0006000000000000000000000000000000000000000000000000000000000000, 0003000000000000000000000000000000000000000000000000000000000000, 0002000000000000000000000000000000000000000000000000000000000000]", aBigBucket.getClosest(new KIdentifier(5), 4).toString());
    assertEquals("[0001000000000000000000000000000000000000000000000000000000000000, 0000000000000000000000000000000000000000000000000000000000000000, 0002000000000000000000000000000000000000000000000000000000000000]", aBigBucket.getClosest(new KIdentifier(1), 3).toString());
  }
}
