//package org.peercentrum.settlement;
//
//import org.pmarches.jBlockChainAPI.BlockchainDotInfoConnection;
//
//import com.google.bitcoin.core.Address;
//import com.google.bitcoin.core.Coin;
//
//public class SybillScore {
//  BlockchainDotInfoConnection bdiConnection = new BlockchainDotInfoConnection();
//  
//  public SybillScore() throws Exception {
//  }
//  
//  public long getScore(Address addressToScore) throws Exception{
//    long bitcoinAge=bdiConnection.getUnspentCoinAge(addressToScore);
//    //TODO This is kinda crude
//    return bitcoinAge;
//  }
//  
//  public boolean isAddressGenuine(Address addressToCheck, boolean highConfidenceRequired) throws Exception{
//    long bitcoinAge=bdiConnection.getUnspentCoinAge(addressToCheck);
//    if(highConfidenceRequired){
//      return bitcoinAge>(Coin.COIN.longValue()*6*24); //24 hour confidence
//    }
//    else{
//      return bitcoinAge>(Coin.COIN.longValue()*1); //10 minute confidence
//    }
//  }
//}
