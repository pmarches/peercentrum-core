package org.castaconcord.peerdiscovery;
//package .peerdiscovery;
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.Callable;
//import java.util.concurrent.CompletionService;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ExecutorCompletionService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.LinkedBlockingQueue;
//
//import .ApplicationIdentifier;
//import .Application;
//import .NetworkServer;
//import .NodeIdentifier;
//import .ReceivedMessage;
//import .concurency.Awaitable;
//
//public class GossipPeerDiscovery extends Application {
//	static final ApplicationIdentifier GOSSIP_PEER_DISCOVERY_APP_ID=null;
//	NetworkServer network;
//	LinkedBlockingQueue<ReceivedMessage> gossipResponses;
//
//	public GossipPeerDiscovery(NetworkServer network) {
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
//			NodeIdentifier randomlyChosenPeer=network.getRandomNodeIdentifierFromConnectedNodes();
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
//	public ApplicationIdentifier getApplicationIdentifier() {
//		return GOSSIP_PEER_DISCOVERY_APP_ID;
//	}
//}
