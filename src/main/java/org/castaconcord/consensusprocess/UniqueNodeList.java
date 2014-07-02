package org.castaconcord.consensusprocess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.castaconcord.core.BazarroNodeIdentifier;

public class UniqueNodeList implements Iterable<BazarroNodeIdentifier> {
	ArrayList<BazarroNodeIdentifier> unl = new ArrayList<BazarroNodeIdentifier>();
	
	public boolean isNodeInList(BazarroNodeIdentifier possibleNode){
		return unl.contains(possibleNode);
	}

	public void addValidatorNode(BazarroNodeIdentifier node) {
		unl.add(node);
	}

	public int size() {
		return unl.size();
	}

	@Override
	public Iterator<BazarroNodeIdentifier> iterator() {
		return unl.iterator();
	}

	public UniqueNodeList randomSliceWithout(int nbNodes, BazarroNodeIdentifier BazarroNodeIdentifierToExclude) {
		if(nbNodes>=unl.size()){
			throw new RuntimeException(nbNodes+" nodes cannot be sliced from "+unl.size()+" nodes");
		}
		Random rnd = new Random();
		UniqueNodeList slice = new UniqueNodeList();
		for(int i=0; i<nbNodes; i++){
			BazarroNodeIdentifier candidate = unl.get(rnd.nextInt(unl.size()));
			if(candidate.equals(BazarroNodeIdentifierToExclude) || slice.isNodeInList(candidate)){
				i--;
			}
			else{
				slice.addValidatorNode(candidate);
			}
		}
		return slice;
	}

	public BazarroNodeIdentifier getOneExcluding(BazarroNodeIdentifier excludedNodeId) {
		for(BazarroNodeIdentifier id : this){
			if(id.equals(excludedNodeId)==false){
				return id;
			}
		}
		throw new RuntimeException("The UNL list is either empty or only contains "+excludedNodeId);
	}
}
