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

import java.util.UUID;

import org.junit.Test;

import com.google.common.io.BaseEncoding;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.lt.ChatMessageEncryptionKey.InvalidChatMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MessageEncryptionTest {
   private static final byte[] MASTER_SEED = HexUtils.toBytes("000102030405060708090a0b0c0d0e0f");

   private static final String MESSAGE_ONE = "Hello World!";
   private static final String MESSAGE_TWO = "";
   private static final String MESSAGE_THREE = "Hello World!Hello World!Hello World!Hello World!Hello World!Hello World!Hello World!Hello World!";

   @Test
   public void testEncryptionDecryptionPositive() {
      // Generate random trade session ID, encryption key and derive HMAC key
      UUID tradeSessionId = UUID.randomUUID();

      // Create some private/public keys
      HdKeyNode root = HdKeyNode.fromSeed(MASTER_SEED);
      // My private key
      InMemoryPrivateKey myPrv = root.createChildPrivateKey(0);
      // Foreign public key
      PublicKey foreignPub = root.createChildPublicKey(1);

      ChatMessageEncryptionKey encryptionKey = ChatMessageEncryptionKey.fromEcdh(foreignPub, myPrv, tradeSessionId);

      checkEncryptDecrypt(MESSAGE_ONE, encryptionKey);
      checkEncryptDecrypt(MESSAGE_TWO, encryptionKey);
      checkEncryptDecrypt(MESSAGE_THREE, encryptionKey);
   }

   @Test
   public void testEncryptionDecryptionNegative() {
      // Generate random trade session ID, encryption key and derive HMAC key
      UUID tradeSessionId = UUID.randomUUID();

      // Create some private/public keys
      HdKeyNode root = HdKeyNode.fromSeed(MASTER_SEED);
      // My private key
      InMemoryPrivateKey myPrv = root.createChildPrivateKey(0);
      // Foreign public key
      PublicKey foreignPub = root.createChildPublicKey(1);
      // Some other private key
      InMemoryPrivateKey somePrv = root.createChildPrivateKey(2);

      // Create encryption key and encrypt a message
      ChatMessageEncryptionKey encryptionKey = ChatMessageEncryptionKey.fromEcdh(foreignPub, myPrv, tradeSessionId);
      String emsg1 = encryptionKey.encryptChatMessage(MESSAGE_THREE);

      // Try to decrypt with key generated from wrong foreign public key
      ChatMessageEncryptionKey wrongEncryptionKey1 = ChatMessageEncryptionKey.fromEcdh(somePrv.getPublicKey(), myPrv,
            tradeSessionId);
      checkFailDecrypt(emsg1, wrongEncryptionKey1);

      // Try to decrypt with key generated from wrong private key
      ChatMessageEncryptionKey wrongEncryptionKey2 = ChatMessageEncryptionKey.fromEcdh(foreignPub, somePrv,
            tradeSessionId);
      checkFailDecrypt(emsg1, wrongEncryptionKey2);

      // Try to decrypt with key generated from wrong trade session ID
      ChatMessageEncryptionKey wrongEncryptionKey3 = ChatMessageEncryptionKey.fromEcdh(foreignPub, myPrv,
            UUID.randomUUID());
      checkFailDecrypt(emsg1, wrongEncryptionKey3);

      // Flip every single bit in the encrypted chat message one by one and
      // check that it fails decryption
      byte[] data = BaseEncoding.base64().omitPadding().decode(emsg1);
      for (int i = 0; i < data.length * 8; i++) {
         byte[] mutatedData = flipBit(i, data);
         String mutatedString = BaseEncoding.base64().omitPadding().encode(mutatedData);
         checkFailDecrypt(mutatedString, encryptionKey);
      }
   }

   private byte[] flipBit(int bitNum, byte[] data) {
      byte[] copy = BitUtils.copyOf(data, data.length);
      int index = bitNum >> 3;
      int bit = bitNum % 8;
      copy[index] = (byte) (copy[index] ^ (1 << bit));
      return copy;
   }

   private void checkFailDecrypt(String encryptedMessage, ChatMessageEncryptionKey encryptionKey) {
      try {
         encryptionKey.decryptAndCheckChatMessage(encryptedMessage);
         fail("Should fail decryption");
      } catch (InvalidChatMessage e) {
         // Expected
      }
   }

   private void checkEncryptDecrypt(String message, ChatMessageEncryptionKey encryptionKey) {
      String emsg1 = encryptionKey.encryptChatMessage(message);
      String dmsg1;
      try {
         dmsg1 = encryptionKey.decryptAndCheckChatMessage(emsg1);
         assertEquals(dmsg1, message);
      } catch (InvalidChatMessage e) {
         fail(e.getMessage());
      }
   }

}
