package org.peercentrum.settlement;

public class SettlementConfig {
	String rippleSeed;
	String bitcoinWalletPath;
	String settlementDbPath;
  String bitcoinNetwork="regtest";
	
	public String getSettlementDbPath() {
		return settlementDbPath;
	}
	public void setSettlementDbPath(String settlementDbPath) {
		this.settlementDbPath = settlementDbPath;
	}
	public String getRippleSeed() {
		return rippleSeed;
	}
	public void setRippleSeed(String rippleSeed) {
		this.rippleSeed = rippleSeed;
	}

	public String getBitcoinWalletPath() {
		return bitcoinWalletPath;
	}
	public void setBitcoinWalletPath(String bitcoinWalletPath) {
		this.bitcoinWalletPath = bitcoinWalletPath;
	}

	public String getBitcoinNetwork() {
    return bitcoinNetwork;
  }
	public void setBitcoinNetwork(String networkStr) {
    bitcoinNetwork=networkStr;
  }

}
