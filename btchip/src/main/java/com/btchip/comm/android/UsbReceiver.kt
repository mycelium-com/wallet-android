package com.btchip.comm.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * Receives broadcast when a supported USB device is attached, detached or
 * when a permission to communicate to the device has been granted.
 */
class UsbReceiver(val result: (Boolean) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_USB_PERMISSION == intent.action) {
            val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            val deviceName = usbDevice?.deviceName

            val permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            Log.d(TAG, "ACTION_USB_PERMISSION: $permission Device: $deviceName");

            result(permission)
            context.unregisterReceiver(this);
        }
    }

    companion object {
        const val TAG = "UsbReceiver"
        const val ACTION_USB_PERMISSION = "USB_PERMISSION";
    }
}