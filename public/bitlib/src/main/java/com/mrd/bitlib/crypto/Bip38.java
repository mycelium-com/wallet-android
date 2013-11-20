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

package com.mrd.bitlib.crypto;

import Rijndael.Rijndael;

import com.google.bitcoinj.Base58;
import com.lambdaworks.crypto.SCrypt;
import com.lambdaworks.crypto.SCryptProgress;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;

public class Bip38 {

   private static final int SCRYPT_N = 16384;
   private static final int SCRYPT_R = 8;
   private static final int SCRYPT_P = 8;
   private static final int SCRYPT_LENGTH = 64;

   public static String encrypt(String passphrase, String base58EncodedPrivateKey, SCryptProgress progressTracker,
         NetworkParameters network) {
      InMemoryPrivateKey key = new InMemoryPrivateKey(base58EncodedPrivateKey, network);
      Address address = Address.fromStandardPublicKey(key.getPublicKey(), network);

      // Encoded result
      byte[] encoded = new byte[39 + 4];
      int index = 0;
      encoded[index++] = (byte) 0x01;
      encoded[index++] = (byte) 0x42;

      // Flags byte
      byte non_EC_multiplied = (byte) 0xC0;
      byte compressedPublicKey = key.getPublicKey().isCompressed() ? (byte) 0x20 : (byte) 0;
      encoded[index++] = (byte) (non_EC_multiplied | compressedPublicKey);

      // Salt
      byte[] salt = calculateSalt(address);
      System.arraycopy(salt, 0, encoded, index, salt.length);
      index += salt.length;

      // Derive Keys
      byte[] derivedHalf1 = new byte[32];
      byte[] derivedHalf2 = new byte[32];
      try {
         byte[] derived = SCrypt.scrypt(passphrase.getBytes("UTF-8"), salt, SCRYPT_N, SCRYPT_R, SCRYPT_P,
               SCRYPT_LENGTH, progressTracker);
         System.arraycopy(derived, 0, derivedHalf1, 0, 32);
         System.arraycopy(derived, 32, derivedHalf2, 0, 32);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      // Initialize AES key
      Rijndael aes = new Rijndael();
      aes.makeKey(derivedHalf2, 256);

      // Get private key bytes
      byte[] complete = key.getPrivateKeyBytes();

      // Insert first encrypted key part
      byte[] toEncryptPart1 = new byte[16];
      for (int i = 0; i < 16; i++) {
         toEncryptPart1[i] = (byte) ((((int) complete[i]) & 0xFF) ^ (((int) derivedHalf1[i]) & 0xFF));
      }
      byte[] encryptedHalf1 = new byte[16];
      aes.encrypt(toEncryptPart1, encryptedHalf1);
      System.arraycopy(encryptedHalf1, 0, encoded, index, encryptedHalf1.length);
      index += encryptedHalf1.length;

      // Insert second encrypted key part
      byte[] toEncryptPart2 = new byte[16];
      for (int i = 0; i < 16; i++) {
         toEncryptPart2[i] = (byte) ((((int) complete[16 + i]) & 0xFF) ^ (((int) derivedHalf1[16 + i]) & 0xFF));
      }
      byte[] encryptedHalf2 = new byte[16];
      aes.encrypt(toEncryptPart2, encryptedHalf2);
      System.arraycopy(encryptedHalf2, 0, encoded, index, encryptedHalf2.length);
      index += encryptedHalf2.length;

      // Checksum
      byte[] checkSum = HashUtils.doubleSha256(encoded, 0, 39);
      System.arraycopy(checkSum, 0, encoded, 39, 4);

      // Base58 encode
      String result = Base58.encode(encoded);
      return result;
   }

   public static boolean isBip38PrivateKey(String bip38PrivateKey) {
      // Decode Base 58
      byte[] decoded = Base58.decodeChecked(bip38PrivateKey);
      if (decoded == null) {
         return false;
      }

      // Validate length
      if (decoded.length != 39) {
         return false;
      }

      int index = 0;

      // Validate BIP 38 prefix
      if (decoded[index++] != (byte) 0x01) {
         return false;
      }
      if (decoded[index++] != (byte) 0x42) {
         return false;
      }

      // Validate flags
      int flags = ((int) decoded[index++]) & 0x00ff;
      if ((flags | 0x00E0) != 0xE0) {
         // Only bit 6 7 and 8 can be set for non-EC-multiply keys
         return false;
      }
      if ((flags & 0x00c0) != 0x00c0) {
         // Not non-EC-multiplied key
         return false;
      }
      return true;
   }

   public static String decrypt(String bip38PrivateKey, String passphrase, SCryptProgress progressTracker,
         NetworkParameters network) {
      // Decode Base 58
      byte[] decoded = Base58.decodeChecked(bip38PrivateKey);
      if (decoded == null) {
         return null;
      }

      // Validate length
      if (decoded.length != 39) {
         return null;
      }

      int index = 0;

      // Validate BIP 38 prefix
      if (decoded[index++] != (byte) 0x01) {
         return null;
      }
      if (decoded[index++] != (byte) 0x42) {
         return null;
      }

      // Validate flags and determine whether we have a compressed key
      int flags = ((int) decoded[index++]) & 0x00ff;
      if ((flags | 0x00E0) != 0xE0) {
         // Only bit 6 7 and 8 can be set for non-EC-multiply keys
         return null;
      }
      if ((flags & 0x00c0) != 0x00c0) {
         // Not non-EC-multiplied key
         return null;
      }
      boolean compressed = (flags & 0x0020) == 0 ? false : true;

      // Fetch salt
      byte[] salt = new byte[4];
      salt[0] = decoded[index++];
      salt[1] = decoded[index++];
      salt[2] = decoded[index++];
      salt[3] = decoded[index++];

      // Fetch first encrypted half
      byte[] encryptedHalf1 = new byte[16];
      System.arraycopy(decoded, index, encryptedHalf1, 0, encryptedHalf1.length);
      index += encryptedHalf1.length;

      // Fetch second encrypted half
      byte[] encryptedHalf2 = new byte[16];
      System.arraycopy(decoded, index, encryptedHalf2, 0, encryptedHalf2.length);
      index += encryptedHalf2.length;

      // Derive Keys
      byte[] derivedHalf1 = new byte[32];
      byte[] derivedHalf2 = new byte[32];
      try {
         byte[] derived = SCrypt.scrypt(passphrase.getBytes("UTF-8"), salt, SCRYPT_N, SCRYPT_R, SCRYPT_P,
               SCRYPT_LENGTH, progressTracker);
         System.arraycopy(derived, 0, derivedHalf1, 0, 32);
         System.arraycopy(derived, 32, derivedHalf2, 0, 32);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      // Initialize AES key
      Rijndael aes = new Rijndael();
      aes.makeKey(derivedHalf2, 256);

      byte[] decryptedHalf1 = new byte[16];
      aes.decrypt(encryptedHalf1, decryptedHalf1);

      byte[] decryptedHalf2 = new byte[16];
      aes.decrypt(encryptedHalf2, decryptedHalf2);

      byte[] complete = new byte[32];
      for (int i = 0; i < 16; i++) {
         complete[i] = (byte) ((((int) decryptedHalf1[i]) & 0xFF) ^ (((int) derivedHalf1[i]) & 0xFF));
         complete[i + 16] = (byte) ((((int) decryptedHalf2[i]) & 0xFF) ^ (((int) derivedHalf1[i + 16]) & 0xFF));
      }

      // Create private key
      InMemoryPrivateKey key = new InMemoryPrivateKey(complete, compressed);

      // Validate result
      Address address = Address.fromStandardPublicKey(key.getPublicKey(), network);
      byte[] newSalt = calculateSalt(address);
      if (!BitUtils.areEqual(salt, newSalt)) {
         // The passphrase is either invalid or we are on the wrong network
         return null;
      }

      // Get SIPA format
      String result = key.getBase58EncodedPrivateKey(network);
      return result;
   }

   public static SCryptProgress getScryptProgressTracker() {
      return new SCryptProgress(SCRYPT_N, SCRYPT_R, SCRYPT_P);
   }

   private static byte[] calculateSalt(Address address) {
      byte[] salt = new byte[4];
      byte[] hash = HashUtils.doubleSha256(address.toString().getBytes());
      System.arraycopy(hash, 0, salt, 0, salt.length);
      return salt;
   }
}
