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

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

public class OutPoint implements Serializable {
   private static final long serialVersionUID = 1L;
   
   public static final OutPoint COINBASE_OUTPOINT = new OutPoint(Sha256Hash.ZERO_HASH, 0);
   public Sha256Hash hash;
   public int index;

   public OutPoint(Sha256Hash hash, int index) {
      this.hash = hash;
      this.index = index;
   }

   public OutPoint(ByteReader reader) throws InsufficientBytesException {
      this.hash = reader.getSha256Hash();
      this.index = (int) reader.getCompactInt();
   }

   @Override
   public int hashCode() {
      return hash.hashCode() + index;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof OutPoint)) {
         return false;
      }
      return hash.equals(((OutPoint) other).hash) && index == ((OutPoint) other).index;
   }

   @Override
   public String toString() {
      return new StringBuilder().append(hash).append(':').append(index).toString();
   }

   public ByteWriter toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(hash);
      writer.putCompactInt(index);
      return writer;
   }

   public static OutPoint fromString(String string) {
      try {
         if (string == null) {
            return null;
         }
         int colon = string.indexOf(':');
         if (colon == -1) {
            return null;
         }
         String txid = string.substring(0, colon);
         if (txid.length() != 64) {
            return null;
         }
         byte[] bytes = HexUtils.toBytes(txid);
         if (bytes == null) {
            return null;
         }
         String indexString = string.substring(colon + 1);
         int index = Integer.parseInt(indexString);
         return new OutPoint(new Sha256Hash(bytes), index);
      } catch (Exception e) {
         return null;
      }
   }
}
