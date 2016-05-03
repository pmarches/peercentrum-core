package org.peercentrum.dht;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.peercentrum.application.BaseApplicationMessageHandler;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.DHTFindMsg;
import org.peercentrum.core.PB.DHTStoreValueMsg;
import org.peercentrum.core.PB.DHTTopLevelMsg.Builder;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.ServerMain;
import org.peercentrum.dht.selfregistration.SelfRegistrationEntry;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public abstract class DHTApplication extends BaseApplicationMessageHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DHTApplication.class);
  private static final ByteBuf PONG_MESSAGE_BYTES;

  protected DHTClient dhtClient;

  public enum OverflowHandling{
    LIFO,
    FIFO
  }

  public DHTApplication(ServerMain serverMain) throws Exception {
    super(serverMain);
    dhtClient=new DHTClient(serverMain.getNetworkClient(), getApplicationId());
  }

  static {
    PB.DHTTopLevelMsg.Builder topLevelMsg=PB.DHTTopLevelMsg.newBuilder();
    topLevelMsg.setPing(PB.DHTPingMsg.newBuilder());
    PONG_MESSAGE_BYTES=Unpooled.wrappedBuffer(topLevelMsg.build().toByteArray());
  }
  
  @Override
  public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
    try {
      PB.DHTTopLevelMsg dhtTopLevelMsg = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, PB.DHTTopLevelMsg.class);
      dhtClient.receivedMessageFrom(new KIdentifier(serverMain.getNetworkServer().getRemoteNodeIdentifier(ctx).getBytes()));
      
      PB.HeaderMsg.Builder responseHeader = super.newResponseHeaderForRequest(receivedMessage);
      PB.DHTTopLevelMsg.Builder topLevelResponseMsg=PB.DHTTopLevelMsg.newBuilder();
      if(dhtTopLevelMsg.hasPing()){
        return new HeaderAndPayload(responseHeader, PONG_MESSAGE_BYTES);
      }
      else if(dhtTopLevelMsg.getFindCount()!=0){
        if(dhtTopLevelMsg.getFindCount()>1){
          LOGGER.warn("Multiple find not implemented yet, searching only for the first one");
        }
        handleFindMsg(topLevelResponseMsg, dhtTopLevelMsg.getFind(0));
        return new HeaderAndPayload(responseHeader, Unpooled.wrappedBuffer(topLevelResponseMsg.build().toByteArray()));
      }
      else if(dhtTopLevelMsg.getStoreValueCount()!=0){
        for(int i=0; i<dhtTopLevelMsg.getStoreValueCount(); i++){
          handleValidStoreMsg(topLevelResponseMsg, dhtTopLevelMsg.getStoreValue(i));
        }
        return new HeaderAndPayload(responseHeader, Unpooled.wrappedBuffer(topLevelResponseMsg.build().toByteArray()));
      }
    } catch (Exception e) {
      LOGGER.error("generateReponseFromQuery failed", e);
    }
    return null;
  }

  abstract protected void handleValidStoreMsg(Builder topLevelResponseMsg, DHTStoreValueMsg storeValue) throws Exception;
  abstract protected void handleFindMsg(Builder topLevelResponseMsg, DHTFindMsg find) throws Exception;

  protected void populateClosestNodeTo(byte[] nodeIdToFind, int numberOfNodesRequested, PB.DHTFoundMsg.Builder foundMsg) {
    List<KIdentifier> closest=dhtClient.buckets.getClosestNodeTo(new KIdentifier(nodeIdToFind), numberOfNodesRequested);
    for(KIdentifier oneId : closest){
      PB.PeerEndpointMsg.Builder peerEndpointMsg=PB.PeerEndpointMsg.newBuilder();
      peerEndpointMsg.setIdentity(ByteString.copyFrom(oneId.getBytes()));

      InetSocketAddress socketAddress = serverMain.getNodeDatabase().getEndpointByNodeIdentifier(new NodeIdentifier(oneId.getBytes()));
      PB.PeerEndpointMsg.TLSEndpointMsg.Builder ipEndpointBuilder=PB.PeerEndpointMsg.TLSEndpointMsg.newBuilder();
      ipEndpointBuilder.setIpAddress(socketAddress.getHostName());
      ipEndpointBuilder.setPort(socketAddress.getPort());
      peerEndpointMsg.setTlsEndpoint(ipEndpointBuilder);

      foundMsg.addClosestNodes(peerEndpointMsg);
    }
  }


  public DHTClient getDHTClient() {
    return this.dhtClient;
  }

  protected void setEntryTimeToLive(int i, TimeUnit days) {
    // TODO Auto-generated method stub
    
  }

  protected void setEntryMaximumCardinality(int i) {
    // TODO Auto-generated method stub
    
  }

  protected void setEntryOverflowHandling(OverflowHandling lifo) {
    // TODO Auto-generated method stub
  }

  public void onEntryValueOverflow(SelfRegistrationEntry entry){
  }

  public void onEntryTimeToLiveExpired(SelfRegistrationEntry entry){
  }

  abstract public boolean isTransactionValid(DHTStoreValueMsg storeValueMsg);
}