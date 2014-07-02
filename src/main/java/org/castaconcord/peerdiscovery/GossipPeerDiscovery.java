package org.castaconcord.peerdiscovery;
//package bazarro.peerdiscovery;
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.Callable;
//import java.util.concurrent.CompletionService;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ExecutorCompletionService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.LinkedBlockingQueue;
//
//import bazarro.BazarroApplicationIdentifier;
//import bazarro.BazarroApplication;
//import bazarro.BazarroNetworkServer;
//import bazarro.BazarroNodeIdentifier;
//import bazarro.BazarroReceivedMessage;
//import bazarro.concurency.Awaitable;
//
//public class GossipPeerDiscovery extends BazarroApplication {
//	static final BazarroApplicationIdentifier GOSSIP_PEER_DISCOVERY_APP_ID=null;
//	BazarroNetworkServer network;
//	LinkedBlockingQueue<BazarroReceivedMessage> gossipResponses;
//
//	public GossipPeerDiscovery(BazarroNetworkServer network) {
//		network.registerApplication(this);
//	}
//	
//	public void getMorePeers() {
//		//If we need to discover more peers
//		if(weHaveEnoughKnownPeers()){
//			continue;
//		}
//
//		/*Two models:
//		 * 1- send query messages to the network, and check the LinkedBlockingQueue for interesting responses
//		 *    - Counter intuitive?
//		 *    - Maybe have a separate callback that is called for each response? Need an attachment object
//		 *    - Selector model ?
//		 *    
//		 * 2- send query messages, get futures we can wait on to get the response
//		 * 	  - Only one pending query per destination
//		 *    - 
//		 */
//		
//		for(int i=0; i<10; i++){
//			BazarroNodeIdentifier randomlyChosenPeer=network.getRandomNodeIdentifierFromConnectedNodes();
//			ByteBuffer getMorePeerInfoRequestBytes; //From protobuf..
//			Awaitable futureResponse=sendByteBuffer(getMorePeerInfoRequestBytes, GOSSIP_PEER_DISCOVERY_APP_ID, randomlyChosenPeer);
//		}
//	}
//
//	private boolean weHaveEnoughKnownPeers() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public BazarroApplicationIdentifier getApplicationIdentifier() {
//		return GOSSIP_PEER_DISCOVERY_APP_ID;
//	}
//}
