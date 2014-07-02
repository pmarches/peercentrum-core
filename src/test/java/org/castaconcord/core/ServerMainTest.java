package org.castaconcord.core;

import java.io.File;

import org.junit.Test;

public class ServerMainTest {

	@Test
	public void test() throws Exception {
		Config configNode1=Config.loadFromFile(new File("-config.yaml"));
		configNode1.setNodeIdentifier("Node1");
		configNode1.setListenPort(0);
		configNode1.setEnableNAT(false);
		ServerMain node1 = new ServerMain(configNode1);
		NodeGossipConfig node1Gossipconfig=(NodeGossipConfig) configNode1.getAppConfig(NodeGossipConfig.class);
		node1Gossipconfig.setBootstrapEndpoint(null);
		node1.run();

		Config configNode2=Config.loadFromFile(new File("-config.yaml"));
		configNode2.setNodeIdentifier("Node2");
		configNode2.setListenPort(0);
		configNode2.setEnableNAT(false);
		NodeGossipConfig gossipConfigNode2=(NodeGossipConfig) configNode2.getAppConfig(NodeGossipConfig.class);
		gossipConfigNode2.setBootstrapEndpoint("localhost:"+node1.server.getListeningPort());
		ServerMain node2 = new ServerMain(configNode2);
		node2.run();
	}

}
