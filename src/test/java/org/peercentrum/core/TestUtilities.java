package org.peercentrum.core;

import java.io.File;

import com.azazar.bitcoin.jsonrpcclient.BitcoinJSONRPCClient;
import com.azazar.bitcoin.jsonrpcclient.BitcoinRPCException;

public class TestUtilities {
  public static TopLevelConfig getConfig(String whatNode) throws Exception{
    File preparedConfigFile=new File("testdata/"+whatNode+"/peercentrum-config.yaml");
    if(preparedConfigFile.exists()==false){
      throw new Exception("Unknown node '"+whatNode+"'");
    }
    return TopLevelConfig.loadFromFile(preparedConfigFile);
  }
  
  public static BitcoinJSONRPCClient getBitcoindRegtest() throws Exception {
    BitcoinJSONRPCClient bitcoin;
    try {
      bitcoin = new BitcoinJSONRPCClient(true);
      bitcoin.getBlockCount(); //Try to trigger exception
    } catch (BitcoinRPCException e) {
      Runtime.getRuntime().exec("bitcoind --regtest -server -daemon").waitFor();
      Thread.sleep(1000);
      bitcoin = new BitcoinJSONRPCClient(true);
    }
    return bitcoin;
  }
}
