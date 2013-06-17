/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.bitlib.crypto;

import java.io.Serializable;
import java.math.BigInteger;

import com.mrd.bitlib.util.ByteWriter;

public abstract class PrivateKey implements BitcoinSigner , Serializable {

   private static final long serialVersionUID = 1L;

   public abstract PublicKey getPublicKey();

   @Override
   public byte[] makeStandardBitcoinSignature(byte[] transactionSigningHash) {
      byte[] signature = signMessage(transactionSigningHash);
      ByteWriter writer = new ByteWriter(1024);
      // Add signature
      writer.putBytes(signature);
      // Add hash type
      writer.put((byte) ((0 + 1) | 0));
      return writer.toBytes();
   }

   protected byte[] signMessage(byte[] message) {
      BigInteger[] signature = generateSignature(message);
      // Write DER encoding of signature
      ByteWriter writer = new ByteWriter(1024);
      // Write tag
      writer.put((byte) 0x30);
      // Write total length
      byte[] s1 = signature[0].toByteArray();
      byte[] s2 = signature[1].toByteArray();
      int totalLength = 2 + s1.length + 2 + s2.length;
      if (totalLength > 127) {
         // We assume that the total length never goes beyond a 1-byte
         // representation
         throw new RuntimeException("Unsupported signature length: " + totalLength);
      }
      writer.put((byte) (totalLength & 0xFF));
      // Write type
      writer.put((byte) 0x02);
      // We assume that the length never goes beyond a 1-byte representation
      writer.put((byte) (s1.length & 0xFF));
      // Write bytes
      writer.putBytes(s1);
      // Write type
      writer.put((byte) 0x02);
      // We assume that the length never goes beyond a 1-byte representation
      writer.put((byte) (s2.length & 0xFF));
      // Write bytes
      writer.putBytes(s2);
      return writer.toBytes();
   }

   protected abstract BigInteger[] generateSignature(byte[] message);

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

}
