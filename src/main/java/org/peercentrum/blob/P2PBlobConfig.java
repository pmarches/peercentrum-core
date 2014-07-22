package org.peercentrum.blob;

import java.util.LinkedHashMap;



public class P2PBlobConfig {
  public Object transferPricingPerGigabyte;
  
  public PriceLimits getPriceLimits(){
    PriceLimits limits=new PriceLimits();
    if(transferPricingPerGigabyte instanceof LinkedHashMap){
      LinkedHashMap<String, Integer> detailedPricing=(LinkedHashMap<String, Integer>) transferPricingPerGigabyte;
      limits.consumerSatoshiPerIncomingGigabyte=detailedPricing.get("wePayIncoming");
      limits.consumerSatoshiPerOutgoingGigabyte=detailedPricing.get("wePayOutgoing");
      limits.producerSatoshiPerIncomingGigabyte=detailedPricing.get("theyPayIncoming");
      limits.producerSatoshiPerOutgoingGigabyte=detailedPricing.get("theyPayOutgoing");
    }
    else if(transferPricingPerGigabyte instanceof Integer){
      long price=(Integer) transferPricingPerGigabyte;
      limits.consumerSatoshiPerIncomingGigabyte=price;
      limits.consumerSatoshiPerOutgoingGigabyte=price;
      limits.producerSatoshiPerIncomingGigabyte=price;
      limits.producerSatoshiPerOutgoingGigabyte=price;
    }
    else if(transferPricingPerGigabyte instanceof String){
      String transferPricingPerGigabyteStr=(String) transferPricingPerGigabyte;
      if("market".equals(transferPricingPerGigabyteStr)){        
      }
      else if(transferPricingPerGigabyteStr.endsWith("USD")){
      }
      else if(transferPricingPerGigabyteStr.endsWith("XRP")){
      }
    }
    return limits;
  }
}
