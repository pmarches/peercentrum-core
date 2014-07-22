package org.peercentrum.blob;

public class PriceLimits {
  long producerSatoshiPerIncomingGigabyte;
  long producerSatoshiPerOutgoingGigabyte;
  long consumerSatoshiPerIncomingGigabyte=0;
  long consumerSatoshiPerOutgoingGigabyte=0;

  public boolean isDownloadPriceWithinLimits(long downloadPricePerGB) {
    return consumerSatoshiPerIncomingGigabyte<=downloadPricePerGB;
  }
}
