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
	RangeSet<Integer> ranges=TreeRangeSet.create();
	
	public P2PBlobRangeSet() {
	}
	
	public P2PBlobRangeSet(List<ProtocolBuffer.P2PBlobRange> blobRangesList, int maximumRangePossible) {
		for(ProtocolBuffer.P2PBlobRange protobufRange : blobRangesList){
			ranges.add(Range.closed(protobufRange.getBegin(), protobufRange.getEnd()));
		}
		this.ranges.remove(Range.greaterThan(maximumRangePossible));
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

	public List<ProtocolBuffer.P2PBlobRange> toP2PBlobRangeMsgList() {
		if(ranges.contains(Integer.MAX_VALUE)){
			return Collections.emptyList();
		}
		List<ProtocolBuffer.P2PBlobRange> listOfRanges=new ArrayList<>();
		for(Range<Integer> currentRange: ranges.asRanges()){
			ProtocolBuffer.P2PBlobRange.Builder rangeMsg=ProtocolBuffer.P2PBlobRange.newBuilder();
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
