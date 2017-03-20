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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Utilities for converting between byte arrays and unsigned integer values.
 */
public class BitUtils {

   public static long uint16ToLong(byte[] buf, int offset) {
      return ((buf[offset++] & 0xFFL) << 0) | ((buf[offset++] & 0xFFL) << 8);
   }

   public static long uint32ToLong(byte[] buf, int offset) {
      return ((buf[offset++] & 0xFFL) << 0) | ((buf[offset++] & 0xFFL) << 8) | ((buf[offset++] & 0xFFL) << 16)
            | ((buf[offset] & 0xFFL) << 24);
   }

   public static long uint64ToLong(byte[] buf, int offset) {
      return ((buf[offset++] & 0xFFL) << 0) | ((buf[offset++] & 0xFFL) << 8) | ((buf[offset++] & 0xFFL) << 16)
            | ((buf[offset++] & 0xFFL) << 24) | ((buf[offset++] & 0xFFL) << 32) | ((buf[offset++] & 0xFFL) << 40)
            | ((buf[offset++] & 0xFFL) << 48) | ((buf[offset++] & 0xFFL) << 56);
   }

   public static int uint16FromStream(DataInputStream stream) throws IOException {
      return (int) (((stream.read() & 0xFFL) << 0) | ((stream.read() & 0xFFL) << 8));
   }

   public static int uint16FromStreamBE(DataInputStream stream) throws IOException {
      return (int) (((stream.read() & 0xFFL) << 8) | ((stream.read() & 0xFFL) << 0));
   }

   public static int uint32FromStream(DataInputStream stream) throws IOException {
      return (int) (((stream.read() & 0xFFL) << 0) | ((stream.read() & 0xFFL) << 8) | ((stream.read() & 0xFFL) << 16) | ((stream
            .read() & 0xFFL) << 24));
   }

   public static long uint64FromStream(DataInputStream stream) throws IOException {
      return ((stream.read() & 0xFFL) << 0) | ((stream.read() & 0xFFL) << 8) | ((stream.read() & 0xFFL) << 16)
            | ((stream.read() & 0xFFL) << 24) | ((stream.read() & 0xFFL) << 32) | ((stream.read() & 0xFFL) << 40)
            | ((stream.read() & 0xFFL) << 48) | ((stream.read() & 0xFFL) << 56);
   }

   public static void uint32ToStream(long value, OutputStream stream) throws IOException {
      stream.write((byte) (0xFFL & (value >> 0)));
      stream.write((byte) (0xFFL & (value >> 8)));
      stream.write((byte) (0xFFL & (value >> 16)));
      stream.write((byte) (0xFFL & (value >> 24)));
   }

   public static void uint64ToStream(long value, OutputStream stream) throws IOException {
      stream.write((byte) (0xFFL & (value >> 0)));
      stream.write((byte) (0xFFL & (value >> 8)));
      stream.write((byte) (0xFFL & (value >> 16)));
      stream.write((byte) (0xFFL & (value >> 24)));
      stream.write((byte) (0xFFL & (value >> 32)));
      stream.write((byte) (0xFFL & (value >> 40)));
      stream.write((byte) (0xFFL & (value >> 48)));
      stream.write((byte) (0xFFL & (value >> 56)));
   }

   public static void uint32ToByteArrayLE(long value, byte[] output, int offset) {
      output[offset + 0] = (byte) (0xFFL & (value >> 0));
      output[offset + 1] = (byte) (0xFFL & (value >> 8));
      output[offset + 2] = (byte) (0xFFL & (value >> 16));
      output[offset + 3] = (byte) (0xFFL & (value >> 24));
   }

   public static void uint64ToByteArrayLE(long value, byte[] output, int offset) {
      output[offset + 0] = (byte) (0xFFL & (value >> 0));
      output[offset + 1] = (byte) (0xFFL & (value >> 8));
      output[offset + 2] = (byte) (0xFFL & (value >> 16));
      output[offset + 3] = (byte) (0xFFL & (value >> 24));
      output[offset + 4] = (byte) (0xFFL & (value >> 32));
      output[offset + 5] = (byte) (0xFFL & (value >> 40));
      output[offset + 6] = (byte) (0xFFL & (value >> 48));
      output[offset + 7] = (byte) (0xFFL & (value >> 56));
   }

   public static boolean areEqual(byte[] a, byte[] b) {
      if (a == null && b == null) {
         return true;
      }
      if (a == null || b == null) {
         return false;
      }
      if (a.length != b.length) {
         return false;
      }
      for (int i = 0; i < a.length; i++) {
         if (a[i] != b[i]) {
            return false;
         }
      }
      return true;
   }

   public static byte[] copyByteArray(byte[] source) {
      byte[] buf = new byte[source.length];
      System.arraycopy(source, 0, buf, 0, buf.length);
      return buf;
   }

   /**
    * Returns a copy of the given byte array in reverse order.
    */
   public static byte[] reverseBytes(byte[] bytes) {
      byte[] buf = new byte[bytes.length];
      for (int i = 0; i < bytes.length; i++)
         buf[i] = bytes[bytes.length - 1 - i];
      return buf;
   }

   /**
    * Read a number of bytes or throw.
    *
    * @param size The number of bytes read
    * @return The array of bytes read.
    * @throws IOException
    */
   public static byte[] readBytes(DataInputStream stream, int size) throws IOException {
      byte[] buf = new byte[size];
      int toRead = size;
      int done = 0;
      while (toRead > 0) {
         int read = stream.read(buf, done, toRead);
         if (read == -1) {
            throw new IOException();
         }
         done += read;
         toRead -= read;
      }
      return buf;
   }

   // Arrays.copyOf implementation which we can use also on Java versions <
   // 1.6
   public static byte[] copyOf(byte[] original, int newLength) {
      if (newLength < 0) {
         throw new IllegalArgumentException();
      }
      byte[] buf = new byte[newLength];
      int lastIndex = Math.min(original.length, newLength);
      System.arraycopy(original, 0, buf, 0, lastIndex);
      return buf;
   }

   // Arrays.copyOfRange implementation which we can use also on Java versions <
   // 1.6
   public static byte[] copyOfRange(byte[] original, int from, int to) {
      if (to < from || from < 0 || from > original.length) {
         throw new IllegalArgumentException();
      }
      byte[] buf = new byte[to - from];
      int lastIndex = Math.min(original.length, to);
      System.arraycopy(original, from, buf, 0, lastIndex - from);
      return buf;
   }

   /**
    * Return the concatenation of two byte arrays in a new byte array
    */
   public static byte[] concatenate(byte[] a, byte[] b) {
      byte[] result = new byte[a.length + b.length];
      System.arraycopy(a, 0, result, 0, a.length);
      System.arraycopy(b, 0, result, a.length, b.length);
      return result;
   }
}
