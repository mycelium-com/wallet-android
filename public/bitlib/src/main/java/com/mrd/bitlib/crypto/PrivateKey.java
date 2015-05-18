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

import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;


public abstract class PrivateKey implements BitcoinSigner, Serializable {

   private static final long serialVersionUID = 1L;

   public abstract PublicKey getPublicKey();

   @Override
   public byte[] makeStandardBitcoinSignature(Sha256Hash transactionSigningHash) {
      byte[] signature = signMessage(transactionSigningHash);
      ByteWriter writer = new ByteWriter(1024);
      // Add signature
      writer.putBytes(signature);
      // Add hash type
      writer.put((byte) ((0 + 1) | 0)); // move to constant to document
      return writer.toBytes();
   }

   protected byte[] signMessage(Sha256Hash message) {
      return generateSignature(message).derEncode();
   }

   // Sign the message with a signature based random k-Value
   protected abstract Signature generateSignature(Sha256Hash message, RandomSource randomSource);

   // Sign the message deterministic, according to rfc6979
   protected abstract Signature generateSignature(Sha256Hash message);

   @Override
   public int hashCode() {
      return getPublicKey().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PrivateKey)) {
         return false;
      }
      PrivateKey other = (PrivateKey) obj;
      return getPublicKey().equals(other.getPublicKey());
   }

   public SignedMessage signMessage(String message) {
      byte[] data = Signatures.formatMessageForSigning(message);
      Sha256Hash hash = HashUtils.doubleSha256(data);
      return signHash(hash);
   }

   public SignedMessage signHash(Sha256Hash hashToSign) {
      Signature sig = generateSignature(hashToSign);

      // Now we have to work backwards to figure out the recId needed to recover the signature.
      PublicKey targetPubKey = getPublicKey();
      boolean compressed = targetPubKey.isCompressed();
      int recId = -1;
      for (int i = 0; i < 4; i++) {

         PublicKey k = SignedMessage.recoverFromSignature(i, sig, hashToSign, compressed);
         if (k != null && targetPubKey.equals(k)) {
            recId = i;
            break;
         }
      }
      return SignedMessage.from(sig, targetPubKey, recId);
//      return Base64.encodeToString(sigData,false);

   }

}
