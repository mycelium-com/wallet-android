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

package com.mycelium.wapi.wallet;

import Rijndael.Rijndael;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.util.*;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

import java.io.UnsupportedEncodingException;

// XXX This needs proper testing.

/**
 * Encrypting/decrypting arbitrary data using AES in CBC-mode with zero padding and Sha256 hashing for check sums.
 */
public class AesKeyCipher implements KeyCipher {

   public static final int AES_KEY_BYTE_LENGTH = 16;
   private static final AesKeyCipher defaultKeyCipher =
           new AesKeyCipher(HexUtils.toBytes("a8105c3c8b75556a9099b8dcab9cc133"), -3564501270110218910L);
   private final byte[] _keyBytes;
   private final Rijndael _aes;
   private final long _thumbprint;

   /**
    * The default key cipher, which gives no kind of protection at all.
    *
    * @return the default key cipher, which gives no kind of protection at all.
    */
   public static AesKeyCipher defaultKeyCipher() {
      return defaultKeyCipher;
   }

   public AesKeyCipher(String password) {
      // Hash password
      Sha256Hash hash;
      try {
         hash = HashUtils.sha256(password.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
         // Never happens
         throw new RuntimeException();
      }

      // Get key bytes, the first 16 bytes of the hash
      _keyBytes = new byte[AES_KEY_BYTE_LENGTH];
      System.arraycopy(hash.getBytes(), 0, _keyBytes, 0, AES_KEY_BYTE_LENGTH);

      // Get thumbprint, the next 8 bytes of the hash
      _thumbprint = BitUtils.uint64ToLong(hash.getBytes(), AES_KEY_BYTE_LENGTH);

      _aes = new Rijndael();
      _aes.makeKey(_keyBytes, AES_KEY_BYTE_LENGTH * 8);
   }

   /**
    * Construct a key from an array of 16 bytes
    *
    * @param keyBytes must be an array of 16 bytes
    */
   public AesKeyCipher(byte[] keyBytes) {
      this(keyBytes, 0);
   }

   private AesKeyCipher(byte[] keyBytes, long thumbprint) {
      Preconditions.checkArgument(keyBytes.length == AES_KEY_BYTE_LENGTH);
      _keyBytes = BitUtils.copyByteArray(keyBytes);
      _thumbprint = thumbprint;
      _aes = new Rijndael();
      _aes.makeKey(_keyBytes, AES_KEY_BYTE_LENGTH * 8);
   }

   @Override
   public byte[] decrypt(byte[] data) throws InvalidKeyCipher {
      // data to decrypt must be a whole number of blocks and have room for a
      // checksum
      Preconditions.checkArgument(data.length >= Rijndael.BLOCK_SIZE);
      Preconditions.checkArgument(data.length % Rijndael.BLOCK_SIZE == 0);

      // Decrypt
      ByteWriter writer = new ByteWriter(data.length - Rijndael.BLOCK_SIZE);
      ByteReader reader = new ByteReader(BitUtils.copyOf(data, data.length - Rijndael.BLOCK_SIZE));
      byte[] IV = new byte[Rijndael.BLOCK_SIZE];
      byte[] plainBlock = new byte[Rijndael.BLOCK_SIZE];
      while (reader.available() > 0) {
         byte[] cipherBlock;
         try {
            cipherBlock = reader.getBytes(Rijndael.BLOCK_SIZE);
         } catch (InsufficientBytesException e) {
            // Does not happen as we already checked that the input is a whole
            // number of blocks
            throw new RuntimeException(e);
         }
         _aes.decrypt(cipherBlock, plainBlock);
         xorBytes(IV, plainBlock);
         writer.putBytes(plainBlock);
         IV = cipherBlock;
      }

      // Strip padding
      byte[] plaintext = writer.toBytes();
      try {
         plaintext = stripPadding(plaintext);
      } catch (InsufficientBytesException e) {
         throw new InvalidKeyCipher();
      }

      // Calculate checksum
      byte[] checksum = HashUtils.sha256(plaintext).getBytes();

      // Verify checksum
      for (int i = 0; i < Rijndael.BLOCK_SIZE; i++) {
         if (checksum[i] != data[data.length - Rijndael.BLOCK_SIZE + i]) {
            throw new InvalidKeyCipher();
         }
      }
      return plaintext;
   }

   private byte[] addPadding(byte[] data) {
      ByteWriter writer = new ByteWriter(data.length + 1 + Rijndael.BLOCK_SIZE);
      writer.putCompactInt(data.length);
      writer.putBytes(data);
      int excess = writer.length() % Rijndael.BLOCK_SIZE;
      if (excess == 0) {
         return writer.toBytes();
      }
      return BitUtils.copyOf(writer.toBytes(), writer.length() + Rijndael.BLOCK_SIZE - excess);
   }

   private byte[] stripPadding(byte[] data) throws InsufficientBytesException {
      ByteReader reader = new ByteReader(data);
      int length = (int) reader.getCompactInt();
      if (length < 0) {
         throw new InsufficientBytesException();
      }
      return reader.getBytes(length);
   }

   @Override
   public byte[] encrypt(byte[] data) {
      // Data to encrypt must be a whole number of blocks, add padding
      byte[] plaintext = addPadding(data);

      ByteWriter writer = new ByteWriter(plaintext.length + Rijndael.BLOCK_SIZE);
      ByteReader reader = new ByteReader(plaintext);
      byte[] IV = new byte[Rijndael.BLOCK_SIZE];
      byte[] cipherBlock = new byte[Rijndael.BLOCK_SIZE];
      while (reader.available() > 0) {
         byte[] plainBlock;
         try {
            plainBlock = reader.getBytes(Rijndael.BLOCK_SIZE);
         } catch (InsufficientBytesException e) {
            // Does not happen as we already checked that the input is a whole
            // number of blocks
            throw new RuntimeException(e);
         }
         xorBytes(IV, plainBlock);
         _aes.encrypt(plainBlock, cipherBlock);
         writer.putBytes(cipherBlock);
         IV = cipherBlock;
      }
      // Calculate checksum
      byte[] checksum = HashUtils.sha256(data).getBytes();
      writer.putBytes(checksum, 0, Rijndael.BLOCK_SIZE);
      return writer.toBytes();
   }

   private static void xorBytes(byte[] toApply, byte[] target) {
      for (int i = 0; i < toApply.length; i++) {
         target[i] = (byte) (target[i] ^ toApply[i]);
      }
   }

   @Override
   public long getThumbprint() {
      return _thumbprint;
   }

   public byte[] getKeyBytes() {
      return _keyBytes;
   }

}
