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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import com.btchip.BTChipException;

public class SignatureUtils {
	
	private static final BigInteger HALF_ORDER = new BigInteger(Dump.hexToBin("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0"));
	private static final BigInteger ORDER = new BigInteger(1, Dump.hexToBin("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"));	
	
	public static byte[] canonicalize(byte[] signature, boolean includeHashType, int hashType) throws BTChipException {
		ByteArrayInputStream in = new ByteArrayInputStream(signature);
		if (in.read() != 0x30) {
			throw new BTChipException("Invalid sequence");
		}
		in.read();
		if (in.read() != (byte)0x02) {
			throw new BTChipException("Invalid r");
		}
		int rSize = in.read();
		byte[] value = new byte[rSize];
		in.read(value, 0, rSize);		
		BigInteger r = new BigInteger(value);
		if (in.read() != (byte)0x02) {
			throw new BTChipException("Invalid s");
		}
		int sSize = in.read();
		value = new byte[sSize];
		in.read(value, 0, sSize);
		BigInteger s = new BigInteger(value);
		if (s.compareTo(HALF_ORDER) > 0) {
			s = ORDER.subtract(s);
			byte[] rByte = r.toByteArray();
			byte[] sByte = s.toByteArray();
			ByteArrayOutputStream result = new ByteArrayOutputStream(100);
			result.write(0x30);
			result.write(2 + rByte.length + 2 + sByte.length);
			result.write(0x02);
			result.write(rByte.length);
			result.write(rByte, 0, rByte.length);
			result.write(0x02);
			result.write(sByte.length);
			result.write(sByte, 0, sByte.length);
			if (includeHashType) {
				result.write(hashType);
			}
			return result.toByteArray();
		}
		else {
			return signature;
		}
		
	}
}
