/*
*******************************************************************************    
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

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
