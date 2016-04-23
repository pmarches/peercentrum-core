package org.peercentrum.core;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import com.google.protobuf.Parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;

public class ProtobufByteBufCodec {
    public static void encodeWithLengthPrefix(MessageLite msg, ByteBuf out) throws Exception {
    	ByteBuf msgBytes;
    	if(msg==null){
    		msgBytes=Unpooled.EMPTY_BUFFER;
    	}
    	else{
    		msgBytes=encodeNoLengthPrefix(msg);
    	}
        int encodedMessageLen = msgBytes.readableBytes();
        int headerLenLen = CodedOutputStream.computeRawVarint32Size(encodedMessageLen);
        out.ensureWritable(headerLenLen + encodedMessageLen);

        CodedOutputStream headerOut =
                CodedOutputStream.newInstance(new ByteBufOutputStream(out), headerLenLen);
        headerOut.writeRawVarint32(encodedMessageLen);
        headerOut.flush();

        out.writeBytes(msgBytes, msgBytes.readerIndex(), encodedMessageLen);

    }
    
    public static ByteBuf encodeNoLengthPrefix(MessageLiteOrBuilder msg) {
        if (msg instanceof MessageLite) {
            return Unpooled.wrappedBuffer(((MessageLite) msg).toByteArray());
        }
        if (msg instanceof MessageLite.Builder) {
            return Unpooled.wrappedBuffer(((MessageLite.Builder) msg).build().toByteArray());
        }
        return null;
    }

	@SuppressWarnings("unchecked")
	public static <T extends MessageLite> T decodeNoLengthPrefix(ByteBuf msgBytes, Class<T> messageClass) throws Exception {
		if(msgBytes==null){
			msgBytes=Unpooled.EMPTY_BUFFER;
		}
        final byte[] array;
        final int offset;
        final int length = msgBytes.readableBytes();
        if (msgBytes.hasArray()) {
            array = msgBytes.array();
            offset = msgBytes.arrayOffset() + msgBytes.readerIndex();
        } else {
            array = new byte[length];
            msgBytes.getBytes(msgBytes.readerIndex(), array, 0, length);
            offset = 0;
        }

		com.google.protobuf.Parser<T> PARSER=(Parser<T>) messageClass.getField("PARSER").get(null);
		return PARSER.parseFrom(array, offset, length);
    }
    
	public static <T extends MessageLite> T decodeWithLengthPrefix(ByteBuf in, Class<T> messageClass) throws Exception{
        in.markReaderIndex();
        final byte[] buf = new byte[5];
        ByteBuf protoBufMessageBytes=null;
		for (int i = 0; i < buf.length; i ++) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return null;
            }

            buf[i] = in.readByte();
            if (buf[i] >= 0) {
                int length = CodedInputStream.newInstance(buf, 0, i + 1).readRawVarint32();
                if (length < 0) {
                    throw new CorruptedFrameException("negative length: " + length);
                }

                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                    return null;
                } else {
                    protoBufMessageBytes=in.readSlice(length);
                    break;
                }
            }
        }
		if(protoBufMessageBytes==null){
			return null;
		}
		return decodeNoLengthPrefix(protoBufMessageBytes, messageClass);
	}

//	public static HeaderMsg protobufBytesToMessage(ByteBuf protoBufMessageBytes) throws InvalidProtocolBufferException {
//        final byte[] array;
//        final int offset;
//        final int length = protoBufMessageBytes.readableBytes();
//        if (protoBufMessageBytes.hasArray()) {
//            array = protoBufMessageBytes.array();
//            offset = protoBufMessageBytes.arrayOffset() + protoBufMessageBytes.readerIndex();
//        } else {
//            array = new byte[length];
//            protoBufMessageBytes.getBytes(protoBufMessageBytes.readerIndex(), array, 0, length);
//            offset = 0;
//        }
//
//        return PB.HeaderMsg.newBuilder().mergeFrom(array, offset, length).build();
//	}

}