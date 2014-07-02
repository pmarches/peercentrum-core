package org.castaconcord.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class HeaderAndPayloadToBytesEncoder extends MessageToByteEncoder<HeaderAndPayload> {
	@Override
	protected void encode(ChannelHandlerContext ctx, HeaderAndPayload msg, ByteBuf out) throws Exception {
		ProtobufByteBufCodec.encodeWithLengthPrefix(msg.header, out);
		out.ensureWritable(msg.payload.readableBytes());
        out.writeBytes(msg.payload, msg.payload.readerIndex(), msg.payload.readableBytes());
	}

    @SuppressWarnings("deprecation")
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	cause.printStackTrace();
    	super.exceptionCaught(ctx, cause);
    }
}
