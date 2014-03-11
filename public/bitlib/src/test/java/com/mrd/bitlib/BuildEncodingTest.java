package com.mrd.bitlib;

import junit.framework.Assert;
import org.junit.Test;

public class BuildEncodingTest {
   @Test
   public void testChars(){
      Assert.assertEquals(3,"ΟΛΩ".length());
      Assert.assertEquals("\u039f\u039b\u03a9","ΟΛΩ");
   }
}
