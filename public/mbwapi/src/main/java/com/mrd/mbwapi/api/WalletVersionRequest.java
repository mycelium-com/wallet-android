package com.mrd.mbwapi.api;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;

import java.util.Locale;

public class WalletVersionRequest extends ApiObject {

   public final String currentVersion;
   public final Locale locale;

   public WalletVersionRequest(String currentVersion, Locale locale) {
      this.currentVersion = currentVersion;
      this.locale = locale;
   }

   public WalletVersionRequest(ByteReader payloadReader) {
      try {
         currentVersion = payloadReader.getString();
         locale = new Locale(payloadReader.getString());
      } catch (ByteReader.InsufficientBytesException e) {
         throw new IllegalArgumentException("could not read version request", e);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putString(currentVersion);
      writer.putString(locale.toString());
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.WALLET_VERSION_REQUEST_TYPE;
   }
}
