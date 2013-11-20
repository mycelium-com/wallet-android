/*
 * Copyright 2013 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
