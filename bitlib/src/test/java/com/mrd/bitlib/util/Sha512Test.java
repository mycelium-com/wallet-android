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

package com.mrd.bitlib.util;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;

public class Sha512Test {

   private static final String TEST1_STRING = "abc";
   private static final Sha512Hash TEST1_RESULT = new Sha512Hash(HexUtils.toBytes("ddaf35a193617aba"
         + "cc417349ae204131" + "12e6fa4e89a97ea2" + "0a9eeee64b55d39" + "a2192992a274fc1a8" + "36ba3c23a3feebbd"
         + "454d4423643ce80e" + "2a9ac94fa54ca49f"));

   private static final String TEST2_STRING = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
   private static final Sha512Hash TEST2_RESULT = new Sha512Hash(HexUtils.toBytes("204a8fc6dda82f0a"
         + "0ced7beb8e08a416" + "57c16ef468b228a8" + "279be331a703c335" + "96fd15c13b1b07f9" + "aa1d3bea57789ca0"
         + "31ad85c7a71dd703" + "54ec631238ca3445"));

   private static final String TEST3_STRING = "";
   private static final Sha512Hash TEST3_RESULT = new Sha512Hash(HexUtils.toBytes("cf83e1357eefb8bd"
         + "f1542850d66d8007" + "d620e4050b5715dc" + "83f4a921d36ce9ce" + "47d0d13c5d85f2b0" + "ff8318d2877eec2f"
         + "63b931bd47417a81" + "a538327af927da3e"));

   @Test
   public void testVectorsTest() {
      Assert.assertEquals(TEST1_RESULT, HashUtils.sha512(stringToBytes(TEST1_STRING)));
      Assert.assertEquals(TEST2_RESULT, HashUtils.sha512(stringToBytes(TEST2_STRING)));
      Assert.assertEquals(TEST3_RESULT, HashUtils.sha512(stringToBytes(TEST3_STRING)));
   }

   private byte[] stringToBytes(String string) {
      try {
         return string.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException();
      }
   }

}
