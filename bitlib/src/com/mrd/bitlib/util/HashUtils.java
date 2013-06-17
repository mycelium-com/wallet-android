/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.bitlib.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.mrd.bitlib.crypto.digest.RIPEMD160Digest;

/**
 * Various hashing utilities used in the Bitcoin system.
 */
public class HashUtils {

   private static final String SHA256 = "SHA-256";

   public static byte[] sha256(byte[] data) {
      return sha256(data, 0, data.length);
   }

   public static byte[] sha256(byte[] data1, byte[] data2) {
      try {
         MessageDigest digest;
         digest = MessageDigest.getInstance(SHA256);
         digest.update(data1, 0, data1.length);
         digest.update(data2, 0, data2.length);
         return digest.digest();
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

   public static byte[] sha256(byte[] data, int offset, int length) {
      try {
         MessageDigest digest;
         digest = MessageDigest.getInstance(SHA256);
         digest.update(data, offset, length);
         return digest.digest();
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

   public static byte[] doubleSha256(byte[] data) {
      return doubleSha256(data, 0, data.length);
   }

   public static byte[] doubleSha256TwoBuffers(byte[] data1, byte[] data2) {
      try {
         MessageDigest digest;
         digest = MessageDigest.getInstance(SHA256);
         digest.update(data1, 0, data1.length);
         digest.update(data2, 0, data2.length);
         return digest.digest(digest.digest());
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

   public static byte[] doubleSha256(byte[] data, int offset, int length) {
      try {
         MessageDigest digest;
         digest = MessageDigest.getInstance(SHA256);
         digest.update(data, offset, length);
         return digest.digest(digest.digest());
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

   private static final RIPEMD160Digest ripeMD160 = new RIPEMD160Digest();

   /**
    * Calculate the RipeMd160 value of the SHA-256 of an array of bytes. This is
    * how a Bitcoin address is derived from public key bytes.
    * 
    * @param pubkeyBytes
    *           A Bitcoin public key as an array of bytes.
    * @return The Bitcoin address as an array of bytes.
    */
   public static synchronized byte[] addressHash(byte[] pubkeyBytes) {
      try {
         byte[] sha256 = MessageDigest.getInstance(SHA256).digest(pubkeyBytes);
         byte[] out = new byte[20];
         ripeMD160.update(sha256, 0, sha256.length);
         ripeMD160.doFinal(out, 0); // This also resets the hash function for
                                    // next use
         return out;
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

}
