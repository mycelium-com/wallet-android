package com.satoshilabs.trezor;

import android.content.Context;

// based on https://github.com/keepkey/keepkey-android
public class KeepKey extends ExternalSignatureDevice {
   public static final int KEEPKEY_USB_VENDOR_ID = 0x2B24;
   public static final int KEEPKEY_USB_PROD_ID = 0x0001;
   private static final String DEFAULT_LABEL = "KeepKey";
   private static final VersionNumber MOST_RECENT_VERSION = new VersionNumber(1, 1, 0);

   public KeepKey(Context context) {
      super(context);
   }


   @Override
   UsbDeviceId getUsbId() {
      return new UsbDeviceId(KEEPKEY_USB_VENDOR_ID, KEEPKEY_USB_PROD_ID);
   }

   @Override
   public String getDefaultAccountName() {
      return DEFAULT_LABEL;
   }

   @Override
   public VersionNumber getMostRecentFirmwareVersion() {
      return MOST_RECENT_VERSION;
   }
}
