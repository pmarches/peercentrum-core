package org.peercentrum.consensusprocess;

import org.peercentrum.core.NodeIdentifier;

import com.google.common.primitives.UnsignedLong;

public class BaseTestCase {
	public static NodeIdentifier nodeA;
	public static NodeIdentifier nodeB;
	public static NodeIdentifier nodeC;
	public static NodeIdentifier nodeD;
	public static NodeIdentifier nodeE;
	public static NodeIdentifier nodeZ;

	public static UniqueNodeList unl;
	public static ConsensusTransaction tx1;
	public static ConsensusTransaction tx2;
	public static ConsensusTransaction tx3;
	public static ConsensusTransaction tx4;
	public static ConsensusTransaction tx5;
	public static ConsensusTransaction tx6;
	public static ConsensusTransaction tx6_clone;
	public static MockTransaction tx7_invalid;

	public static ProposedTransactions<ConsensusTransaction> nodeAProposal_1_3;
	public static ProposedTransactions<ConsensusTransaction> nodeBProposal_2;
	public static ProposedTransactions<ConsensusTransaction> nodeBProposal_1_2_3;
	public static ProposedTransactions<ConsensusTransaction> nodeCProposal_1_3;
	public static ProposedTransactions<ConsensusTransaction> nodeDProposal_1_3;
	public static ProposedTransactions<ConsensusTransaction> nodeZProposal_1_2_3;

	static{
		nodeA = new NodeIdentifier("nodeA");
		nodeB = new NodeIdentifier("nodeB");
		nodeC = new NodeIdentifier("nodeC");
		nodeD = new NodeIdentifier("nodeC");
		nodeZ = new NodeIdentifier("nodeZ");
		
		unl = new UniqueNodeList();
		unl.addValidatorNode(nodeA);
		unl.addValidatorNode(nodeB);
		unl.addValidatorNode(nodeC);
		unl.addValidatorNode(nodeD);
		
		tx1=new MockTransaction(true, 1);
		tx2=new MockTransaction(true, 2);
		tx3=new MockTransaction(true, 3);
		tx4=new MockTransaction(true, 4);
		tx5=new MockTransaction(true, 5);
		tx6=new MockTransaction(true, 6);
		tx6_clone=new MockTransaction(true, 6);
		tx7_invalid=new MockTransaction(false, 7);
		
		nodeAProposal_1_3 = new ProposedTransactions<>(UnsignedLong.ONE, nodeA);
		nodeAProposal_1_3.addTransactions(tx1);
		nodeAProposal_1_3.addTransactions(tx3);

		nodeBProposal_2 = new ProposedTransactions<>(UnsignedLong.ONE, nodeB);
		nodeBProposal_2.addTransactions(tx2);

		nodeBProposal_1_2_3 = new ProposedTransactions<>(UnsignedLong.ONE, nodeB);
		nodeBProposal_1_2_3.addTransactions(tx1);
		nodeBProposal_1_2_3.addTransactions(tx2);
		nodeBProposal_1_2_3.addTransactions(tx3);

		nodeCProposal_1_3 = new ProposedTransactions<>(UnsignedLong.ONE, nodeC);
		nodeCProposal_1_3.addTransactions(tx1);
		nodeCProposal_1_3.addTransactions(tx3);

		nodeDProposal_1_3 = new ProposedTransactions<>(UnsignedLong.ONE, nodeD);
		nodeDProposal_1_3.addTransactions(tx1);
		nodeDProposal_1_3.addTransactions(tx3);

		nodeZProposal_1_2_3 = new ProposedTransactions<>(UnsignedLong.ONE, nodeZ);
		nodeZProposal_1_2_3.addTransactions(tx1);
		nodeZProposal_1_2_3.addTransactions(tx2);
		nodeZProposal_1_2_3.addTransactions(tx3);
	}

}
