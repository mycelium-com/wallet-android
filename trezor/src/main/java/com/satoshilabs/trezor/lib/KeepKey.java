package com.satoshilabs.trezor.lib;

import android.content.Context;

public class KeepKey extends ExternalSignatureDevice {
   public static final UsbDeviceIds USB_IDS = new UsbDeviceIds(
           new SingleUsbDeviceId(0x2B24, 0x0001), // KeepKey
           new SingleUsbDeviceId(0x2B24, 0x0002) // KeepKey after firmware update
   );
   private static final String DEFAULT_LABEL = "KeepKey";
   private static final VersionNumber MOST_RECENT_VERSION = new VersionNumber(1, 1, 0);

   public KeepKey(Context context) {
      super(context);
   }

   @Override
   UsbDeviceId getUsbId() {
      return USB_IDS;
   }

   @Override
   public String getDefaultAccountName() {
      return DEFAULT_LABEL;
   }

   @Override
   public VersionNumber getMostRecentFirmwareVersion() {
      return MOST_RECENT_VERSION;
   }

   @Override
   public String getDeviceConfiguratorAppName() {
      return null;
   }
}
