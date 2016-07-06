package com.satoshilabs.trezor;

import android.content.Context;

// based on https://github.com/keepkey/keepkey-android
public class Trezor extends ExternalSignatureDevice {
   public static final int TREZOR_USB_VENDOR_ID = 0x534c;
   public static final int TREZOR_USB_PROD_ID = 0x0001;
   private static final String DEFAULT_LABEL = "Trezor";
   private static final VersionNumber MOST_RECENT_VERSION = new VersionNumber(1, 3, 5);

   public Trezor(Context context) {
      super(context);
   }


   @Override
   UsbDeviceId getUsbId() {
      return new UsbDeviceId(TREZOR_USB_VENDOR_ID, TREZOR_USB_PROD_ID);
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
