package org.peercentrum.core;

import java.nio.ByteBuffer;

public class ReceivedMessage {
	public ApplicationIdentifier applicationId;
	public NodeIdentifier senderNode;
	public ByteBuffer dataReturned;
	public byte[] signature;
	public int requestNumber;
}
