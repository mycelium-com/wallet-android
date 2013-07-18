package com.mrd.mbwapi.api;

import com.google.common.base.Throwables;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;


public class ErrorCollectionRequest extends ApiObject {

   //does not make much sense to have actual stacktraces here because we are traversing VMs
   public final String error;
   public final String version;

   public ErrorCollectionRequest(Throwable t, String version) {
      this.version = version;
      this.error = Throwables.getStackTraceAsString(t);
   }

   public ErrorCollectionRequest(ByteReader reader) {
      try {
         error = reader.getString();
         version = reader.getString();
      } catch (ByteReader.InsufficientBytesException e) {
         throw new IllegalArgumentException("could not read error string",e);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putString(error);
      return writer;
   }

   @Override
   protected byte getType() {
      return ERROR_REQUEST_TYPE;
   }
}
