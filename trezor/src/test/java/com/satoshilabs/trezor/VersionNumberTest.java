package com.satoshilabs.trezor;

import com.satoshilabs.trezor.lib.ExternalSignatureDevice.VersionNumber;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionNumberTest {
   @Test
   public void testIsNewerThan() {
      VersionNumber referenceVersion = new VersionNumber(1, 3, 8);

      VersionNumber newerVersion = new VersionNumber(1, 4, 0);
      VersionNumber currentVersion = new VersionNumber(1, 3, 8);
      VersionNumber oldVersion = new VersionNumber(1, 3, 6);

      assertFalse(isNewerThan(referenceVersion, newerVersion));
      assertFalse(isNewerThan(referenceVersion, currentVersion));
      assertTrue(isNewerThan(referenceVersion, oldVersion));
   }

   private boolean isNewerThan(VersionNumber left, VersionNumber right) {
      return left.isNewerThan(right.major, right.minor, right.patch);
   }
}
