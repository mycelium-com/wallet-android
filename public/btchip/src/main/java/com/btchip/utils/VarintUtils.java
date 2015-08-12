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

import com.btchip.BTChipException;

public class VarintUtils {
	
	public static long read(ByteArrayInputStream in) throws BTChipException {
		long result = 0;
		int val1 = (int)(in.read() & 0xff);
		if (val1 < 0xfd) {
			result = val1;
		}
		else
		if (val1 == 0xfd) {
			result |= (int)(in.read() & 0xff);
			result |= (((int)in.read() & 0xff) << 8);
		}
		else
		if (val1 == 0xfe) {
			result |= (int)(in.read() & 0xff);
			result |= (((int)in.read() & 0xff) << 8);
			result |= (((int)in.read() & 0xff) << 16);
			result |= (((int)in.read() & 0xff) << 24);
		}
		else {
			throw new BTChipException("Unsupported varint encoding");
		}
		return result;
	}
	
	public static void write(ByteArrayOutputStream buffer, long value) {
		if (value < 0xfd) {
			buffer.write((byte)value);
		}
		else
		if (value <= 0xffff) {
			buffer.write(0xfd);
			buffer.write((byte)(value & 0xff));
			buffer.write((byte)((value >> 8) & 0xff));			
		}
		else {
			buffer.write(0xfe);
			buffer.write((byte)(value & 0xff));
			buffer.write((byte)((value >> 8) & 0xff));
			buffer.write((byte)((value >> 16) & 0xff));
			buffer.write((byte)((value >> 24) & 0xff));
		}
	}
}
