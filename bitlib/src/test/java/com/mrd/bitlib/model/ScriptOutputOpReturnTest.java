package com.mrd.bitlib.model;

import org.junit.Test;

import static com.mrd.bitlib.model.Script.OP_RETURN;
import static org.junit.Assert.*;

import static com.mrd.bitlib.model.ScriptOutputOpReturn.isScriptOutputOpReturn;

public class ScriptOutputOpReturnTest {
   @Test
   public void isScriptOutputOpReturnTest() throws Exception {
      byte[][] chunks = new byte[2][];
      chunks[0]=new byte[]{OP_RETURN};
      assertFalse(isScriptOutputOpReturn(chunks));
      assertTrue(isScriptOutputOpReturn(new byte[][]{{OP_RETURN}, "Hallotest".getBytes()}));
      assertFalse(isScriptOutputOpReturn(new byte[][]{{OP_RETURN}, null}));
      assertFalse(isScriptOutputOpReturn(new byte[][]{{OP_RETURN}, "".getBytes()}));
      assertFalse(isScriptOutputOpReturn(new byte[][]{null, "Hallo".getBytes()}));
   }
}
