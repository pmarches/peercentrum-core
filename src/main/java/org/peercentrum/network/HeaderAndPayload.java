package org.peercentrum.network;

import io.netty.buffer.ByteBuf;

import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMessage;

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

	public PB.HeaderMessage header;
	public ByteBuf payload;
}
