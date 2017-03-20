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
    * @param b the byte to encode
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
         if (high < 0 || low < 0){
            throw new RuntimeException("Invalid hex digit " + hex[i * 2] + hex[i * 2 + 1]);
         }
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

   public static boolean isAllZero(byte[] bytes){
      for (byte b : bytes){
         if (b != 0) {
            return false;
         }
      }
      return true;
   }
}
