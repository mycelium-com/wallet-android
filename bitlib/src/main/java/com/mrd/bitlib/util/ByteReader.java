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

   public boolean getBoolean() throws InsufficientBytesException {
      return get() != 0;
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

   public int getIntBE() throws InsufficientBytesException {
      checkAvailable(4);
      return ((_buf[_index++] & 0xFF) << 24) | ((_buf[_index++] & 0xFF) << 16) | ((_buf[_index++] & 0xFF) << 8)
            | ((_buf[_index++] & 0xFF) << 0);
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
      checkAvailable(Sha256Hash.HASH_LENGTH);
      return Sha256Hash.of(getBytes(Sha256Hash.HASH_LENGTH));
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
