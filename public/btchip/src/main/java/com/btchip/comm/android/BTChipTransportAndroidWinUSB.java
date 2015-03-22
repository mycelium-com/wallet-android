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

import android.util.Log;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.utils.Dump;

public class BTChipTransportAndroidWinUSB implements BTChipTransport {

	private UsbDeviceConnection connection;
	private UsbInterface dongleInterface;
	private UsbEndpoint in;
	private UsbEndpoint out;
	private int timeout;
	private byte transferBuffer[];
	private boolean debug;
	
	public BTChipTransportAndroidWinUSB(UsbDeviceConnection connection, UsbInterface dongleInterface, UsbEndpoint in, UsbEndpoint out, int timeout) {
		this.connection = connection;
		this.dongleInterface = dongleInterface;
		this.in = in;
		this.out = out;
		this.timeout = timeout;
		transferBuffer = new byte[260];
	}

	@Override
	public byte[] exchange(byte[] command) throws BTChipException {
		if (debug) {
			Log.d(BTChipTransportAndroid.LOG_STRING, "=> " + Dump.dump(command));
		}
		int result = connection.bulkTransfer(out, command, command.length, timeout);
		if (result != command.length) {
			throw new BTChipException("Write failed");
		}
		result = connection.bulkTransfer(in, transferBuffer, transferBuffer.length, timeout);
		if (result < 0) {
			throw new BTChipException("Write failed");
		}
		if (debug) {
			Log.d(BTChipTransportAndroid.LOG_STRING, "<= " + Dump.dump(transferBuffer));
		}		
		int sw1 = (int)(transferBuffer[0] & 0xff);
		int sw2 = (int)(transferBuffer[1] & 0xff);
		if (sw1 != SW1_DATA_AVAILABLE) {
			byte[] response = new byte[2];
			response[0] = (byte)sw1;
			response[1] = (byte)sw2;
			return response;
		}
		byte[] response = new byte[sw2 + 2];
		System.arraycopy(transferBuffer, 2, response, 0, sw2 + 2);
		return response;
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
	
	private static final int SW1_DATA_AVAILABLE = 0x61;

}
