package org.peercentrum.dht;

import java.util.BitSet;

import org.peercentrum.core.Identifier;

import com.google.common.primitives.UnsignedBytes;

public class KIdentifier extends Identifier implements Comparable<KIdentifier> {
  public static final int NB_BITS=256;
  protected BitSet identifierBits;
  
  public KIdentifier(byte[] nodeIdBytes) {
    super(nodeIdBytes);
    identifierBits=BitSet.valueOf(nodeIdBytes);
  }

  /*junit*/ KIdentifier(int i) {
    this(new byte[] {0,(byte) i,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
  }

  public int getKDistance(KIdentifier other){
    BitSet distanceBS=(BitSet) identifierBits.clone();
    distanceBS.xor(other.identifierBits);
    int firstDifferentBit=distanceBS.nextSetBit(0);
    if(firstDifferentBit==-1){
      return 0;
    }
    return firstDifferentBit+1;
  }

  @Override
  public int compareTo(KIdentifier o) {
    for(int i=0; i<super.binaryValue.length; i++){
      int byteComparison=UnsignedBytes.compare(binaryValue[i], o.binaryValue[i]);
      if(byteComparison!=0){
        return byteComparison;
      }
    }
    return 0;
  }
}
