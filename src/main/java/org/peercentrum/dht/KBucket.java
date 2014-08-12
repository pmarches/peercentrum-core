package org.peercentrum.dht;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class KBucket {  
  final int K_BUCKET_SIZE=20;
  TreeSet<KIdentifier> nodes=new TreeSet<>();
  public void maybeAdd(KIdentifier newNode) {
    if(nodes.size()>=K_BUCKET_SIZE){
      //TODO Check for overflow and send to replacement cache
      throw new Error("Not implemented");
    }
    nodes.add(newNode);
  }

  public int size() {
    return nodes.size();
  }

  public List<KIdentifier> getClosest(KIdentifier idToSearch, int numberOfNodesRequested) {
    List<KIdentifier> closestMatches=new ArrayList<>(numberOfNodesRequested);
    Iterator<KIdentifier> lowerNodes = nodes.descendingSet().tailSet(idToSearch).iterator();
    Iterator<KIdentifier> higherNodes = nodes.tailSet(idToSearch, false).iterator();
    KIdentifier low=null;
    KIdentifier high=null;
    for(int i=0; i<numberOfNodesRequested; i++){
      if(low==null && lowerNodes.hasNext()){
        low=lowerNodes.next();
      }
      if(high==null && higherNodes.hasNext()){
        high=higherNodes.next();
      }
      
      if(low==null && high==null){
        break;
      }
      else if(low==null){
        closestMatches.add(high);
        high=null;
        continue;
      }
      else if(high==null){
        closestMatches.add(low);
        low=null;
        continue;
      }
      
      if(low.getKDistance(idToSearch)<=high.getKDistance(idToSearch)){
        closestMatches.add(low);
        low=null;
      }
      else{
        closestMatches.add(high);
        high=null;
      }
    }
    return closestMatches;
  }
}
