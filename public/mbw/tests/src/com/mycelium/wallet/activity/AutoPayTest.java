package com.mycelium.wallet.activity;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AutoPayTest {

   @Test
   public void testExtractAmount(){
  //    assertEquals("", SettingsActivity.extractAmount(null));
      assertEquals("", SettingsActivity.extractAmount(""));
      assertEquals("1", SettingsActivity.extractAmount("1"));
      assertEquals("1.00", SettingsActivity.extractAmount("1.00"));
      assertEquals("1.00", SettingsActivity.extractAmount("1,00"));
      assertEquals("0.10", SettingsActivity.extractAmount("0,10"));
      assertEquals("0.10", SettingsActivity.extractAmount("0,100"));
      assertEquals("0.00", SettingsActivity.extractAmount("0.0001"));
      assertEquals("1234.00", SettingsActivity.extractAmount("1234.0001"));

   }
}
