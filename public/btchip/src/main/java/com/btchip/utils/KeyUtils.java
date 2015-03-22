package com.btchip.utils;

import com.btchip.BTChipException;

public class KeyUtils {
	
	public static byte[] compressPublicKey(byte[] publicKey) throws BTChipException {
		switch(publicKey[0]) {
			case 0x04:
				if (publicKey.length != 65) {
					throw new BTChipException("Invalid public key");
				}
				break;
			case 0x02:
			case 0x03:
				if (publicKey.length != 33) {
					throw new BTChipException("Invalid public key");
				}
				return publicKey;
			default:
				throw new BTChipException("Invalid public key");
		}
		byte[] result = new byte[33];
		result[0] = (((publicKey[64] & 1) != 0) ? (byte)0x03 : (byte)0x02);
		System.arraycopy(publicKey, 1, result, 1, 32);
		return result;		
	}

}
