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

package com.mrd.mbwapi.api;

import java.math.BigDecimal;
import java.util.Date;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class ExchangeSummary extends ApiObject {

   public enum ExchangeId {
      Unknown("Unknown"), BitStamp("BitStamp"), MtGox("MtGox");

      private String _name;

      private ExchangeId(String name) {
         _name = name;
      }

      public static ExchangeId fromString(String name) {
         if (name.equals(BitStamp._name)) {
            return BitStamp;
         } else if (name.equals(MtGox._name)) {
            return MtGox;
         } else {
            return Unknown;
         }
      }

      @Override
      public String toString() {
         return _name;
      }

   };

   public ExchangeId exchange;
   public long time;
   public String currency;
   public BigDecimal high;
   public BigDecimal low;
   public BigDecimal last;
   public BigDecimal bid;
   public BigDecimal ask;
   public long satoshiVolume;

   public ExchangeSummary(ExchangeId exchange, long time, String currency, BigDecimal high, BigDecimal low,
         BigDecimal last, BigDecimal bid, BigDecimal ask, long satoshiVolume) {
      this.exchange = exchange;
      this.time = time;
      this.currency = currency;
      this.high = high;
      this.low = low;
      this.last = last;
      this.bid = bid;
      this.ask = ask;
      this.satoshiVolume = satoshiVolume;
   }

   protected ExchangeSummary(ByteReader reader) throws InsufficientBytesException {
      exchange = ExchangeId.fromString(reader.getString());
      time = reader.getLongLE();
      currency = reader.getString();
      high = new BigDecimal(reader.getString());
      low = new BigDecimal(reader.getString());
      last = new BigDecimal(reader.getString());
      bid = new BigDecimal(reader.getString());
      ask = new BigDecimal(reader.getString());
      satoshiVolume = reader.getLongLE();

      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Exchange: ").append(exchange).append("\r\n");
      sb.append("time: ").append(new Date(time)).append("\r\n");
      sb.append("currency: ").append(currency).append("\r\n");
      sb.append("high: ").append(high).append("\r\n");
      sb.append("low: ").append(low).append("\r\n");
      sb.append("bid: ").append(low).append("\r\n");
      sb.append("ask: ").append(low).append("\r\n");
      sb.append("last: ").append(last).append("\r\n");
      sb.append("volume: ").append(satoshiVolume).append("\r\n");
      return sb.toString();
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putString(exchange.toString());
      writer.putLongLE(time);
      writer.putString(currency);
      writer.putString(high.toString());
      writer.putString(low.toString());
      writer.putString(last.toString());
      writer.putString(bid.toString());
      writer.putString(ask.toString());
      writer.putLongLE(satoshiVolume);
      return writer;
   }

   @Override
   public byte getType() {
      return ApiObject.EXCHANGE_TRADE_SUMMARY_TYPE;
   }

}
