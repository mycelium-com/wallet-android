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

package com.mrd.bitlib.util;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Sha256Hash implements Serializable {
   private static final long serialVersionUID = 1L;

   public static final Sha256Hash ZERO_HASH = new Sha256Hash();
   public static final int HASH_LENGTH = 32;

   final private byte[] _bytes;
   private int _hash;

   private Sha256Hash() {
      this._bytes = new byte[32];
      _hash = -1;
   }

   public Sha256Hash(byte[] bytes) {
      this._bytes = bytes;
      _hash = -1;
   }

   public Sha256Hash(byte[] bytes, boolean reverse) {
      if (reverse) {
         this._bytes = BitUtils.reverseBytes(bytes);
      } else {
         this._bytes = bytes;

      }
      _hash = -1;
   }

   public Sha256Hash(byte[] bytes, int offset, boolean reverse) {
      _bytes = new byte[32];
      if (reverse) {
         // Copy 32 byte hash from offset and reverse byte order
         for (int i = 0; i < _bytes.length; i++) {
            _bytes[i] = bytes[offset + 32 - 1 - i];
         }
      } else {
         System.arraycopy(bytes, offset, _bytes, 0, 32);
      }
      _hash = -1;
   }

   public Sha256Hash(ByteBuffer buf, boolean reverse) {
      byte[] bytes = new byte[32];
      buf.get(bytes, 0, 32);
      if (reverse) {
         this._bytes = BitUtils.reverseBytes(bytes);
      } else {
         this._bytes = bytes;
      }
      _hash = -1;
   }

   public static Sha256Hash create(byte[] contents) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         return new Sha256Hash(digest.digest(contents));
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e); // Cannot happen.
      }
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof Sha256Hash))
         return false;
      return Arrays.equals(_bytes, ((Sha256Hash) other)._bytes);
   }

   @Override
   public int hashCode() {
      if (_hash == -1) {
         final int offset = _bytes.length - 4;
         _hash = 0;
         for (int i = 0; i < 4; i++) {
            _hash <<= 8;
            _hash |= (((int) _bytes[offset + i]) & 0xFF);
         }
      }
      return _hash;
   }

   @Override
   public String toString() {
      return HexUtils.toHex(_bytes);
   }

   public byte[] getBytes() {
      return _bytes;
   }

   public void toByteBuffer(ByteBuffer buf, boolean reverse) {
      if (reverse) {
         buf.put(BitUtils.reverseBytes(_bytes));
      } else {
         buf.put(_bytes);
      }
   }

   public Sha256Hash duplicate() {
      return new Sha256Hash(_bytes);
   }
}
