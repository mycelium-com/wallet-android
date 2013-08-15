/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.bitlib.model;

import java.io.Serializable;

public class ScriptOutputStandard extends ScriptOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   private byte[] _addressBytes;

   protected ScriptOutputStandard(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
      _addressBytes = chunks[2];
   }

   protected static boolean isScriptOutputStandard(byte[][] chunks) {
      if (chunks.length != 5 && chunks.length != 6) {
         return false;
      }
      if (!Script.isOP(chunks[0], OP_DUP)) {
         return false;
      }
      if (!Script.isOP(chunks[1], OP_HASH160)) {
         return false;
      }
      if (chunks[2].length != 20) {
         return false;
      }
      if (!Script.isOP(chunks[3], OP_EQUALVERIFY)) {
         return false;
      }
      if (!Script.isOP(chunks[4], OP_CHECKSIG)) {
         return false;
      }
      if (chunks.length == 6 && !Script.isOP(chunks[5], OP_NOP)) {
         // Variant that has a NOP at the end
         return false;
      }
      return true;
   }

   public ScriptOutputStandard(byte[] addressBytes) {
      //todo check length for type specfic length 20?
      super(scriptEncodeChunks(new byte[][] { { (byte) OP_DUP }, { (byte) OP_HASH160 }, addressBytes,
            { (byte) OP_EQUALVERIFY }, { (byte) OP_CHECKSIG } }));
      _addressBytes = addressBytes;
   }

   /**
    * Get the address that this output is for.
    * 
    * @return The address that this output is for.
    */
   public byte[] getAddressBytes() {
      return _addressBytes;
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      return Address.fromStandardBytes(getAddressBytes(), network);
   }

}
