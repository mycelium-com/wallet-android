/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet;


import junit.framework.TestCase;

import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mycelium.wallet.Utils;


// you should be able to run this tests using "gradle connectedInstrumentTest" or "gradle cIT"
// in the mbw folder

public class UtilsTest extends TestCase {

   public void testIsValidBitcoinDecimalNumber() throws Exception {
      // Succeed on sane number
      assertTrue(Utils.isValidBitcoinDecimalNumber("10.12345678", Denomination.BTC));

      // Check number of decimals
      assertTrue(Utils.isValidBitcoinDecimalNumber("0.00000001", Denomination.BTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.000000001", Denomination.BTC));
      assertTrue(Utils.isValidBitcoinDecimalNumber("0.00001", Denomination.mBTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.000001", Denomination.mBTC));
      assertTrue(Utils.isValidBitcoinDecimalNumber("0.01", Denomination.uBTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.001", Denomination.uBTC));
      
      // Fail on more than one dot
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.0.1", Denomination.BTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("..1", Denomination.BTC));

      // Fail on comma
      assertFalse(Utils.isValidBitcoinDecimalNumber("0,1", Denomination.BTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.0,1", Denomination.BTC));

      // Fail on non decimal characters
      assertFalse(Utils.isValidBitcoinDecimalNumber("X0.1", Denomination.BTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.1X", Denomination.BTC));
      assertFalse(Utils.isValidBitcoinDecimalNumber("0.X1", Denomination.BTC));

      // Fail on negative
      assertFalse(Utils.isValidBitcoinDecimalNumber("-1", Denomination.BTC));
      
   }

   public void testIsValidFiatDecimalNumber() throws Exception {
      // Succeed on sane number
      assertTrue(Utils.isValidFiatDecimalNumber("10.12"));

      // Check number of decimals
      assertTrue(Utils.isValidFiatDecimalNumber("0.01"));
      assertFalse(Utils.isValidFiatDecimalNumber("0.001"));
      
      // Fail on more than one dot
      assertFalse(Utils.isValidFiatDecimalNumber("0.0.1"));
      assertFalse(Utils.isValidFiatDecimalNumber("..1"));

      // Fail on comma
      assertFalse(Utils.isValidFiatDecimalNumber("0,1"));
      assertFalse(Utils.isValidFiatDecimalNumber("0.0,1"));

      // Fail on non decimal characters
      assertFalse(Utils.isValidFiatDecimalNumber("X0.1"));
      assertFalse(Utils.isValidFiatDecimalNumber("0.1X"));
      assertFalse(Utils.isValidFiatDecimalNumber("0.X1"));

      // Fail on negative
      assertFalse(Utils.isValidFiatDecimalNumber("-1"));
      
   }

}
