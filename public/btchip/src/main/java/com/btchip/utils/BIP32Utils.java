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

import java.io.ByteArrayOutputStream;

import com.btchip.BTChipException;

public class BIP32Utils {
	
	public static byte[] splitPath(String path) throws BTChipException {
		if (path.length() == 0) {
			return new byte[] { 0 };
		}		
		String elements[] = path.split("/");
		if (elements.length > 10) {
			throw new BTChipException("Path too long");
		}
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		result.write((byte)elements.length);
		for (String element : elements) {
			long elementValue;
			int hardenedIndex = element.indexOf('\'');
			if (hardenedIndex > 0) {
				elementValue = Long.parseLong(element.substring(0, hardenedIndex));
				elementValue |= 0x80000000;
			}
			else {
				elementValue = Long.parseLong(element);
			}
			BufferUtils.writeUint32BE(result, elementValue);
		}
		return result.toByteArray();
	}
}
