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

import com.mrd.bitlib.model.CompactInt;

final public class ByteWriter {

   private byte[] _buf;
   private int _index;

   public ByteWriter(int capacity) {
      _buf = new byte[capacity];
      _index = 0;
   }

   public ByteWriter(byte[] buf) {
      _buf = buf;
      _index = buf.length;
   }

   final private void ensureCapacity(int capacity) {
      if (_buf.length - _index < capacity) {
         byte[] temp = new byte[_buf.length * 2 + capacity];
         System.arraycopy(_buf, 0, temp, 0, _index);
         _buf = temp;
      }
   }

   public void put(byte b) {
      ensureCapacity(1);
      _buf[_index++] = b;
   }

   public void putShortLE(short value) {
      ensureCapacity(2);
      _buf[_index++] = (byte) (0xFF & (value >> 0));
      _buf[_index++] = (byte) (0xFF & (value >> 8));
   }

   public void putIntLE(int value) {
      ensureCapacity(4);
      _buf[_index++] = (byte) (0xFF & (value >> 0));
      _buf[_index++] = (byte) (0xFF & (value >> 8));
      _buf[_index++] = (byte) (0xFF & (value >> 16));
      _buf[_index++] = (byte) (0xFF & (value >> 24));
   }

   public void putLongLE(long value) {
      ensureCapacity(8);
      _buf[_index++] = (byte) (0xFFL & (value >> 0));
      _buf[_index++] = (byte) (0xFFL & (value >> 8));
      _buf[_index++] = (byte) (0xFFL & (value >> 16));
      _buf[_index++] = (byte) (0xFFL & (value >> 24));
      _buf[_index++] = (byte) (0xFFL & (value >> 32));
      _buf[_index++] = (byte) (0xFFL & (value >> 40));
      _buf[_index++] = (byte) (0xFFL & (value >> 48));
      _buf[_index++] = (byte) (0xFFL & (value >> 56));
   }

   public void putBytes(byte[] value) {
      ensureCapacity(value.length);
      System.arraycopy(value, 0, _buf, _index, value.length);
      _index += value.length;
   }

   public void putBytes(byte[] value, int offset, int length) {
      ensureCapacity(length);
      System.arraycopy(value, offset, _buf, _index, length);
      _index += length;
   }

   public void putCompactInt(long value) {
      putBytes(CompactInt.toBytes(value));
   }

   public void putSha256Hash(Sha256Hash hash) {
      putBytes(hash.getBytes());
   }

   public void putSha256Hash(Sha256Hash hash, boolean reverse) {
      if (reverse) {
         putBytes(BitUtils.reverseBytes(hash.getBytes()));
      } else {
         putBytes(hash.getBytes());
      }
   }

   public void putString(String s) {
      byte[] bytes = s.getBytes();
      putIntLE(bytes.length);
      putBytes(bytes);
   }

   public byte[] toBytes() {
      byte[] bytes = new byte[_index];
      System.arraycopy(_buf, 0, bytes, 0, _index);
      return bytes;
   }

   public int length() {
      return _index;
   }
}
