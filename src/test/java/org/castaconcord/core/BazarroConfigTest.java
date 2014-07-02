package org.castaconcord.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

public class BazarroConfigTest {

	@Test
	public void test() throws Exception {
		BazarroConfig config = BazarroConfig.loadFromFile(new File("bazarro-config.yaml"));
		assertNotNull(config);
		System.out.println(config);
		assertEquals("Node1", config.getNodeIdentifier());
		BazarroNodeGossipConfig gossipConfig=(BazarroNodeGossipConfig) config.getAppConfig(BazarroNodeGossipConfig.class);
		assertNotNull(gossipConfig);
		assertEquals("127.0.0.1:1234", gossipConfig.getBootstrapEndpoint());
		assertEquals(new File(".").getCanonicalFile(), config.getBaseDirectory());
	}

}
