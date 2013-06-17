/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbw;

import java.math.BigDecimal;

import android.net.Uri;

/**
 * This is a crude implementation of a Bitcoin URI, but for now it works for our
 * purpose.
 */
public class BitcoinUri {

   private String _address;
   private long _amount;

   public BitcoinUri(String address, long amount) {
      _address = address;
      _amount = amount;
   }

   public String getAddress() {
      return _address;
   }

   public long getAmount() {
      return _amount;
   }

   public static BitcoinUri parse(String uri) {
      try {
         Uri u = Uri.parse(uri);
         String scheme = u.getScheme();
         if (!scheme.equalsIgnoreCase("bitcoin")) {
            // not a bitcoin URI
            return null;
         }
         String schemeSpecific = u.getSchemeSpecificPart();
         if (schemeSpecific.startsWith("//")) {
            // Fix for invalid bitcoin URI on the form "bitcoin://"
            schemeSpecific = schemeSpecific.substring(2);
         }
         u = Uri.parse("bitcoin://" + schemeSpecific);
         if (u == null) {
            return null;
         }
         String host = u.getHost();
         if (host == null || host.length() < 1) {
            return null;
         }
         String amountStr = u.getQueryParameter("amount");
         long amount = 0;
         if (amountStr != null) {
            amount = new BigDecimal(amountStr).movePointRight(8).toBigIntegerExact().longValue();
         }
         return new BitcoinUri(host, amount);

      } catch (Exception e) {
         return null;
      }
   }

}
