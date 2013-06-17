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

public class ByteReader {

   public static class InsufficientBytesException extends Exception {

      private static final long serialVersionUID = 1L;
   }
   
   private byte[] _buf;
   private int _index;

   public ByteReader(byte[] buf) {
      _buf = buf;
      _index = 0;
   }

   public ByteReader(byte[] buf, int index) {
      _buf = buf;
      _index = index;
   }

   public byte get() throws InsufficientBytesException {
      checkAvailable(1);
      return _buf[_index++];
   }

   public int getShortLE() throws InsufficientBytesException {
      checkAvailable(2);
      return (((_buf[_index++] & 0xFF) << 0) | ((_buf[_index++] & 0xFF) << 8)) & 0xFFFF;
   }

   public int getIntLE() throws InsufficientBytesException {
      checkAvailable(4);
      return ((_buf[_index++] & 0xFF) << 0) | ((_buf[_index++] & 0xFF) << 8) | ((_buf[_index++] & 0xFF) << 16)
            | ((_buf[_index++] & 0xFF) << 24);
   }

   public long getLongLE() throws InsufficientBytesException {
      checkAvailable(8);
      return ((_buf[_index++] & 0xFFL) << 0) | ((_buf[_index++] & 0xFFL) << 8) | ((_buf[_index++] & 0xFFL) << 16)
            | ((_buf[_index++] & 0xFFL) << 24) | ((_buf[_index++] & 0xFFL) << 32) | ((_buf[_index++] & 0xFFL) << 40)
            | ((_buf[_index++] & 0xFFL) << 48) | ((_buf[_index++] & 0xFFL) << 56);
   }

   public byte[] getBytes(int size) throws InsufficientBytesException {
      checkAvailable(size);
      byte[] bytes = new byte[size];
      System.arraycopy(_buf, _index, bytes, 0, size);
      _index += size;
      return bytes;
   }

   public String getString() throws InsufficientBytesException {
      int length = getIntLE();
      byte[] bytes = getBytes(length);
      return new String(bytes);
   }

   public void skip(int num) throws InsufficientBytesException {
      checkAvailable(num);
      _index += num;
   }

   public void reset() {
      _index = 0;
   }

   public long getCompactInt() throws InsufficientBytesException {
      return CompactInt.fromByteReader(this);
   }

   public Sha256Hash getSha256Hash() throws InsufficientBytesException {
      return new Sha256Hash(getBytes(32));
   }

   public Sha256Hash getSha256Hash(boolean reverse) throws InsufficientBytesException {
      checkAvailable(32);
      if (reverse) {
         return new Sha256Hash(BitUtils.reverseBytes(getBytes(32)));
      }
      return new Sha256Hash(getBytes(32));
   }

   public int getPosition() {
      return _index;
   }

   public void setPosition(int index) {
      _index = index;
   }

   public final int available() {
      return _buf.length - _index;
   }

   private final void checkAvailable(int num) throws InsufficientBytesException {
      if (_buf.length - _index < num) {
         throw new InsufficientBytesException();
      }
   }
}
