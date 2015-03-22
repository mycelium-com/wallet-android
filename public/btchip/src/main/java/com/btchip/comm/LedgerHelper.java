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

package com.btchip.comm;

import java.io.ByteArrayOutputStream;

import com.btchip.BTChipException;

public class LedgerHelper {
	
	private static final int TAG_APDU = 0x05;
	
	public static byte[] wrapCommandAPDU(int channel, byte[] command, int packetSize) throws BTChipException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (packetSize < 3) {
			throw new BTChipException("Can't handle Ledger framing with less than 3 bytes for the report");
		}
		int sequenceIdx = 0;
		int offset = 0;
		output.write(channel >> 8);
		output.write(channel);
		output.write(TAG_APDU);
		output.write(sequenceIdx >> 8);
		output.write(sequenceIdx);
		sequenceIdx++;
		output.write(command.length >> 8);
		output.write(command.length);
		int blockSize = (command.length > packetSize - 7 ? packetSize - 7 : command.length);
		output.write(command, offset, blockSize);
		offset += blockSize;
		while (offset != command.length) {
			output.write(channel >> 8);
			output.write(channel);
			output.write(TAG_APDU);
			output.write(sequenceIdx >> 8);
			output.write(sequenceIdx);
			sequenceIdx++;
			blockSize = (command.length - offset > packetSize - 5 ? packetSize - 5 : command.length - offset);
			output.write(command, offset, blockSize);
			offset += blockSize;			
		}
		if ((output.size() % packetSize) != 0) {
			byte[] padding = new byte[packetSize - (output.size() % packetSize)];
			output.write(padding, 0, padding.length);
		}
		return output.toByteArray();		
	}
	
	public static byte[] unwrapResponseAPDU(int channel, byte[] data, int packetSize) throws BTChipException {
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		int offset = 0;
		int responseLength;
		int sequenceIdx = 0;
		if ((data == null) || (data.length < 7 + 5)) {
			return null;
		}
		if (data[offset++] != (channel >> 8)) {
			throw new BTChipException("Invalid channel");
		}
		if (data[offset++] != (channel & 0xff)) {
			throw new BTChipException("Invalid channel");
		}
		if (data[offset++] != TAG_APDU) {
			throw new BTChipException("Invalid tag");			
		}
		if (data[offset++] != 0x00) {
			throw new BTChipException("Invalid sequence");
		}
		if (data[offset++] != 0x00) {
			throw new BTChipException("Invalid sequence");
		}
		responseLength = ((data[offset++] & 0xff) << 8);
		responseLength |= (data[offset++] & 0xff);
		if (data.length < 7 + responseLength) {
			return null;
		}
		int blockSize = (responseLength > packetSize - 7 ? packetSize - 7 : responseLength);
		response.write(data, offset, blockSize);
		offset += blockSize;
		while (response.size() != responseLength) {
			sequenceIdx++;
			if (offset == data.length) {
				return null;
			}
			if (data[offset++] != (channel >> 8)) {
				throw new BTChipException("Invalid channel");
			}
			if (data[offset++] != (channel & 0xff)) {
				throw new BTChipException("Invalid channel");
			}
			if (data[offset++] != TAG_APDU) {
				throw new BTChipException("Invalid tag");			
			}
			if (data[offset++] != (sequenceIdx >> 8)) {
				throw new BTChipException("Invalid sequence");
			}
			if (data[offset++] != (sequenceIdx & 0xff)) {
				throw new BTChipException("Invalid sequence");
			}
			blockSize = (responseLength - response.size() > packetSize - 5 ? packetSize - 5 : responseLength - response.size());
			if (blockSize > data.length - offset) {
				return null;
			}
			response.write(data, offset, blockSize);
			offset += blockSize;			
		}
		return response.toByteArray();
	}

}
