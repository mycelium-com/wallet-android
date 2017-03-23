package com.mrd.bitlib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildEncodingTest {
   @Test
   public void testChars(){
      assertEquals(3,"ΟΛΩ".length());
      assertEquals("\u039f\u039b\u03a9","ΟΛΩ");
   }
}
