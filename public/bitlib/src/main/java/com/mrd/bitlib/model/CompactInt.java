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

package com.mrd.bitlib.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

/**
 * Used for representing Bitcoin's compact size.
 */
public class CompactInt {

   /**
    * Read a CompactInt from a byte buffer.
    * 
    * @param buf
    *           The byte buffer to read from
    * @return the long value representing the CompactInt read or -1 if the
    *         buffer is too small to hold the CompactInt.
    * @throws IOException
    */
   public static long fromByteBuffer(ByteBuffer buf) {
      if (buf.remaining() < 1) {
         // XXX make all callers check for -1
         return -1;
      }
      long first = 0x00000000000000FFL & ((long) buf.get());
      long value;
      if (first < 253) {
         // Regard this byte as a 8 bit value.
         value = 0x00000000000000FFL & ((long) first);
      } else if (first == 253) {
         // Regard the following two bytes as a 16 bit value
         if (buf.remaining() < 2) {
            return -1;
         }
         buf.order(ByteOrder.LITTLE_ENDIAN);
         value = 0x0000000000FFFFL & ((long) buf.getShort());
      } else if (first == 254) {
         // Regard the following four bytes as a 32 bit value
         if (buf.remaining() < 4) {
            return -1;
         }
         buf.order(ByteOrder.LITTLE_ENDIAN);
         value = 0x00000000FFFFFFFF & ((long) buf.getInt());
      } else {
         // Regard the following four bytes as a 64 bit value
         if (buf.remaining() < 8) {
            return -1;
         }
         buf.order(ByteOrder.LITTLE_ENDIAN);
         value = buf.getLong();
      }
      return value;
   }

   public static long fromByteReader(ByteReader reader) throws InsufficientBytesException {
      long first = 0x00000000000000FFL & ((long) reader.get());
      long value;
      if (first < 253) {
         // Regard this byte as a 8 bit value.
         value = 0x00000000000000FFL & ((long) first);
      } else if (first == 253) {
         // Regard the following two bytes as a 16 bit value
         value = 0x0000000000FFFFL & ((long) reader.getShortLE());
      } else if (first == 254) {
         // Regard the following four bytes as a 32 bit value
         value = 0x00000000FFFFFFFF & ((long) reader.getIntLE());
      } else {
         // Regard the following four bytes as a 64 bit value
         value = reader.getLongLE();
      }
      return value;
   }

   /**
    * Write a long value to a {@code ByteBuffer} as a CompaceInt
    * 
    * @param value
    *           The value to write.
    * @param buf
    *           The buffer to write to.
    * @throws IOException
    */
   public static void toByteBuffer(long value, ByteBuffer buf) {
      buf.put(toBytes(value));
   }

   /**
    * Turn a long value into an array of bytes containing the CompactInt
    * representation.
    * 
    * @param value
    *           The value to turn into an array of bytes.
    * @return an array of bytes.
    */
   public static byte[] toBytes(long value) {
      if (isLessThan(value, 253)) {
         return new byte[] { (byte) value };
      } else if (isLessThan(value, 65536)) {
         return new byte[] { (byte) 253, (byte) (value), (byte) (value >> 8) };
      } else if (isLessThan(value, 4294967295L)) {
         byte[] bytes = new byte[5];
         bytes[0] = (byte) 254;
         BitUtils.uint32ToByteArrayLE(value, bytes, 1);
         return bytes;
      } else {
         byte[] bytes = new byte[9];
         bytes[0] = (byte) 255;
         BitUtils.uint32ToByteArrayLE(value, bytes, 1);
         BitUtils.uint32ToByteArrayLE(value >>> 32, bytes, 5);
         return bytes;
      }
   }

   /**
    * Determine whether one long is less than another long when comparing as
    * unsigned longs.
    */
   private static boolean isLessThan(long n1, long n2) {
      return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
   }

}
