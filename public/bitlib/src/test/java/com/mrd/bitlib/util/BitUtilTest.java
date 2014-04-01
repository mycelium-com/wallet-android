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

import junit.framework.Assert;

import org.junit.Test;

public class BitUtilTest {
   @Test
   public void copyOfTest() {
      byte[] original = HexUtils.toBytes("0001020304");
      byte[] original_first_three = HexUtils.toBytes("000102");
      byte[] original_with_one_zero_appended = HexUtils.toBytes("000102030400");
      byte[] empty = new byte[] {};

      // Copy All
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOf(original, original.length), original));

      // Copy nothing
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOf(original, 0), empty));
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOf(empty, 0), empty));

      // Copy first
      Assert.assertTrue(BitUtils.copyOf(original, 1)[0] == 0x00);

      // Partial copy
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOf(original, 3), original_first_three));

      // Copy one beyond last index
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOf(original, original.length + 1),
            original_with_one_zero_appended));

   }

   @Test
   public void copyOfRangeTest() {
      byte[] original = HexUtils.toBytes("0001020304");
      byte[] original_with_one_zero_appended = HexUtils.toBytes("000102030400");
      byte[] original_from_index_1_with_one_zero_appended = HexUtils.toBytes("0102030400");
      byte[] empty = new byte[] {};

      // Copy all
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(original, 0, original.length), original));

      // Copy nothing
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(original, 0, 0), empty));
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(original, 1, 1), empty));
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(original, original.length, original.length), empty));
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(empty, 0, 0), empty));

      // Copy one more than we have and expect zero truncation
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(original, 0, original.length + 1),
            original_with_one_zero_appended));

      // Copy from index 1 to one more than we have and expect truncation
      Assert.assertTrue(BitUtils.areEqual(BitUtils.copyOfRange(original, 1, original.length + 1),
            original_from_index_1_with_one_zero_appended));

      // Copy one byte from index 1
      Assert.assertTrue(BitUtils.copyOfRange(original, 1, 2)[0] == 0x01);

   }

}
