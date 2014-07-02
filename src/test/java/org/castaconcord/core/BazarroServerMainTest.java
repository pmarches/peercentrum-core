package org.castaconcord.core;

import java.io.File;

import org.junit.Test;

public class BazarroServerMainTest {

	@Test
	public void test() throws Exception {
		BazarroConfig configNode1=BazarroConfig.loadFromFile(new File("bazarro-config.yaml"));
		configNode1.setNodeIdentifier("Node1");
		configNode1.setListenPort(0);
		configNode1.setEnableNAT(false);
		BazarroServerMain node1 = new BazarroServerMain(configNode1);
		BazarroNodeGossipConfig node1Gossipconfig=(BazarroNodeGossipConfig) configNode1.getAppConfig(BazarroNodeGossipConfig.class);
		node1Gossipconfig.setBootstrapEndpoint(null);
		node1.run();

		BazarroConfig configNode2=BazarroConfig.loadFromFile(new File("bazarro-config.yaml"));
		configNode2.setNodeIdentifier("Node2");
		configNode2.setListenPort(0);
		configNode2.setEnableNAT(false);
		BazarroNodeGossipConfig gossipConfigNode2=(BazarroNodeGossipConfig) configNode2.getAppConfig(BazarroNodeGossipConfig.class);
		gossipConfigNode2.setBootstrapEndpoint("localhost:"+node1.server.getListeningPort());
		BazarroServerMain node2 = new BazarroServerMain(configNode2);
		node2.run();
	}

}
