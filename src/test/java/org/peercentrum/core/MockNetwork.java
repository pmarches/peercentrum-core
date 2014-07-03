package org.peercentrum.core;
//package ;
//
//import java.util.ArrayList;
//
//import zmq.Ctx;
//import zmq.ZMQ;
//
//public class MockNetwork {
//	public ArrayList<NetworkClient> allClients=new ArrayList<NetworkClient>();
//	public ArrayList<NetworkServer> allServers=new ArrayList<NetworkServer>();
//	public Ctx ctx;
//	
//	public MockNetwork(int numberOfNodes) {
//		ctx=ZMQ.zmq_init(1);
//		NodeBuilder builder= new NodeBuilder(ctx);
//
//		//TODO Build the static network configuration
//		for(int i=1; i<=numberOfNodes; i++){
//			NetworkClient clientNode = builder.build(new NodeIdentifier(String.format("BEEF%04d", i)));
//			allClients.add(clientNode);
//			NetworkServer serverNode = new NetworkServer(clientNode);
//			allServers.add(serverNode);
//			serverNode.start();
//		}
//	}
//	
//	public void shutdown() throws Exception{
//		for(NetworkClient currentNode : allClients){
//			currentNode.shutdown();
//		}
//		Thread.sleep(1000);
//
//		ctx.terminate();
//	}
//}
