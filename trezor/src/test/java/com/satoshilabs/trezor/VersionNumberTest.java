package com.satoshilabs.trezor;

import junit.framework.TestCase;

public class VersionNumberTest extends TestCase {
   public void testIsNewerThan() throws Exception {
      final ExternalSignatureDevice.VersionNumber referenceVersion = new ExternalSignatureDevice.VersionNumber(1, 3, 8);

      final ExternalSignatureDevice.VersionNumber newerVersion = new ExternalSignatureDevice.VersionNumber(1, 4, 0);
      final ExternalSignatureDevice.VersionNumber currentVersion = new ExternalSignatureDevice.VersionNumber(1, 3, 8);
      final ExternalSignatureDevice.VersionNumber oldVersion = new ExternalSignatureDevice.VersionNumber(1, 3, 6);

      assertEquals(false, referenceVersion.isNewerThan(newerVersion.major, newerVersion.minor, newerVersion.patch));
      assertEquals(false, referenceVersion.isNewerThan(currentVersion.major, currentVersion.minor, currentVersion.patch));
      assertEquals(true, referenceVersion.isNewerThan(oldVersion.major, oldVersion.minor, oldVersion.patch));
   }

}