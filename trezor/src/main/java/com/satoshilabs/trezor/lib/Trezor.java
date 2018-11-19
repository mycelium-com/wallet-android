package com.satoshilabs.trezor.lib;

import android.content.Context;


public class Trezor extends ExternalSignatureDevice {
   public static final UsbDeviceIds USB_IDS = new UsbDeviceIds(
           new SingleUsbDeviceId(0x534c, 0x0001), // trezorV1
           new SingleUsbDeviceId(0x1209, 0x53C1) // trezorV2
   );
   private static final String DEFAULT_LABEL = "Trezor";
   private static final VersionNumber MOST_RECENT_VERSION = new VersionNumber(1, 4, 0);

   public Trezor(Context context) {
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
      return "io.trezor.app";
   }
}
