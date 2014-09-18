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

package com.mrd.bitlib.util;

import com.mrd.bitlib.crypto.digest.RIPEMD160Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Various hashing utilities used in the Bitcoin system.
 */
public class HashUtils {

   private static final String SHA256 = "SHA-256";
   private static final String SHA512 = "SHA-512";

   public static Sha256Hash sha256(byte[] data) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data, 0, data.length);
      return Sha256Hash.of(digest.digest());
   }

   public static Sha256Hash sha256(byte[] data1, byte[] data2) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data1, 0, data1.length);
      digest.update(data2, 0, data2.length);
      return new Sha256Hash(digest.digest());

   }

   private static MessageDigest getSha256Digest() {
      try {
         return MessageDigest.getInstance(SHA256);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); //cannot happen
      }
   }

   public static Sha256Hash doubleSha256(byte[] data) {
      return doubleSha256(data, 0, data.length);
   }

   public static Sha256Hash doubleSha256TwoBuffers(byte[] data1, byte[] data2) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data1, 0, data1.length);
      digest.update(data2, 0, data2.length);
      return new Sha256Hash(digest.digest(digest.digest()));

   }

   public static Sha256Hash doubleSha256(byte[] data, int offset, int length) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data, offset, length);
      return new Sha256Hash(digest.digest(digest.digest()));
   }

   public static Sha512Hash sha512(byte[] data) {
      MessageDigest digest;
      digest = getSha512Digest();
      digest.update(data, 0, data.length);
      return Sha512Hash.of(digest.digest());
   }

   public static Sha256Hash sha256(byte[] data, int offset, int length) {
      MessageDigest digest;
      digest = getSha256Digest();
      digest.update(data, offset, length);
      return new Sha256Hash(digest.digest());
   }

   public static Sha512Hash sha512(byte[] data1, byte[] data2) {
      MessageDigest digest;
      digest = getSha512Digest();
      digest.update(data1, 0, data1.length);
      digest.update(data2, 0, data2.length);
      return new Sha512Hash(digest.digest());

   }

   private static MessageDigest getSha512Digest() {
      try {
         return MessageDigest.getInstance(SHA512);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); //cannot happen
      }
   }

   /**
    * Calculate the RipeMd160 value of the SHA-256 of an array of bytes. This is
    * how a Bitcoin address is derived from public key bytes.
    *
    * @param pubkeyBytes A Bitcoin public key as an array of bytes.
    * @return The Bitcoin address as an array of bytes.
    */
   public static byte[] addressHash(byte[] pubkeyBytes) {
      byte[] sha256 = getSha256Digest().digest(pubkeyBytes);
      byte[] out = new byte[20];
      RIPEMD160Digest ripeMD160 = new RIPEMD160Digest();
      ripeMD160.update(sha256, 0, sha256.length);
      ripeMD160.doFinal(out, 0); // This also resets the hash function for
      // next use
      return out;
   }
}
