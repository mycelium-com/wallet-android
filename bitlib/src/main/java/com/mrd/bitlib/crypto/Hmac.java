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
}
