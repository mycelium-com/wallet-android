/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

package com.mycelium.wallet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Bus;

public class AddressBookManager {
   private static final String ADDRESS_BOOK_FILE_NAME = "address-book.txt";

   public static abstract class AddressBookKey {
   }

   public static class Entry implements Comparable<Entry> {
      private Address _address;
      private String _name;

      public Entry(Address address, String name) {
         _address = address;
         _name = name == null ? "" : name;
      }

      public Address getAddress() {
         return _address;
      }

      public String getName() {
         return _name;
      }

      @Override
      public int compareTo(Entry another) {
         return _name.compareToIgnoreCase(another._name);
      }

      @Override
      public int hashCode() {
         return _name.hashCode() + _address.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         if (!(obj instanceof Entry)) {
            return false;
         }
         Entry other = (Entry) obj;
         return _address.equals(other._address) && _name.equals(other._name);
      }

   }

   private Context _applicationContext;
   private List<Entry> _entries;
   private Map<Address, Entry> _addressMap;

   public AddressBookManager(Context context) {
      _applicationContext = context.getApplicationContext();
      List<Entry> entries = loadEntries(_applicationContext);
      _entries = Lists.newArrayList();
      _addressMap = Maps.newHashMap();
      for (Entry entry : entries) {
         insertOrUpdateEntryInt(entry.getAddress(), entry.getName());
      }
      Collections.sort(_entries);
   }

   private void insertOrUpdateEntryInt(Address address, String name) {
      Preconditions.checkNotNull(address,name);
      name = name.trim();
      if (name.length() == 0) {
         // We don't want entries with blank names
         return;
      }

      Entry entry = _addressMap.get(address);
      if (entry == null) {
         entry = new Entry(address, name);
         _entries.add(new Entry(address, name));
      } else {
         _entries.remove(entry);
         entry._name = name;
         _entries.add(entry);
      }
      _addressMap.put(address, entry);
   }

   public List<Entry> getEntries() {
      return Collections.unmodifiableList(_entries);
   }

   private static List<Entry> loadEntries(Context applicationContext) {
      try {
         List<Entry> entries = new ArrayList<Entry>();
         BufferedReader stream;
         try {
            stream = new BufferedReader(new InputStreamReader(applicationContext.openFileInput(ADDRESS_BOOK_FILE_NAME)));
         } catch (FileNotFoundException e) {
            //todo insert uncaught error handler
            // ignore and return an empty set of addresses
            return entries;
         }

         while (true) {
            String line = stream.readLine();
            if (line == null) {
               break;
            }
            List<String> list = stringToValueList(line);
            String addressString = null;
            if (list.size() > 0) {
               addressString = decode(list.get(0));
            }
            String name = null;
            if (list.size() > 1) {
               name = decode(list.get(1));
            }
            Address address = Address.fromString(addressString);
            if (address != null) {
               entries.add(new Entry(address, name));
            }
         }
         stream.close();
         return entries;
      } catch (Exception e) {
         e.printStackTrace();
         // ignore and return an empty set of addresses
         return new ArrayList<Entry>();
      }
   }

   private static List<String> stringToValueList(String string) {
      int startIndex = 0;
      List<String> values = new LinkedList<String>();
      while (true) {
         int separatorIndex = nextSeparator(string, startIndex);
         if (separatorIndex == -1) {
            // something wrong, return empty list
            return new LinkedList<String>();
         }
         String value = string.substring(startIndex, separatorIndex);
         startIndex = separatorIndex + 1;
         values.add(value);
         if (separatorIndex == string.length()) {
            break;
         }
      }
      return values;
   }

   /**
    * Find the next ',' occurrence in a string where: we skip '/' followed by
    * any char, skip anything in an opening parenthesis until we hit a matching
    * closing parenthesis
    * 
    * @param s
    *           the string to find the next ',' in
    * @param startIndex
    *           the start index where the search starts
    * @return the resulting comma index or the length of the string
    */
   private static int nextSeparator(String s, int startIndex) {
      boolean slash = false;
      int pCounter = 0;
      for (int i = startIndex; i < s.length(); i++) {
         if (slash) {
            slash = false;
            continue;
         }
         char c = s.charAt(i);
         if (c == '/') {
            slash = true;
            continue;
         }
         if (c == '(') {
            pCounter++;
            continue;
         }
         if (c == ')') {
            if (pCounter < 0) {
               return -1;
            }
            pCounter--;
            continue;
         }
         if (pCounter > 0) {
            continue;
         }
         if (c == ',') {
            return i;
         }
      }
      if (pCounter < 0) {
         return -1;
      }
      return s.length();
   }

   private static String decode(String value) {
      StringBuilder sb = new StringBuilder(value.length() + 1);
      char[] chars = value.toCharArray();
      boolean slash = false;
      for (char c : chars) {
         if (slash) {
            slash = false;
            if (c == '/') {
               sb.append('/');
            } else if (c == ',') {
               sb.append(',');
            } else if (c == '(') {
               sb.append('(');
            } else if (c == ')') {
               sb.append(')');
            } else {
               // decode error, ignore this character
            }
         } else {
            if (c == '/') {
               slash = true;
            } else {
               sb.append(c);
            }
         }
      }
      return sb.toString();
   }
}
