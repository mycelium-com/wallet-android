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

import java.util.UUID;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.mrd.bitlib.model.Address;

public class AddressBookManager {
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
      public int compareTo(@NonNull Entry another) {
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

   public static class IconEntry extends Entry{
      private Drawable _icon;
      private UUID id;

      public IconEntry(Address address, String name, Drawable icon) {
         super(address, name);
         this._icon = icon;
      }

      public IconEntry(Address address, String name,  Drawable icon, UUID id) {
         this(address, name, icon);
         this.id = id;
      }

      public UUID getId() {
         return id;
      }

      public Drawable getIcon() {
         return _icon;
      }
   }
}
