package org.castaconcord.core;

import java.net.InetSocketAddress;

public class BazarroNodeInformation {
	public BazarroNodeInformation(BazarroNodeIdentifier bazarroNodeIdentifier, InetSocketAddress nodeSocketAddress) {
		this.publicKey=bazarroNodeIdentifier;
		this.nodeSocketAddress=nodeSocketAddress;
	}

	public BazarroNodeIdentifier publicKey;
	public InetSocketAddress nodeSocketAddress;

	public long firstSeenAt;
	public long lastSeenAt;
	public BazarroApplicationIdentifier applications;
	
	//Stats to detect leechers? Looks weak.
	public long numberOfMessagesExchanged;
	public long numberOfBytesSent;
	public long numberOfBytesReceived;
}