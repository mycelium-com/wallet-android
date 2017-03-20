/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
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

package com.mrd.bitlib.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;

public class Hmac {

   private static final String SHA256 = "SHA-256";
   private static final String SHA512 = "SHA-512";
   private static final int SHA256_BLOCK_SIZE = 64;
   private static final int SHA512_BLOCK_SIZE = 128;

   public static byte[] hmacSha256(byte[] key, byte[] message) {
      MessageDigest digest;
      try {
         digest = MessageDigest.getInstance(SHA256);
      } catch (NoSuchAlgorithmException e) {
         // Only happens if the platform does not support SHA-256
         throw new RuntimeException(e);
      }
      return hmac(digest, SHA256_BLOCK_SIZE, key, message);
   }

   public static byte[] hmacSha512(byte[] key, byte[] message) {
      MessageDigest digest;
      try {
         digest = MessageDigest.getInstance(SHA512);
      } catch (NoSuchAlgorithmException e) {
         // Only happens if the platform does not support SHA-512
         throw new RuntimeException(e);
      }
      return hmac(digest, SHA512_BLOCK_SIZE, key, message);
   }

   private static byte[] hmac(MessageDigest digest, int blockSize, byte[] key, byte[] message) {

      // Ensure sufficient key length
      if (key.length > blockSize) {
         key = hash(digest, key);
      }
      if (key.length < blockSize) {
         // Zero pad
         byte[] temp = new byte[blockSize];
         System.arraycopy(key, 0, temp, 0, key.length);
         key = temp;
      }

      // Prepare o key pad
      byte[] o_key_pad = new byte[blockSize];
      for (int i = 0; i < blockSize; i++) {
         o_key_pad[i] = (byte) (0x5c ^ key[i]);
      }

      // Prepare i key pad
      byte[] i_key_pad = new byte[blockSize];
      for (int i = 0; i < blockSize; i++) {
         i_key_pad[i] = (byte) (0x36 ^ key[i]);
      }

      return hash(digest, o_key_pad, hash(digest, i_key_pad, message));
   }

   private static byte[] hash(MessageDigest digest, byte[] data) {
      digest.reset();
      digest.update(data, 0, data.length);
      return digest.digest();
   }

   private static byte[] hash(MessageDigest digest, byte[] data1, byte[] data2) {
      digest.reset();
      digest.update(data1, 0, data1.length);
      digest.update(data2, 0, data2.length);
      return digest.digest();
   }

   /**
    * Run test vectors from RFC-4231
    * 
    * @return true iff the tests succeed
    */
   public static boolean testTestVectors() {
      byte[] key, data, expected_256, expected_512, result_256, result_512;

      // Test case 1
      key = HexUtils.toBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
      data = HexUtils.toBytes("4869205468657265");
      expected_256 = HexUtils.toBytes("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7");
      expected_512 = HexUtils
            .toBytes("87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cdedaa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854");
      result_256 = Hmac.hmacSha256(key, data);
      result_512 = Hmac.hmacSha512(key, data);
      if (!BitUtils.areEqual(result_256, expected_256) || !BitUtils.areEqual(result_512, expected_512)) {
         return false;
      }

      // Test case 2
      key = HexUtils.toBytes("4a656665");
      data = HexUtils.toBytes("7768617420646f2079612077616e7420666f72206e6f7468696e673f");
      expected_256 = HexUtils.toBytes("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
      expected_512 = HexUtils
            .toBytes("164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737");
      result_256 = Hmac.hmacSha256(key, data);
      result_512 = Hmac.hmacSha512(key, data);
      if (!BitUtils.areEqual(result_256, expected_256) || !BitUtils.areEqual(result_512, expected_512)) {
         return false;
      }

      // Test case 3
      key = HexUtils.toBytes("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      data = HexUtils
            .toBytes("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
      expected_256 = HexUtils.toBytes("773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe");
      expected_512 = HexUtils
            .toBytes("fa73b0089d56a284efb0f0756c890be9b1b5dbdd8ee81a3655f83e33b2279d39bf3e848279a722c806b485a47e67c807b946a337bee8942674278859e13292fb");
      result_256 = Hmac.hmacSha256(key, data);
      result_512 = Hmac.hmacSha512(key, data);
      if (!BitUtils.areEqual(result_256, expected_256) || !BitUtils.areEqual(result_512, expected_512)) {
         return false;
      }

      return true;
   }
}
