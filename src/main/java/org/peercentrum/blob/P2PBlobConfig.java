package org.peercentrum.blob;


public class P2PBlobConfig {
	String blobRepositoryPath;
	String storagePricePerGBPerDay;
	String inboundTransferPricePerGB;
	String outboundTransferPricePerGB;

	
	
	public String getStoragePricePerGBPerDay() {
		return storagePricePerGBPerDay;
	}
	public void setStoragePricePerGBPerDay(String storagePricePerGBPerDay) {
		this.storagePricePerGBPerDay = storagePricePerGBPerDay;
	}
	public String getInboundTransferPricePerGB() {
		return inboundTransferPricePerGB;
	}
	public void setInboundTransferPricePerGB(String inboundTransferPricePerGB) {
		this.inboundTransferPricePerGB = inboundTransferPricePerGB;
	}
	public String getOutboundTransferPricePerGB() {
		return outboundTransferPricePerGB;
	}
	public void setOutboundTransferPricePerGB(String outboundTransferPricePerGB) {
		this.outboundTransferPricePerGB = outboundTransferPricePerGB;
	}
	public void setBlobRepositoryPath(String blobRepositoryPath){
		this.blobRepositoryPath=blobRepositoryPath;
	}
	public String getBlobRepositoryPath() {
		return this.blobRepositoryPath;
	}
	
}
