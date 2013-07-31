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
    * @param size
    *           The number of bytes read
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

}
