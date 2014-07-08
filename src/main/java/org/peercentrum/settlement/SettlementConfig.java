package org.peercentrum.settlement;

public class SettlementConfig {
	String rippleSeed;
	String bitcoinWalletPath;
	String settlementDbPath;
  
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
}
