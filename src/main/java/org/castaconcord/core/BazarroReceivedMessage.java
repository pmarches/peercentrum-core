package org.castaconcord.core;

import java.nio.ByteBuffer;

public class BazarroReceivedMessage {
	public BazarroApplicationIdentifier applicationId;
	public BazarroNodeIdentifier senderNode;
	public ByteBuffer dataReturned;
	public byte[] signature;
	public int requestNumber;
}
