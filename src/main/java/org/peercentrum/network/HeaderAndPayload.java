package org.peercentrum.network;

import io.netty.buffer.ByteBuf;

import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.HeaderMessage;

public class HeaderAndPayload {
	public HeaderAndPayload(HeaderMessage.Builder header, ByteBuf payload) {
		header.setApplicationSpecificBlockLength(payload.readableBytes());
		this.header=header.build();
		this.payload=payload;
	}
	
	public HeaderAndPayload(HeaderMessage header, ByteBuf applicationBlock) {
		this.header=header;
		this.payload=applicationBlock;
	}

	public ProtocolBuffer.HeaderMessage header;
	public ByteBuf payload;
}
