package org.peercentrum.settlement;

public class SettlementConfig {
	String rippleSeed;
	String bitcoinPrivateKey;
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

	public String getBitcoinPrivateKey() {
		return bitcoinPrivateKey;
	}
	public void setBitcoinPrivateKey(String bitcoinPrivateKey) {
		this.bitcoinPrivateKey = bitcoinPrivateKey;
	}
}
