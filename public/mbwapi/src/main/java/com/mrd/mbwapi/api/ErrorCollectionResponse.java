package com.mrd.mbwapi.api;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;


public class ErrorCollectionResponse extends ApiObject {

   //nothing interesting here for now.

   public ErrorCollectionResponse(ByteReader payloadReader) {

   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      return writer;
   }

   @Override
   protected byte getType() {
      return ERROR_RESPONSE_TYPE;
   }
}
