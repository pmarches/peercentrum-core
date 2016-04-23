package org.peercentrum.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.core.nodegossip.NodeGossipConfig;

public class TopLevelConfigTest {

	@Test
	public void test() throws Exception {
		TopLevelConfig config = TopLevelConfig.loadFromFile(new File("peercentrum-config.yaml"));
		assertNotNull(config);
		NodeGossipConfig gossipConfig=(NodeGossipConfig) config.getAppConfig(NodeGossipConfig.class);
		assertNotNull(gossipConfig);
		assertEquals("66.172.33.39:35460", gossipConfig.getBootstrapEndpoint());
		assertEquals(new File(".").getCanonicalFile(), config.getBaseDirectory());
		
		P2PBlobConfig blobConfig=(P2PBlobConfig) config.getAppConfig(P2PBlobConfig.class);
		blobConfig.getPriceLimits();
	}

}
