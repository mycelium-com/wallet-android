package com.mrd.mbwapi.api;

import com.mrd.bitlib.util.ByteWriter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ErrorCollectionRequestTest {
   @Test
   public void testToByteWriter() throws Exception {
      String longString = String.valueOf(new char[2000]);
      ErrorCollectionRequest req = new ErrorCollectionRequest(new RuntimeException(longString), "junit", ErrorMetaData.DUMMY);
      ByteWriter bigWriter = new ByteWriter(1024);
      assertEquals(0, bigWriter.length());
      req.toByteWriter(bigWriter);
      assertTrue(bigWriter.length() > 1024);
   }
}
