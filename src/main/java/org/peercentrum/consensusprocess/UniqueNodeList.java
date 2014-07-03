package org.peercentrum.consensusprocess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.peercentrum.core.NodeIdentifier;

public class UniqueNodeList implements Iterable<NodeIdentifier> {
	ArrayList<NodeIdentifier> unl = new ArrayList<NodeIdentifier>();
	
	public boolean isNodeInList(NodeIdentifier possibleNode){
		return unl.contains(possibleNode);
	}

	public void addValidatorNode(NodeIdentifier node) {
		unl.add(node);
	}

	public int size() {
		return unl.size();
	}

	@Override
	public Iterator<NodeIdentifier> iterator() {
		return unl.iterator();
	}

	public UniqueNodeList randomSliceWithout(int nbNodes, NodeIdentifier NodeIdentifierToExclude) {
		if(nbNodes>=unl.size()){
			throw new RuntimeException(nbNodes+" nodes cannot be sliced from "+unl.size()+" nodes");
		}
		Random rnd = new Random();
		UniqueNodeList slice = new UniqueNodeList();
		for(int i=0; i<nbNodes; i++){
			NodeIdentifier candidate = unl.get(rnd.nextInt(unl.size()));
			if(candidate.equals(NodeIdentifierToExclude) || slice.isNodeInList(candidate)){
				i--;
			}
			else{
				slice.addValidatorNode(candidate);
			}
		}
		return slice;
	}

	public NodeIdentifier getOneExcluding(NodeIdentifier excludedNodeId) {
		for(NodeIdentifier id : this){
			if(id.equals(excludedNodeId)==false){
				return id;
			}
		}
		throw new RuntimeException("The UNL list is either empty or only contains "+excludedNodeId);
	}
}
