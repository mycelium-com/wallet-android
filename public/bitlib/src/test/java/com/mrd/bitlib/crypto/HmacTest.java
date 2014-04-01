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

package com.mrd.bitlib.crypto;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.mrd.bitlib.util.HexUtils;

public class HmacTest {

   private static final byte[] TEST_1_KEY = HexUtils.toBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
   private static final byte[] TEST_1_DATA = HexUtils.toBytes("4869205468657265");
   private static final byte[] TEST_1_RESULT = HexUtils
         .toBytes("87aa7cdea5ef619d4ff0b4241a1d6cb0" + "2379f4e2ce4ec2787ad0b30545e17cde"
               + "daa833b7d6b8a702038b274eaea3f4e4" + "be9d914eeb61f1702e696c203a126854");

   private static final byte[] TEST_2_KEY = HexUtils.toBytes("4a656665");
   private static final byte[] TEST_2_DATA = HexUtils.toBytes("7768617420646f2079612077616e7420"
         + "666f72206e6f7468696e673f");
   private static final byte[] TEST_2_RESULT = HexUtils
         .toBytes("164b7a7bfcf819e2e395fbe73b56e0a3" + "87bd64222e831fd610270cd7ea250554"
               + "9758bf75c05a994a6d034f65f8f0e6fd" + "caeab1a34d4a6b4b636e070a38bce737");

   private static final byte[] TEST_3_KEY = HexUtils.toBytes("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
   private static final byte[] TEST_3_DATA = HexUtils.toBytes("dddddddddddddddddddddddddddddddd"
         + "dddddddddddddddddddddddddddddddd" + "dddddddddddddddddddddddddddddddd" + "dddd");
   private static final byte[] TEST_3_RESULT = HexUtils
         .toBytes("fa73b0089d56a284efb0f0756c890be9" + "b1b5dbdd8ee81a3655f83e33b2279d39"
               + "bf3e848279a722c806b485a47e67c807" + "b946a337bee8942674278859e13292fb");

   @Test
   public void hmacSha512Test() throws InterruptedException {
      assertTrue(Arrays.equals(TEST_1_RESULT, Hmac.hmacSha512(TEST_1_KEY, TEST_1_DATA)));
      assertTrue(Arrays.equals(TEST_2_RESULT, Hmac.hmacSha512(TEST_2_KEY, TEST_2_DATA)));
      assertTrue(Arrays.equals(TEST_3_RESULT, Hmac.hmacSha512(TEST_3_KEY, TEST_3_DATA)));
   }

}
