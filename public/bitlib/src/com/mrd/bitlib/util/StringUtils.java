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
    * @param str
    *           The string to cap
    * @param capSize
    *           The maximum cap size
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
