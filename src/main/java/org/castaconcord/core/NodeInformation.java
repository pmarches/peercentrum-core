package org.castaconcord.core;

import java.net.InetSocketAddress;

public class NodeInformation {
	public NodeInformation(NodeIdentifier NodeIdentifier, InetSocketAddress nodeSocketAddress) {
		this.publicKey=NodeIdentifier;
		this.nodeSocketAddress=nodeSocketAddress;
	}

	public NodeIdentifier publicKey;
	public InetSocketAddress nodeSocketAddress;

	public long firstSeenAt;
	public long lastSeenAt;
	public ApplicationIdentifier applications;
	
	//Stats to detect leechers? Looks weak.
	public long numberOfMessagesExchanged;
	public long numberOfBytesSent;
	public long numberOfBytesReceived;
}