package com.mrd.mbwapi.impl.shorten;

import com.google.common.base.Optional;
import org.junit.Test;

import com.mrd.bitlib.model.Address;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BtcToTest {
   @Test
   public void testQuery() throws Exception {
      BtcTo test = new BtcTo();
      Optional<Address> res = test.query("11");
      assertTrue(res.isPresent());
      assertEquals(Address.fromString("1GsFNGcThqVQNwfRjMrxwnCuc64toqY7ax"), res.get());
   }
}
