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

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.btchip.comm.BTChipTransport;
import com.btchip.comm.BTChipTransportFactory;

public class BTChipTransportAndroid implements BTChipTransportFactory {
	
	private UsbManager usbManager;
	private BTChipTransport transport;
	private final LinkedBlockingQueue<Boolean> gotRights = new LinkedBlockingQueue<Boolean>(1);
	
	private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";

	/**
	 * Receives broadcast when a supported USB device is attached, detached or
	 * when a permission to communicate to the device has been granted.
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			String deviceName = usbDevice.getDeviceName();

			if (ACTION_USB_PERMISSION.equals(action)) {
				boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false);
	            Log.d("usb", "ACTION_USB_PERMISSION: " + permission + " Device: " + deviceName);

	            // sync with connect
	            gotRights.add(permission);
			}
		}
	};
		
	public BTChipTransportAndroid(Context context) {
		usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
	}
	
	@Override
	public boolean isPluggedIn() {
		return getDevice(usbManager) != null;
	}
	
	@Override
	public BTChipTransport getTransport() {
		return transport;
	}
	
	@Override
	public boolean connect(final Context context) {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		context.registerReceiver(mUsbReceiver, filter);

		final UsbDevice device = getDevice(usbManager);
		final Intent intent = new Intent(ACTION_USB_PERMISSION);

		usbManager.requestPermission(device, PendingIntent.getBroadcast(context, 0, intent, 0));
		// retry because of InterruptedException
		while (true) {
			try {
				// gotRights.take blocks until the UsbManager gives us the rights via callback to the BroadcastReceiver
	            // this might need an user interaction
				if (gotRights.take()) {
					transport = open(usbManager, device);
					return true;
				}
				else {
					return false;
				}
	         } catch (InterruptedException ignored) {
	        }
		}
	}	
			
	public static UsbDevice getDevice(UsbManager manager) {
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		for (UsbDevice device : deviceList.values()) {
			if ((device.getVendorId() == VID) && 
			   ((device.getProductId() == PID_WINUSB) || (device.getProductId() == PID_HID) ||
			    (device.getProductId() == PID_HID_LEDGER) || (device.getProductId() == PID_HID_LEDGER_PROTON))) {
				return device;
			}
		}
		return null;		
	}
	
	public static BTChipTransport open(UsbManager manager, UsbDevice device) {
		// Must only be called once permission is granted (see http://developer.android.com/reference/android/hardware/usb/UsbManager.html)
		// Important if enumerating, rather than being awaken by the intent notification
		UsbInterface dongleInterface = device.getInterface(0);
        UsbEndpoint in = null;
        UsbEndpoint out = null;
        boolean ledger; 
        for (int i=0; i<dongleInterface.getEndpointCount(); i++) {
            UsbEndpoint tmpEndpoint = dongleInterface.getEndpoint(i);
            if (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                in = tmpEndpoint;
            }
            else {
                out = tmpEndpoint;
            }
        }
        UsbDeviceConnection connection = manager.openDevice(device);
        connection.claimInterface(dongleInterface, true);
        ledger = ((device.getProductId() == PID_HID_LEDGER) || (device.getProductId() == PID_HID_LEDGER_PROTON));
        if (device.getProductId() == PID_WINUSB) {
        	return new BTChipTransportAndroidWinUSB(connection, dongleInterface, in, out, TIMEOUT);
        }
        else {
        	return new BTChipTransportAndroidHID(connection, dongleInterface, in, out, TIMEOUT, ledger);
        }
	}
	
	public static final String LOG_STRING = "BTChip";
	
	private static final int VID = 0x2581;
	private static final int PID_WINUSB = 0x1b7c;
	private static final int PID_HID = 0x2b7c;
	private static final int PID_HID_LEDGER = 0x3b7c;
	private static final int PID_HID_LEDGER_PROTON = 0x4b7c;
	private static final int TIMEOUT = 20000;	
}
