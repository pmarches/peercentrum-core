package org.peercentrum.blob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.peercentrum.core.PB;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class P2PBlobRangeSet {
  RangeSet<Integer> ranges=TreeRangeSet.create();

  public P2PBlobRangeSet() {
  }

  public P2PBlobRangeSet(List<PB.P2PBlobRangeMsg> blobRangesList) {
    for(PB.P2PBlobRangeMsg protobufRange : blobRangesList){
      ranges.add(Range.closed(protobufRange.getBegin(), protobufRange.getEnd()));
    }
  }

  public P2PBlobRangeSet(int low, int high) {
    ranges.add(Range.<Integer>closed(low, high));
  }

  public P2PBlobRangeSet(String rangeSetStr) {
    final Splitter rangeSpliter = Splitter.on(CharMatcher.anyOf(",[]‥∞"))
        .trimResults()
        .omitEmptyStrings();

    Iterator<String> it = rangeSpliter.split(rangeSetStr).iterator();
    while(it.hasNext()){
      String lowStr=it.next();
      String highStr=it.next();
      ranges.add(Range.<Integer>closed(Integer.parseInt(lowStr), Integer.parseInt(highStr)));
    }
  }

  public P2PBlobRangeSet(RangeSet<Integer> ranges) {
    this.ranges=ranges;
  }

  public List<PB.P2PBlobRangeMsg> toP2PBlobRangeMsgList() {
    if(ranges.contains(Integer.MAX_VALUE)){
      return Collections.emptyList();
    }
    List<PB.P2PBlobRangeMsg> listOfRanges=new ArrayList<>();
    for(Range<Integer> currentRange: ranges.asRanges()){
      PB.P2PBlobRangeMsg.Builder rangeMsg=PB.P2PBlobRangeMsg.newBuilder();
      rangeMsg.setBegin(currentRange.lowerEndpoint());
      rangeMsg.setEnd(currentRange.upperEndpoint());
      listOfRanges.add(rangeMsg.build());
    }
    return listOfRanges;
  }

  public class DiscreteIterator implements Iterator<Integer>{
    Iterator<Range<Integer>> rangeIterator;
    Iterator<Integer> contiguousIterator;

    public DiscreteIterator(RangeSet<Integer> rangeSet) {
      this.rangeIterator=rangeSet.asRanges().iterator();
    }

    @Override
    public boolean hasNext() {
      if(contiguousIterator==null || contiguousIterator.hasNext()==false){
        if(rangeIterator.hasNext()==false){
          return false;
        }
        Range<Integer> nextRange=rangeIterator.next();
        contiguousIterator=ContiguousSet.create(nextRange, DiscreteDomain.integers()).iterator();
      }
      return true;
    }

    @Override
    public Integer next() {
      if(contiguousIterator==null || contiguousIterator.hasNext()==false){
        Range<Integer> nextRange=rangeIterator.next();
        contiguousIterator=ContiguousSet.create(nextRange, DiscreteDomain.integers()).iterator();
      }
      return contiguousIterator.next();
    }

    @Override
    public void remove() {
      throw new RuntimeException("remove() Not supported");
    }

  }

  public DiscreteIterator discreteIterator() {
    return new DiscreteIterator(ranges);
  }

  @Override
  public String toString() {
    return ranges.toString();
  }

  public void add(Range<Integer> range) {
    this.ranges.add(range);
  }

  public P2PBlobRangeSet constrainMaximumSpan(int maximumSpanSize) {
    int currentSize=0;
    RangeSet<Integer> constrainedRange=TreeRangeSet.create();
    for(Range<Integer> r: ranges.asRanges()){
      int sizeOfRange=r.upperEndpoint()-r.lowerEndpoint()+1;
      if(currentSize+sizeOfRange<=maximumSpanSize){
        currentSize+=sizeOfRange;
        constrainedRange.add(r);
      }
      else{
        sizeOfRange=maximumSpanSize-currentSize-1;
        constrainedRange.add(Range.closed(r.lowerEndpoint(), r.lowerEndpoint()+sizeOfRange));
        break;
      }
    }
    return new P2PBlobRangeSet(constrainedRange);
  }

  public void intersectionThis(P2PBlobRangeSet otherRangeSet) {
    RangeSet<Integer> intersection=TreeRangeSet.create();
    for(Range<Integer> otherRange: otherRangeSet.ranges.asRanges()){
      intersection.addAll(ranges.subRangeSet(otherRange));
    }
    ranges=intersection;
  }

  public P2PBlobRangeSet minus(P2PBlobRangeSet otherRange) {
    P2PBlobRangeSet result=new P2PBlobRangeSet(ranges);
    result.ranges.removeAll(otherRange.ranges);
    return result;
  }

  public int getCardinality() {
    int cardinality=0;
    for(Range<Integer> r: ranges.asRanges()){
      cardinality+=r.upperEndpoint()-r.lowerEndpoint()+1;
    }
    return cardinality;
  }
  
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ranges == null) ? 0 : ranges.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    P2PBlobRangeSet other = (P2PBlobRangeSet) obj;
    if (ranges == null) {
      if (other.ranges != null)
        return false;
    } else if (!ranges.equals(other.ranges))
      return false;
    return true;
  }
}
