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

package com.mycelium.lt;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import Rijndael.Rijndael;

import com.google.common.io.BaseEncoding;
import com.mrd.bitlib.crypto.Ecdh;
import com.mrd.bitlib.crypto.Hmac;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChatMessageEncryptionKey implements Serializable {
   private static final long serialVersionUID = 1L;

   /**
    * Exception thrown when failing to decrypt and checking the integrity an
    * encrypted chat message
    */
   public static class InvalidChatMessage extends Exception {
      private static final long serialVersionUID = 1L;

      public InvalidChatMessage(String message) {
         super(message);
      }
   }

   public final byte[] encryptionKey;
   public final byte[] hmacKey;

   private ChatMessageEncryptionKey(byte[] aesKey, byte[] hmacKey) {
      this.encryptionKey = aesKey;
      this.hmacKey = hmacKey;
   }

   /**
    * Construct a chat message encryption key using Diffie-Hellman for Elliptic
    * curves
    * 
    * @param foreignPublicKey
    *           the public key of the peer to generate an encryption key for
    * @param privateKey
    *           your own private key to generate an encryption key for
    * @param tradeSessionId
    *           The trade session ID to generate an encryption key for
    * @return an encryption key that is shared with the peer
    */
   public static ChatMessageEncryptionKey fromEcdh(PublicKey foreignPublicKey, InMemoryPrivateKey privateKey,
         UUID tradeSessionId) {
      byte[] sharedSecret = Ecdh.calculateSharedSecret(
              checkNotNull(foreignPublicKey),
              checkNotNull(privateKey));
      ByteWriter writer = new ByteWriter(sharedSecret.length + 8 + 8);
      writer.putLongBE(tradeSessionId.getMostSignificantBits());
      writer.putLongBE(tradeSessionId.getLeastSignificantBits());
      writer.putBytes(sharedSecret);
      byte[] keyMaterial = HashUtils.sha256(writer.toBytes()).getBytes();
      byte[] encryptionKey = BitUtils.copyOfRange(keyMaterial, 0, 16);
      byte[] hmacKey = BitUtils.copyOfRange(keyMaterial, 16, 32);
      return new ChatMessageEncryptionKey(encryptionKey, hmacKey);
   }

   @Override
   public int hashCode() {
      return (int) BitUtils.uint32ToLong(encryptionKey, 0) + (int) BitUtils.uint32ToLong(hmacKey, 0);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof ChatMessageEncryptionKey)) {
         return false;
      }
      ChatMessageEncryptionKey other = (ChatMessageEncryptionKey) obj;
      return BitUtils.areEqual(encryptionKey, other.encryptionKey) && BitUtils.areEqual(hmacKey, other.hmacKey);
   }

   /**
    * Encrypt a chat message and add integrity check using an AES key and HMAC
    * key.
    * 
    * @param message
    *           The message to encrypt
    * @return An encrypted message
    */
   public String encryptChatMessage(String message) {
      // Get the UTF-8 byte encoding of the message
      byte[] messageBytes = utf8StringToBytes(message);

      // Calculate MAC value, linking this message with the trade session
      byte[] mac = calculateHmac(messageBytes);

      // Prepare message for encryption
      ByteWriter writer = new ByteWriter(messageBytes.length + 4);
      writer.putIntLE(messageBytes.length);
      writer.putBytes(messageBytes);

      // Encrypt message using the MAC value as IV
      byte[] IV = BitUtils.copyOf(mac, Rijndael.BLOCK_SIZE);
      byte[] enc = aesCbcEncryption(IV, writer.toBytes());

      // Concatenate the MAC value and the encrypted message
      byte[] concatenated = new byte[mac.length + enc.length];
      System.arraycopy(mac, 0, concatenated, 0, mac.length);
      System.arraycopy(enc, 0, concatenated, mac.length, enc.length);

      // Base-64 encode without padding
      return BaseEncoding.base64().omitPadding().encode(concatenated);
   }

   /**
    * Decrypt and verify the integrity of an encrypted chat message
    * 
    * @param encryptedChatMessage
    *           the encrypted message
    * @return the plaintext message
    * @throws InvalidChatMessage
    *            if the message fails integrity checks
    */
   public String decryptAndCheckChatMessage(String encryptedChatMessage) throws InvalidChatMessage {
      // Base-64 decode without padding
      byte[] encryptedMessageBytes;
      try {
         encryptedMessageBytes = BaseEncoding.base64().omitPadding().decode(encryptedChatMessage);
      } catch (IllegalArgumentException e) {
         throw new InvalidChatMessage("Invalid Base-64 encoding");
      }

      // Extract MAC and encrypted bytes
      byte[] mac = BitUtils.copyOf(encryptedMessageBytes, MAC_LENGTH);
      byte[] encryptedBytes = BitUtils.copyOfRange(encryptedMessageBytes, MAC_LENGTH, encryptedMessageBytes.length);

      // Decrypt message using the MAC value as IV
      byte[] IV = BitUtils.copyOf(mac, Rijndael.BLOCK_SIZE);
      byte[] decryptedMessage = aesDecrypt(IV, encryptedBytes);

      // Get message bytes
      ByteReader reader = new ByteReader(decryptedMessage);
      byte[] messageBytes;
      try {
         int messageByteSize = reader.getIntLE();
         if (messageByteSize < 0 || messageByteSize > encryptedMessageBytes.length) {
            throw new InvalidChatMessage("Invalid chat message size");
         }
         messageBytes = reader.getBytes(messageByteSize);
      } catch (InsufficientBytesException e) {
         throw new InvalidChatMessage("Invalid chat message size");
      }

      // Validate MAC
      byte[] calculatedMac = calculateHmac(messageBytes);
      if (!BitUtils.areEqual(calculatedMac, mac)) {
         throw new InvalidChatMessage("Message integrity check failed");
      }

      // Get the string
      return bytesToUtf8String(messageBytes);
   }

   private byte[] aesDecrypt(byte[] IV, byte[] encryptedBytes) {
      // Prepare AES key
      Rijndael aes = new Rijndael();
      aes.makeKey(encryptionKey, encryptionKey.length * 8, Rijndael.DIR_DECRYPT);

      // Decrypt in CBC mode using IV
      byte[] pt = new byte[Rijndael.BLOCK_SIZE];
      byte[] ct;
      byte[] parentBlock = IV;
      ByteWriter writer = new ByteWriter(encryptedBytes.length);
      for (int i = 0; i < encryptedBytes.length; i += Rijndael.BLOCK_SIZE) {
         ct = BitUtils.copyOfRange(encryptedBytes, i, i + Rijndael.BLOCK_SIZE);
         aes.decrypt(ct, pt);
         xorBytes(parentBlock, pt);
         parentBlock = ct;
         writer.putBytes(pt);
      }
      return writer.toBytes();
   }

   private byte[] aesCbcEncryption(byte[] IV, byte[] data) {
      // Prepare AES key
      Rijndael aes = new Rijndael();
      aes.makeKey(encryptionKey, encryptionKey.length * 8, Rijndael.DIR_ENCRYPT);

      // Encrypt in CBC mode using IV
      byte[] pt;
      byte[] ct = new byte[Rijndael.BLOCK_SIZE];
      byte[] parentBlock = IV;
      ByteWriter writer = new ByteWriter(data.length + Rijndael.BLOCK_SIZE);
      for (int i = 0; i < data.length; i += Rijndael.BLOCK_SIZE) {
         pt = BitUtils.copyOfRange(data, i, i + Rijndael.BLOCK_SIZE);
         xorBytes(parentBlock, pt);
         aes.encrypt(pt, ct);
         parentBlock = ct;
         writer.putBytes(ct);
      }
      return writer.toBytes();
   }

   private static void xorBytes(byte[] toApply, byte[] target) {
      if (toApply.length != target.length) {
         throw new RuntimeException();
      }
      for (int i = 0; i < toApply.length; i++) {
         target[i] = (byte) (target[i] ^ toApply[i]);
      }
   }

   private static final int MAC_LENGTH = 128 / 8;

   /**
    * Calculate the HMAC-SHA256 value of a message, and return the first 16
    * bytes
    */
   private byte[] calculateHmac(byte[] messageBytes) {
      byte[] hmac = Hmac.hmacSha256(hmacKey, messageBytes);
      // We use the first half of the H-MAC value as our MAC value
      return BitUtils.copyOf(hmac, MAC_LENGTH);
   }

   private static byte[] utf8StringToBytes(String string) {
      try {
         return string.getBytes("UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException();
      }
   }

   private static String bytesToUtf8String(byte[] stringBytes) {
      try {
         return new String(stringBytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException();
      }
   }
}
