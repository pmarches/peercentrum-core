package org.peercentrum.network;

import io.netty.buffer.ByteBuf;

import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMsg;

public class HeaderAndPayload {
	public HeaderAndPayload(HeaderMsg.Builder header, ByteBuf payload) {
		header.setApplicationSpecificBlockLength(payload.readableBytes());
		this.header=header.build();
		this.payload=payload;
	}
	
	public HeaderAndPayload(HeaderMsg header, ByteBuf applicationBlock) {
		this.header=header;
		this.payload=applicationBlock;
	}

	public PB.HeaderMsg header;
	public ByteBuf payload;
}
