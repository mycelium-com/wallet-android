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

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.crypto.WrongSignatureException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ApiUtils {

   private static final String SIGNATURE_PREFIX = "Mycelium Local Trader:";

   public static boolean validateUuidHashSignature(Address address, UUID uuid, String signatureBase64) {
      if (address == null || uuid == null || signatureBase64 == null) {
         return false;
      }
      String message = uuidToMessage(uuid);
      try {
         SignedMessage.validate(address, message, signatureBase64);
      } catch (WrongSignatureException e) {
         return false;
      }
      return true;
   }

   private static String uuidToMessage(UUID uuid) {
      byte[] uuidBytes = uuidToBytes(uuid);
      Sha256Hash uuidHash = HashUtils.doubleSha256(uuidBytes);
      return new StringBuilder().append(SIGNATURE_PREFIX).append(uuidHash.toHex()).toString();
   }

   public static String generateUuidHashSignature(InMemoryPrivateKey key, UUID uuid) {
      return key.signMessage(uuidToMessage(uuid)).getBase64Signature();
   }

   protected static byte[] uuidToBytes(UUID uuid) {
      ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
      DataOutputStream da = new DataOutputStream(ba);
      try {
         da.writeLong(uuid.getMostSignificantBits());
         da.writeLong(uuid.getLeastSignificantBits());
      } catch (IOException e) {
         // Never happens
      }
      return ba.toByteArray();
   }


}
