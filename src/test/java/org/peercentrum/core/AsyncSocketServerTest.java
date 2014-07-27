package org.peercentrum.core;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.peercentrum.core.PB.HeaderMsg;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkClientConnection;
import org.peercentrum.network.NetworkServer;

public class AsyncSocketServerTest {
	final int NB_CLIENTS=10;
	final int NUMBER_OF_MESSAGE = 2;

	static class MessageEchoApp extends BaseApplicationMessageHandler {
		public static final ApplicationIdentifier ECHO_APP = new ApplicationIdentifier("EchoApp".getBytes());
		CountDownLatch countdownLatch;
		public AtomicInteger numberOfMessagesReceived=new AtomicInteger();

		public MessageEchoApp(NetworkServer server, CountDownLatch serverSideLatch) {
			super(server);
			countdownLatch = serverSideLatch;
		}
		
		@Override
		public ApplicationIdentifier getApplicationId() {
			return ECHO_APP;
		}

		@Override
		public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedRequest) {
			if(countdownLatch!=null){
				countdownLatch.countDown();
			}
			numberOfMessagesReceived.incrementAndGet();
//			System.out.println("Payload is of size "+receivedRequest.payload.readableBytes());
			HeaderMsg.Builder headerBuilder = newResponseHeaderForRequest(receivedRequest);
			
			HeaderAndPayload response = new HeaderAndPayload(headerBuilder, receivedRequest.payload);
			return response;
		}
	}

	@Test
	public void testAsyncSocketServer() throws Exception {
		ResourceLeakDetector.setLevel(Level.ADVANCED);
		TopLevelConfig topConfig=new TopLevelConfig("server1");
		final NetworkServer server = new NetworkServer(topConfig);
		final CountDownLatch serverDoneBarrier = new CountDownLatch(NB_CLIENTS*NUMBER_OF_MESSAGE);
		MessageEchoApp serverSideCountingHandler=new MessageEchoApp(server, serverDoneBarrier);
		
		InetSocketAddress serverEndpoint=new InetSocketAddress(server.getListeningPort());
		final NetworkClientConnection connection = new NetworkClientConnection(null, server.getLocalNodeId(), serverEndpoint, 0);
		final CountDownLatch clientsDoneBarrier = new CountDownLatch(NB_CLIENTS);
		for(int i=0; i<NB_CLIENTS; i++){
			new Thread(){ @Override public void run() {
					try {
						doNettyClientWrite(connection);
						clientsDoneBarrier.countDown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
		clientsDoneBarrier.await();
		connection.close();
		serverDoneBarrier.await();
		
		server.stopAcceptingConnections();

		assertEquals(NB_CLIENTS*NUMBER_OF_MESSAGE, serverSideCountingHandler.numberOfMessagesReceived.intValue());
	}

	private void doNettyClientWrite(NetworkClientConnection connection) throws InterruptedException, ExecutionException {
		ByteBuf helloWorldBuffer = Unpooled.wrappedBuffer("Hello world".getBytes());
		Future<ByteBuf> helloWorldResponse=connection.sendRequestBytes(MessageEchoApp.ECHO_APP, helloWorldBuffer);

		ByteBuf bonjourBuffer = Unpooled.wrappedBuffer("Bonjour le monde".getBytes());
		Future<ByteBuf> bonjourResponse=connection.sendRequestBytes(MessageEchoApp.ECHO_APP, bonjourBuffer);

		assertEquals(bonjourBuffer, bonjourResponse.get());
		assertEquals(helloWorldBuffer, helloWorldResponse.get());
	}
	
//	protected void doClientWrite(InetSocketAddress remoteAddress) throws Exception {
//		java.nio.channels.SocketChannel clientChannel = java.nio.channels.SocketChannel.open(remoteAddress);
//		clientChannel.setOption(StandardSocketOptions.SO_LINGER, 10);
//
//		for(int j=0; j<NUMBER_OF_MESSAGE; j++){
//			int MESSAGE_SIZE=j+600;
//			ByteBuf payloadBytesToWrite = Unpooled.buffer(MESSAGE_SIZE);
//			for (int i=0; i < MESSAGE_SIZE; i++) {
//				payloadBytesToWrite.writeByte(0+i);
//			}
//			sendHeaderAndPayload(clientChannel, MessageEchoApp.ECHO_APP, payloadBytesToWrite);
//			receivedHeaderAndPayload(clientChannel);
//		}
//		
//		PB.NetworkMessage closeMsg=PB.NetworkMessage.newBuilder()
//				.setOperation(NetworkOperation.CLOSE_CONNECTION).build();
//		sendHeaderAndProtobufMessage(clientChannel, NetworkApplication.NETWORK_APPID, closeMsg);
//
//		clientChannel.close();
//		System.out.println("Client done");
//	}
//
//
//	private HeaderAndPayload receivedHeaderAndPayload(SocketChannel clientChannel) throws IOException {
//        int headerLength=readProtobufLength(clientChannel);
//		ByteBuffer headerBytes = ByteBuffer.allocateDirect(headerLength);
//		while(headerBytes.hasRemaining()){
//			int nbBytesRead=clientChannel.read(headerBytes);
//			if(nbBytesRead<0){
//				break;
//			}
//		}
//        
//        int payloadLength=readProtobufLength(clientChannel);
//		ByteBuffer payloadBytes = ByteBuffer.allocateDirect(payloadLength);
//		while(payloadBytes.hasRemaining()){
//			int nbBytesRead=clientChannel.read(payloadBytes);
//			if(nbBytesRead<0){
//				break;
//			}
//		}
//		
//		HeaderMsg header=byteBufToProcolBuffer(headerBytes, PB.HeaderMsg.newBuilder());
//		HeaderAndPayload headerAndPayload= new HeaderAndPayload(header);
//		headerAndPayload.payload=Unpooled.copiedBuffer(payloadBytes);
//		return headerAndPayload;
//	}
//
//
//	@SuppressWarnings("unchecked")
//	protected <T extends MessageLite> T byteBufToProcolBuffer(ByteBuffer msg, Builder builder) throws InvalidProtocolBufferException {
//        final byte[] array;
//        final int offset;
//        final int length = msg.remaining();
//        if (msg.hasArray()) {
//            array = msg.array();
//            offset = msg.arrayOffset() + msg.position();
//        } else {
//            array = new byte[length];
//            msg.get(array, 0, length);
//            offset = 0;
//        }
//
//        return (T) builder.mergeFrom(array, offset, length).build();
//	}
//
//	protected int readProtobufLength(SocketChannel clientChannel) throws IOException {
//		final byte[] buf = new byte[5];
//		ByteBuffer oneByte = ByteBuffer.allocateDirect(1);
//        for (int i = 0; i < buf.length; i ++) {
//        	int nbBytesRead=clientChannel.read(oneByte);
//        	if(nbBytesRead==0){
//        		i--;
//        		continue;
//        	}
//        	
//        	if(nbBytesRead<0){
//        		throw new IOException("Read error EOS");
//        	}
//        	oneByte.flip();
//        	
//            buf[i] = oneByte.get();
//            if (buf[i] >= 0) {
//                int length = CodedInputStream.newInstance(buf, 0, i + 1).readRawVarint32();
//                if (length < 0) {
//                    throw new CorruptedFrameException("negative length: " + length);
//                }
//                return length;
//            }
//        }
//
//        // Couldn't find the byte whose MSB is off.
//        throw new CorruptedFrameException("length wider than 32-bit");
//	}
//
//
//	private void sendHeaderAndProtobufMessage(SocketChannel socketChannel, ApplicationIdentifier appid, NetworkMessage protobufMsg) throws Exception {
//		ProtobufNetworkMessageCodec codec = new ProtobufNetworkMessageCodec();
//		sendHeaderAndPayload(socketChannel, appid, codec.encode(protobufMsg));
//	}
//
//
//	protected void sendHeaderAndPayload(
//			java.nio.channels.SocketChannel socketChannel,
//			ApplicationIdentifier appid, ByteBuf payloadBytesToWrite) throws IOException {
//		HeaderMsg headerMessage = PB.HeaderMsg.newBuilder()
//				.setApplicationId(ByteString.copyFrom(appid.toByteArray()))
//				.build();
//		byte[] headerMessageBytes = headerMessage.toByteArray();
//		
//		writeProtobufLength(socketChannel, headerMessageBytes.length);
//		ByteBuffer headerBytesToWrite = ByteBuffer.wrap(headerMessageBytes);
//		while (headerBytesToWrite.hasRemaining()) {
//			socketChannel.write(headerBytesToWrite);
//		}
//
//		ByteBuffer payloadNioBuffer = payloadBytesToWrite.nioBuffer();
//		writeProtobufLength(socketChannel, payloadNioBuffer.limit());
//		while (payloadNioBuffer.hasRemaining()) {
//			socketChannel.write(payloadNioBuffer);
//		}
//	}
//
//	protected void writeProtobufLength(
//			java.nio.channels.SocketChannel clientChannel,
//			int length) throws IOException {
//		byte[] codedLengthBytes=new byte[CodedOutputStream.computeRawVarint32Size(length)];
//		CodedOutputStream codedOS = CodedOutputStream.newInstance(codedLengthBytes);
//		codedOS.writeRawVarint32(length);
//		codedOS.flush();
//		ByteBuffer codedLengthByteBuffer=ByteBuffer.wrap(codedLengthBytes);
//		while(codedLengthByteBuffer.hasRemaining()){
//			clientChannel.write(codedLengthByteBuffer);
//		}
//	}

	public static void main(String[] args) throws Exception {
		new AsyncSocketServerTest().testAsyncSocketServer();
	}
}
