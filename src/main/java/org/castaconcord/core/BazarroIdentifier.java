package org.castaconcord.core;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import com.google.protobuf.ByteString;

public class BazarroIdentifier {
	final static SecureRandom RANDOM = new SecureRandom();
	protected byte[] binaryValue;
	
	public BazarroIdentifier() {
		binaryValue=new byte[32];
		RANDOM.nextBytes(binaryValue);
	}
	
	public BazarroIdentifier(byte[] binaryValue) {
		this.binaryValue=binaryValue;
	}
	
	public byte[] getBytes(){
		return binaryValue;
	}

	public ByteString toByteString() {
		return ByteString.copyFrom(binaryValue);
	}

	@Override
	public String toString() {
		return DatatypeConverter.printHexBinary(binaryValue);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(binaryValue);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof BazarroIdentifier)){
			return false;
		}
		BazarroIdentifier other=(BazarroIdentifier) obj;
		return Arrays.equals(binaryValue, other.binaryValue);
	}

}
