/*
 * Copyright 2013 Megion Research & Development GmbH
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

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;

public class StringUtils {

   /**
    * Join a collection of strings with the given separator.
    * 
    * @param strings
    *           The strings to join
    * @param separator
    *           The separator to use
    * @return The concatenation of the collection of strings with the given
    *         separator.
    */
   public static String join(Collection<String> strings, String separator) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (String s : strings) {
         if (first) {
            first = false;
         } else {
            sb.append(separator);
         }
         sb.append(s);
      }
      return sb.toString();
   }

   /**
    * Join an array of strings with the given separator.
    * 
    * @param strings
    *           The strings to join
    * @param separator
    *           The separator to use
    * @return The concatenation of the collection of strings with the given
    *         separator.
    */
   public static String join(String[] strings, String separator) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (String s : strings) {
         if (first) {
            first = false;
         } else {
            sb.append(separator);
         }
         sb.append(s);
      }
      return sb.toString();
   }

   /**
    * Join the string representation of an array objects with the given
    * separator.
    * <p>
    * the toString() method is called on each object to get its string
    * representation.
    * 
    * @param objects
    *           The object whose string representation is to be joined.
    * @param separator
    *           The separator to use
    * @return The concatenation of the collection of strings with the given
    *         separator.
    */
   public static String join(Object[] objects, String separator) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (Object o : objects) {
         if (first) {
            first = false;
         } else {
            sb.append(separator);
         }
         sb.append(o.toString());
      }
      return sb.toString();
   }

   /**
    * Return a string that is no longer than capSize, and pad with "..." if
    * returning a substring.
    *
    * @param str     The string to cap
    * @param capSize The maximum cap size
    * @return The string capped at capSize.
    */
   public static String cap(String str, int capSize) {
      if (str.length() <= capSize) {
         return str;
      }
      if (capSize <= 3) {
         return str.substring(0, capSize);
      }
      return str.substring(0, capSize - 3) + "...";
   }

   public static String readFully(Reader reader) throws IOException {
      char[] buffer = new char[2048];
      StringBuilder sb = new StringBuilder();
      while (true) {
         int read = reader.read(buffer);
         if (read != -1) {
            sb.append(buffer, 0, read);
         } else {
            return sb.toString();
         }
      }
   }

}
