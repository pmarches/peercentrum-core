package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Range;

public class P2PBlobRangeSetTest {

  @Test
  public void testIterator() {
    P2PBlobRangeSet rangeSet = new P2PBlobRangeSet();
    rangeSet.add(Range.closed(1, 3));
    rangeSet.add(Range.open(11, 15));
    P2PBlobRangeSet.DiscreteIterator it1=rangeSet.discreteIterator();
    assertTrue(it1.hasNext());
    assertEquals(1, it1.next().intValue());
    assertTrue(it1.hasNext());
    assertEquals(2, it1.next().intValue());
    assertTrue(it1.hasNext());
    assertEquals(3, it1.next().intValue());
    assertTrue(it1.hasNext());
    assertEquals(12, it1.next().intValue());
    assertTrue(it1.hasNext());
    assertEquals(13, it1.next().intValue());
    assertTrue(it1.hasNext());
    assertEquals(14, it1.next().intValue());
    assertFalse(it1.hasNext());
  }

//  @Test
//  public void testLimitedRange(){
//    ArrayList<PB.P2PBlobRangeMsg> protobufRanges=new ArrayList<>();
//    protobufRanges.add(P2PBlobRangeMsg.newBuilder().setBegin(0).setEnd(10).build());
//    protobufRanges.add(P2PBlobRangeMsg.newBuilder().setBegin(25).setEnd(26).build());
//    protobufRanges.add(P2PBlobRangeMsg.newBuilder().setBegin(20).setEnd(100).build());
//    assertEquals("[[0‥0]]", new P2PBlobRangeSet(protobufRanges, 0).toString());
//    assertEquals("[[0‥5]]", new P2PBlobRangeSet(protobufRanges, 5).toString());
//    assertEquals("[[0‥10], [20‥20]]", new P2PBlobRangeSet(protobufRanges, 20).toString());
//    assertEquals("[[0‥10], [20‥21]]", new P2PBlobRangeSet(protobufRanges, 21).toString());
//  }

  @Test
  public void testToString(){
    P2PBlobRangeSet rangeSetOri=new P2PBlobRangeSet();
    rangeSetOri.add(Range.closed(0, 10));
    rangeSetOri.add(Range.closed(15, 20));

    P2PBlobRangeSet rangeSetFromString=new P2PBlobRangeSet(rangeSetOri.toString());
    assertEquals(rangeSetOri, rangeSetFromString);
  }

  @Test
  public void testSizeConstraint(){
    P2PBlobRangeSet range1=new P2PBlobRangeSet(0, 257);
    assertEquals(new P2PBlobRangeSet(0, 255), range1.constrainMaximumSpan(256));

    P2PBlobRangeSet range2=new P2PBlobRangeSet("[[0‥10], [20‥21], [30‥100], [300‥1000]]");
    assertEquals("[[0‥10], [20‥21], [30‥31]]", range2.constrainMaximumSpan(15).toString());
  }

  @Test
  public void testIntersection(){
    P2PBlobRangeSet range1=new P2PBlobRangeSet("[[0‥10], [20‥21], [30‥100], [300‥1000]]");
    P2PBlobRangeSet range2=new P2PBlobRangeSet("[[0‥9], [12‥12], [30‥90], [100‥1000]]");
    range1.intersectionThis(range2);
    assertEquals("[[0‥9], [30‥90], [100‥100], [300‥1000]]", range1.toString());
  }

}
