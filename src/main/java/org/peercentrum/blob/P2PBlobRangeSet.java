package org.peercentrum.blob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.peercentrum.core.ProtocolBuffer;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class P2PBlobRangeSet {
	RangeSet<Long> ranges=TreeRangeSet.create();
	
	public P2PBlobRangeSet() {
	}
	
	public P2PBlobRangeSet(List<ProtocolBuffer.P2PBlobRange> blobRangesList, long maximumRangePossible) {
		for(ProtocolBuffer.P2PBlobRange protobufRange : blobRangesList){
			ranges.add(Range.closed(Long.valueOf(protobufRange.getBegin()), Long.valueOf(protobufRange.getEnd())));
		}
		this.ranges.remove(Range.greaterThan(Long.valueOf(maximumRangePossible)));
	}

	public P2PBlobRangeSet(long low, long high) {
		ranges.add(Range.<Long>closed(Long.valueOf(low), Long.valueOf(high)));
	}

	public P2PBlobRangeSet(String rangeSetStr) {
		final Splitter rangeSpliter = Splitter.on(CharMatcher.anyOf(",[]‥∞"))
	       .trimResults()
	       .omitEmptyStrings();
		
		Iterator<String> it = rangeSpliter.split(rangeSetStr).iterator();
		while(it.hasNext()){
			String lowStr=it.next();
			String highStr=it.next();
			ranges.add(Range.<Long>closed(Long.valueOf(lowStr), Long.valueOf(highStr)));
		}
	}

	public P2PBlobRangeSet(RangeSet<Long> ranges) {
		this.ranges=ranges;
	}

	public List<ProtocolBuffer.P2PBlobRange> toP2PBlobRangeMsgList() {
		if(ranges.contains(Long.MAX_VALUE)){
			return Collections.emptyList();
		}
		List<ProtocolBuffer.P2PBlobRange> listOfRanges=new ArrayList<>();
		for(Range<Long> currentRange: ranges.asRanges()){
			ProtocolBuffer.P2PBlobRange.Builder rangeMsg=ProtocolBuffer.P2PBlobRange.newBuilder();
			rangeMsg.setBegin(currentRange.lowerEndpoint());
			rangeMsg.setEnd(currentRange.upperEndpoint());
			listOfRanges.add(rangeMsg.build());
		}
		return listOfRanges;
	}

	public class DiscreteIterator implements Iterator<Long>{
		Iterator<Range<Long>> rangeIterator;
		Iterator<Long> contiguousIterator;
		
		public DiscreteIterator(RangeSet<Long> rangeSet) {
			this.rangeIterator=rangeSet.asRanges().iterator();
		}

		@Override
		public boolean hasNext() {
			if(contiguousIterator==null || contiguousIterator.hasNext()==false){
				if(rangeIterator.hasNext()==false){
					return false;
				}
				Range<Long> nextRange=rangeIterator.next();
				contiguousIterator=ContiguousSet.create(nextRange, DiscreteDomain.longs()).iterator();
			}
			return true;
		}

		@Override
		public Long next() {
			if(contiguousIterator==null || contiguousIterator.hasNext()==false){
				Range<Long> nextRange=rangeIterator.next();
				contiguousIterator=ContiguousSet.create(nextRange, DiscreteDomain.longs()).iterator();
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

	public void add(Range<Long> range) {
		this.ranges.add(range);
	}

	public P2PBlobRangeSet constrainMaximumSpan(long maximumSpanSize) {
	  long currentSize=0;
	  RangeSet<Long> constrainedRange=TreeRangeSet.create();
    for(Range<Long> r: ranges.asRanges()){
      long sizeOfRange=r.upperEndpoint()-r.lowerEndpoint()+1;
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
