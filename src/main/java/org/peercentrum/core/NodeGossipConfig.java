package org.peercentrum.core;

public class NodeGossipConfig {
	String bootstrapEndpoint;

	
	public String getBootstrapEndpoint() {
		return bootstrapEndpoint;
	}
	public void setBootstrapEndpoint(String bootstrapEndpoint) {
		this.bootstrapEndpoint = bootstrapEndpoint;
	}
	@Override
	public String toString() {
		return "NodeGossipConfig [bootstrapEndpoint=" + bootstrapEndpoint + "]";
	}
	
	
}
