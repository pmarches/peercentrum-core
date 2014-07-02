package org.castaconcord.network;

import io.netty.buffer.ByteBuf;

import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.BazarroHeaderMessage;

public class HeaderAndPayload {
	public HeaderAndPayload(BazarroHeaderMessage.Builder header, ByteBuf payload) {
		header.setApplicationSpecificBlockLength(payload.readableBytes());
		this.header=header.build();
		this.payload=payload;
	}
	
	public HeaderAndPayload(BazarroHeaderMessage header, ByteBuf applicationBlock) {
		this.header=header;
		this.payload=applicationBlock;
	}

	public ProtocolBuffer.BazarroHeaderMessage header;
	public ByteBuf payload;
}
