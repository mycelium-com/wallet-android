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

package com.btchip.comm.android;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import android.util.Log;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.LedgerHelper;
import com.btchip.utils.Dump;
import com.btchip.utils.FutureUtils;

public class BTChipTransportAndroidHID implements BTChipTransport {

	private UsbDeviceConnection connection;
	private UsbInterface dongleInterface;
	private UsbEndpoint in;
	private UsbEndpoint out;
	private int timeout;
	private byte transferBuffer[];
	private boolean debug;
	private boolean ledger;
	
	public BTChipTransportAndroidHID(UsbDeviceConnection connection, UsbInterface dongleInterface, UsbEndpoint in, UsbEndpoint out, int timeout, boolean ledger) {
		this.connection = connection;
		this.dongleInterface = dongleInterface;
		this.in = in;
		this.out = out;
		this.ledger = ledger;
		// Compatibility with old prototypes, to be removed
		if (!this.ledger) {
			this.ledger = (in.getEndpointNumber() != out.getEndpointNumber());
		}
		this.timeout = timeout;
		transferBuffer = new byte[HID_BUFFER_SIZE];
	}

	@Override
	public Future<byte[]> exchange(byte[] command) throws BTChipException {
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		byte[] responseData = null;
		int offset = 0;
		int responseSize;
		int result;
		if (debug) {
			Log.d(BTChipTransportAndroid.LOG_STRING, "=> " + Dump.dump(command));
		}
		if (ledger) {
			command = LedgerHelper.wrapCommandAPDU(LEDGER_DEFAULT_CHANNEL, command, HID_BUFFER_SIZE);
		}
		UsbRequest request = new UsbRequest();
		if (!request.initialize(connection, out)) {
			throw new BTChipException("I/O error");
		}
		while(offset != command.length) {
			int blockSize = (command.length - offset > HID_BUFFER_SIZE ? HID_BUFFER_SIZE : command.length - offset);
			System.arraycopy(command, offset, transferBuffer, 0, blockSize);
			if (!request.queue(ByteBuffer.wrap(transferBuffer), HID_BUFFER_SIZE)) {
				throw new BTChipException("I/O error");	
			}
			connection.requestWait();
			offset += blockSize;
		}
		ByteBuffer responseBuffer = ByteBuffer.allocate(HID_BUFFER_SIZE);
		request = new UsbRequest();
		if (!request.initialize(connection, in)) {
			throw new BTChipException("I/O error");
		}
		if (!ledger) {
			if (!request.queue(responseBuffer, HID_BUFFER_SIZE)) {
				throw new BTChipException("I/O error");
			}
			connection.requestWait();
			responseBuffer.rewind();
			int sw1 = (int)(responseBuffer.get() & 0xff);
			int sw2 = (int)(responseBuffer.get() & 0xff);
			if (sw1 != SW1_DATA_AVAILABLE) {
				response.write(sw1);
				response.write(sw2);
			}
			else {
				responseSize = sw2 + 2;
				offset = 0;
				int blockSize = (responseSize > HID_BUFFER_SIZE - 2 ? HID_BUFFER_SIZE - 2 : responseSize);
				responseBuffer.get(transferBuffer, 0, blockSize);
				response.write(transferBuffer, 0, blockSize);
				offset += blockSize;
				while (offset != responseSize) {
					responseBuffer.clear();
					if (!request.queue(responseBuffer, HID_BUFFER_SIZE)) {
						throw new BTChipException("I/O error");
					}
					connection.requestWait();
					responseBuffer.rewind();
					blockSize = (responseSize - offset > HID_BUFFER_SIZE ? HID_BUFFER_SIZE : responseSize - offset);
					responseBuffer.get(transferBuffer, 0, blockSize);
					response.write(transferBuffer, 0, blockSize);
					offset += blockSize;				
				}
				responseBuffer.clear();
			}
			responseData = response.toByteArray();
		}
		else {			
			while ((responseData = LedgerHelper.unwrapResponseAPDU(LEDGER_DEFAULT_CHANNEL, response.toByteArray(), HID_BUFFER_SIZE)) == null) {
				responseBuffer.clear();
				if (!request.queue(responseBuffer, HID_BUFFER_SIZE)) {
					throw new BTChipException("I/O error");
				}
				connection.requestWait();
				responseBuffer.rewind();
				responseBuffer.get(transferBuffer, 0, HID_BUFFER_SIZE);
				response.write(transferBuffer, 0, HID_BUFFER_SIZE);				
			}						
		}		
		if (debug) {
			Log.d(BTChipTransportAndroid.LOG_STRING, "<= " + Dump.dump(responseData));
		}
		return FutureUtils.getDummyFuture(responseData);				
	}

	@Override
	public void close() throws BTChipException {
		connection.releaseInterface(dongleInterface);
		connection.close();
	}

	
	@Override
	public void setDebug(boolean debugFlag) {
		this.debug = debugFlag;
	}
	
	private static final int HID_BUFFER_SIZE = 64;
	private static final int LEDGER_DEFAULT_CHANNEL = 1;
	private static final int SW1_DATA_AVAILABLE = 0x61;
}
