package com.mycelium.wallet;/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

import junit.framework.TestCase;

// You can run this tests using "gradle connectedInstrumentTest" or "gradle cIT"
// in the mbw folder.
// BEWARE!!! running gradle cIT will uninstall  the app, deleting all keys!!!

public class UtilsTest extends TestCase {

   public void testTruncateAndConvertDecimalString() throws Exception {
      // Succeed on sane number
      assertEquals(Utils.truncateAndConvertDecimalString("10.12345678", 8), "10.12345678");
      assertEquals(Utils.truncateAndConvertDecimalString("5", 8), "5");
      assertEquals(Utils.truncateAndConvertDecimalString("5", 0), "5");

      // Check truncation
      assertEquals(Utils.truncateAndConvertDecimalString("0.123", 4), "0.123");
      assertEquals(Utils.truncateAndConvertDecimalString("0.123", 3), "0.123");
      assertEquals(Utils.truncateAndConvertDecimalString("0.123", 2), "0.12");
      assertEquals(Utils.truncateAndConvertDecimalString("0.123", 1), "0.1");
      assertEquals(Utils.truncateAndConvertDecimalString("0.123", 0), "0");

      // Check trim
      assertEquals(Utils.truncateAndConvertDecimalString("  0.123\t", 8), "0.123");

      // Check comma to dot conversion
      assertEquals(Utils.truncateAndConvertDecimalString("0,123", 8), "0.123");

      // Fail if no digit before separator
      assertNull(Utils.truncateAndConvertDecimalString(".1", 8));
      assertNull(Utils.truncateAndConvertDecimalString(",1", 8));

      // Fail if no digit after separator
      assertNull(Utils.truncateAndConvertDecimalString("1.", 8));
      assertNull(Utils.truncateAndConvertDecimalString("1,", 8));
      
      // Fail on more than one dot
      assertNull(Utils.truncateAndConvertDecimalString("0.0.123", 8));
      assertNull(Utils.truncateAndConvertDecimalString("..123", 8));

      // Fail on dot and comma mix
      assertNull(Utils.truncateAndConvertDecimalString("0.0,123", 8));
      assertNull(Utils.truncateAndConvertDecimalString("0,0.123", 8));

      // Fail on non decimal characters
      assertNull(Utils.truncateAndConvertDecimalString("0.X", 8));
      assertNull(Utils.truncateAndConvertDecimalString("X.1", 8));

      // Fail on negative
      assertNull(Utils.truncateAndConvertDecimalString("-1", 8));

   }

}
