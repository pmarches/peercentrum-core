package org.castaconcord.core;

public class NodeGossipConfig {
	String bootstrapEndpoint;
	String nodeDatabasePath;

	
	
	public String getBootstrapEndpoint() {
		return bootstrapEndpoint;
	}
	public void setBootstrapEndpoint(String bootstrapEndpoint) {
		this.bootstrapEndpoint = bootstrapEndpoint;
	}
	public String getNodeDatabasePath() {
		return nodeDatabasePath;
	}
	public void setNodeDatabasePath(String nodeDatabasePath) {
		this.nodeDatabasePath = nodeDatabasePath;
	}
	@Override
	public String toString() {
		return "NodeGossipConfig [bootstrapEndpoint=" + bootstrapEndpoint + ", nodeDatabasePath="
				+ nodeDatabasePath + "]";
	}
	
	
}
