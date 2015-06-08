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

package com.mycelium.wallet.external.cashila.activity;

import com.google.common.base.Strings;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

// based on https://www.stuzza.at/de/download/qr-code/126-qr-code-und-bcd-definitionen/file.html
public class BcdCodedSepaData implements Serializable {

   public final String bic;
   public final String iban;
   public final String recipient;
   public final String reference;
   public final String displayText;
   public final BigDecimal amount;

   public static BcdCodedSepaData fromString(String bcdData) {
      String[] lines = bcdData.split("\\r?\\n");

      // first 7 lines are mandatory
      if (lines.length < 7) {
         return null;
      }

      if (!lines[0].equals("BCD")) {
         return null;
      }

      if (!lines[1].equals("001")) {
         return null;
      }

      int encodingId;
      try {
         encodingId = Integer.parseInt(lines[2]);
      } catch (NumberFormatException ex) {
         return null;
      }

      // todo
      String encoding;
      switch (encodingId) {
         case 1:
            encoding = "UTF-8";
            break;
         case 2:
            encoding = "ISO 8895-1";
            break;
         case 3:
            encoding = "ISO 8895-2";
            break;
         case 4:
            encoding = "ISO 8895-4";
            break;
         case 5:
            encoding = "ISO 8895-5";
            break;
         case 6:
            encoding = "ISO 8895-7";
            break;
         case 7:
            encoding = "ISO 8895-10";
            break;
         case 8:
            encoding = "ISO 8895-15";
            break;
         default:
            return null;
      }

      if (!lines[3].equals("SCT")) {
         return null;
      }

      final String bic = lines[4];
      final String recipient = lines[5];
      final String iban = lines[6];

      BigDecimal amount;
      if (lines.length > 7) {
         if (!lines[7].startsWith("EUR")) {
            return null;
         }

         // amount uses "." as decimal separator - eg: "EUR184.6"
         try {
            DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
            format.setParseBigDecimal(true);
            amount = (BigDecimal) format.parseObject(lines[7].substring(3));
         } catch (ParseException e) {
            return null;
         }

      } else {
         amount = BigDecimal.ZERO;
      }

      String code;
      if (lines.length > 8) {
         code = lines[8];
      } else {
         code = "";
      }

      // use either line9 (Reference) or line10 (Text) as Reference
      String reference = "";
      if (lines.length > 9) {
         reference = lines[9];
      }

      if (lines.length > 10) {
         if (!Strings.isNullOrEmpty(lines[10].trim())) {
            reference = lines[10];
         }
      }

      String displayText;
      if (lines.length > 11) {
         displayText = lines[11];
      } else {
         displayText = "";
      }


      return new BcdCodedSepaData(bic, iban, recipient, reference, displayText, amount);
   }

   private BcdCodedSepaData(String bic, String iban, String recipient, String reference, String displayText, BigDecimal amount) {
      this.bic = bic;
      this.iban = iban;
      this.recipient = recipient;
      this.reference = reference;
      this.displayText = displayText;
      this.amount = amount;
   }
}
