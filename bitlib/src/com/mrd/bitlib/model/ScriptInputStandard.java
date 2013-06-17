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

package com.mrd.bitlib.model;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

public class ScriptInputStandard extends ScriptInput {
   private static final long serialVersionUID = 1L;

   private byte[] _signature;
   private byte[] _publicKeyBytes;

   public ScriptInputStandard(byte[] signature, byte[] publicKeyBytes) {
      super(scriptEncodeChunks(new byte[][] { signature, publicKeyBytes }));
      _signature = signature;
      _publicKeyBytes = publicKeyBytes;
   }

   protected ScriptInputStandard(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
      _signature = chunks[0];
      _publicKeyBytes = chunks[1];
   }

   protected static boolean isScriptInputStandard(byte[][] chunks) throws ScriptParsingException {
      try {
         if (chunks.length != 2) {
            return false;
         }

         // Verify that first chunk contains two DER encoded BigIntegers
         ByteReader reader = new ByteReader(chunks[0]);

         // Read tag, must be 0x30
         if ((((int) reader.get()) & 0xFF) != 0x30) {
            return false;
         }

         // Read total length as a byte, standard inputs never get longer than
         // this
         int length = ((int) reader.get()) & 0xFF;

         // Read first type, must be 0x02
         if ((((int) reader.get()) & 0xFF) != 0x02) {
            return false;
         }

         // Read first length
         int length1 = ((int) reader.get()) & 0xFF;
         reader.skip(length1);

         // Read second type, must be 0x02
         if ((((int) reader.get()) & 0xFF) != 0x02) {
            return false;
         }

         // Read second length
         int length2 = ((int) reader.get()) & 0xFF;
         reader.skip(length2);

         // Validate that the lengths add up to the total
         if (2 + length1 + 2 + length2 != length) {
            return false;
         }

         // Make sure that we have a hash type at the end
         if (reader.available() != 1) {
            return false;
         }

         // XXX we may want to add more checks to verify public key length in
         // second chunk
         return true;
      } catch (InsufficientBytesException e) {
         throw new ScriptParsingException("Unable to parse " + ScriptInputStandard.class.getSimpleName());
      }
   }

   /**
    * Get the signature of this input.
    */
   public byte[] getSignature() {
      return _signature;
   }

   /**
    * The hash type.
    * <p>
    * Look for SIGHASH_ALL, SIGHASH_NONE, SIGHASH_SINGLE, SIGHASH_ANYONECANPAY
    * in the reference client
    */
   public int getHashType() {
      // hash type is the last byte of the signature
      return ((int) (_signature[_signature.length - 1])) & 0xFF;
   }

   /**
    * Get the public key bytes of this input.
    * 
    * @return The public key bytes of this input.
    */
   public byte[] getPublicKeyBytes() {
      return _publicKeyBytes;
   }

}
