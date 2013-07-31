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

/**
 * Utilities for going to and from ASCII-HEX representation.
 */
public class HexUtils {

   /**
    * Encodes an array of bytes as hex symbols.
    * 
    * @param bytes
    *           the array of bytes to encode
    * @return the resulting hex string
    */
   public static String toHex(byte[] bytes) {
      return toHex(bytes, null);
   }

   /**
    * Encodes an array of bytes as hex symbols.
    * 
    * @param bytes
    *           the array of bytes to encode
    * @param separator
    *           the separator to use between two bytes, can be null
    * @return the resulting hex string
    */
   public static String toHex(byte[] bytes, String separator) {
      return toHex(bytes, 0, bytes.length, separator);
   }

   /**
    * Encodes an array of bytes as hex symbols.
    * 
    * @param bytes
    *           the array of bytes to encode
    * @param offset
    *           the start offset in the array of bytes
    * @param length
    *           the number of bytes to encode
    * @return the resulting hex string
    */
   public static String toHex(byte[] bytes, int offset, int length) {
      return toHex(bytes, offset, length, null);
   }

   /**
    * Encodes a single byte to hex symbols.
    * 
    * @param byte the byte to encode
    * @return the resulting hex string
    */
   public static String toHex(byte b) {
      StringBuilder sb = new StringBuilder();
      appendByteAsHex(sb, b);
      return sb.toString();
   }

   /**
    * Encodes an array of bytes as hex symbols.
    * 
    * @param bytes
    *           the array of bytes to encode
    * @param offset
    *           the start offset in the array of bytes
    * @param length
    *           the number of bytes to encode
    * @param separator
    *           the separator to use between two bytes, can be null
    * @return the resulting hex string
    */
   public static String toHex(byte[] bytes, int offset, int length, String separator) {
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < length; i++) {
         int unsignedByte = bytes[i + offset] & 0xff;

         if (unsignedByte < 16) {
            result.append("0");
         }

         result.append(Integer.toHexString(unsignedByte));
         if (separator != null && i + 1 < length) {
            result.append(separator);
         }
      }
      return result.toString();
   }

   /**
    * Get the byte representation of an ASCII-HEX string.
    * 
    * @param hexString
    *           The string to convert to bytes
    * @return The byte representation of the ASCII-HEX string.
    */
   public static byte[] toBytes(String hexString) {
      if (hexString == null || hexString.length() % 2 != 0) {
         throw new RuntimeException("Input string must contain an even number of characters");
      }
      char[] hex = hexString.toCharArray();
      int length = hex.length / 2;
      byte[] raw = new byte[length];
      for (int i = 0; i < length; i++) {
         int high = Character.digit(hex[i * 2], 16);
         int low = Character.digit(hex[i * 2 + 1], 16);
         int value = (high << 4) | low;
         if (value > 127)
            value -= 256;
         raw[i] = (byte) value;
      }
      return raw;
   }

   public static void appendByteAsHex(StringBuilder sb, byte b) {
      int unsignedByte = b & 0xFF;
      if (unsignedByte < 16) {
         sb.append("0");
      }
      sb.append(Integer.toHexString(unsignedByte));
   }
}
