package com.mrd.mbwapi.api;

import java.io.Serializable;
import java.net.URI;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;

public class WalletVersionResponse extends ApiObject implements Serializable {

   private static final long serialVersionUID = -8708847365845218804L;
   public final String versionNumber;
   public final URI directDownload;
   public final String message;

   public WalletVersionResponse(String versionNumber, URI directDownload, String message) {
      this.versionNumber = versionNumber;
      this.directDownload = directDownload;
      this.message = message;
   }

   public WalletVersionResponse(ByteReader payloadReader) {
      try {
         versionNumber = payloadReader.getString();
         directDownload = URI.create(payloadReader.getString());
         message = payloadReader.getString();
      } catch (ByteReader.InsufficientBytesException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putString(versionNumber);
      writer.putString(directDownload.toString());
      writer.putString(message);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.WALLET_VERSION_RESPONSE_TYPE;
   }

   @Override
   public String toString() {
      return "WalletVersionResponse{" +
            "versionNumber='" + versionNumber + '\'' +
            ", directDownload=" + directDownload +
            ", message='" + message + '\'' +
            '}';
   }
}
