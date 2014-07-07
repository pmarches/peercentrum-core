package org.peercentrum.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.peercentrum.core.NodeGossipConfig;
import org.peercentrum.core.TopLevelConfig;

public class TopLevelConfigTest {

	@Test
	public void test() throws Exception {
		TopLevelConfig config = TopLevelConfig.loadFromFile(new File("-config.yaml"));
		assertNotNull(config);
		System.out.println(config);
		assertEquals("Node1", config.getNodeIdentifier());
		NodeGossipConfig gossipConfig=(NodeGossipConfig) config.getAppConfig(NodeGossipConfig.class);
		assertNotNull(gossipConfig);
		assertEquals("127.0.0.1:1234", gossipConfig.getBootstrapEndpoint());
		assertEquals(new File(".").getCanonicalFile(), config.getBaseDirectory());
	}

}
