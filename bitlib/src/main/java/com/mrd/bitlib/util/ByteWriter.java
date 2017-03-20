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

import java.io.UnsupportedEncodingException;

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

   public void putBoolean(boolean b) {
      put(b ? (byte) 1 : (byte) 0);
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

   public void putIntBE(int value) {
      ensureCapacity(4);
      _buf[_index++] = (byte) (0xFF & (value >> 24));
      _buf[_index++] = (byte) (0xFF & (value >> 16));
      _buf[_index++] = (byte) (0xFF & (value >> 8));
      _buf[_index++] = (byte) (0xFF & (value >> 0));
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

   public void putLongBE(long value) {
      ensureCapacity(8);
      _buf[_index++] = (byte) (0xFFL & (value >> 56));
      _buf[_index++] = (byte) (0xFFL & (value >> 48));
      _buf[_index++] = (byte) (0xFFL & (value >> 40));
      _buf[_index++] = (byte) (0xFFL & (value >> 32));
      _buf[_index++] = (byte) (0xFFL & (value >> 24));
      _buf[_index++] = (byte) (0xFFL & (value >> 16));
      _buf[_index++] = (byte) (0xFFL & (value >> 8));
      _buf[_index++] = (byte) (0xFFL & (value >> 0));
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

   public void putRawStringUtf8(String s) {
      try {
         byte[] bytes = s.getBytes("UTF-8");
         putBytes(bytes);
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
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
