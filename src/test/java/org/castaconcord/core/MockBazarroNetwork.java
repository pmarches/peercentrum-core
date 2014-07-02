package org.castaconcord.core;
//package bazarro;
//
//import java.util.ArrayList;
//
//import zmq.Ctx;
//import zmq.ZMQ;
//
//public class MockBazarroNetwork {
//	public ArrayList<BazarroNetworkClient> allClients=new ArrayList<BazarroNetworkClient>();
//	public ArrayList<BazarroNetworkServer> allServers=new ArrayList<BazarroNetworkServer>();
//	public Ctx ctx;
//	
//	public MockBazarroNetwork(int numberOfNodes) {
//		ctx=ZMQ.zmq_init(1);
//		BazarroNodeBuilder builder= new BazarroNodeBuilder(ctx);
//
//		//TODO Build the static network configuration
//		for(int i=1; i<=numberOfNodes; i++){
//			BazarroNetworkClient clientNode = builder.build(new BazarroNodeIdentifier(String.format("BEEF%04d", i)));
//			allClients.add(clientNode);
//			BazarroNetworkServer serverNode = new BazarroNetworkServer(clientNode);
//			allServers.add(serverNode);
//			serverNode.start();
//		}
//	}
//	
//	public void shutdown() throws Exception{
//		for(BazarroNetworkClient currentNode : allClients){
//			currentNode.shutdown();
//		}
//		Thread.sleep(1000);
//
//		ctx.terminate();
//	}
//}
