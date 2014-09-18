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

package com.mrd.mbwapi.api;

import java.util.Date;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class ExchangeRate extends ApiObject {

   public final String name;
   public final long time;
   public final String currency;
   public final Double price; // null if price is not available

   public ExchangeRate(String name, long time, String currency, Double price) {
      this.name = name;
      this.time = time;
      this.currency = currency;
      this.price = price;
   }

   public ExchangeRate(ExchangeRate o) {
      this(o.name, o.time, o.currency, o.price);
   }

   protected ExchangeRate(ByteReader reader) throws InsufficientBytesException {
      name = reader.getString();
      time = reader.getLongLE();
      currency = reader.getString();
      String priceString = reader.getString();
      if (priceString.length() == 0) {
         price = null;
      } else {
         Double p;
         try {
            p = Double.valueOf(priceString);
         } catch (NumberFormatException e) {
            p = null;
         }
         price = p;
      }
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Name: ").append(name);
      sb.append(" time: ").append(new Date(time));
      sb.append(" currency: ").append(currency);
      sb.append(" price: ").append(price == null ? "<Not available>" : price);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return name.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof ExchangeRate)) {
         return false;
      }
      ExchangeRate other = (ExchangeRate) obj;
      return other.time == time && other.name.equals(name) && other.currency.equals(currency);
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putString(name.toString());
      writer.putLongLE(time);
      writer.putString(currency);
      writer.putString(price == null ? "" : price.toString());
      return writer;
   }

   @Override
   public byte getType() {
      return ApiObject.EXCHANGE_RATE_TYPE;
   }

}
