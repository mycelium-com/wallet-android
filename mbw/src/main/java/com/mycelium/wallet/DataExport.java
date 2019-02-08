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


import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.GenericTransaction;
import com.mycelium.wapi.wallet.WalletAccount;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DataExport {
   private static final String CSV_HEADER = "Account, Transaction ID, Destination Address, Timestamp, Value, Currency, Transaction Label\n";

   public static File getTxHistoryCsv(WalletAccount account, List<GenericTransaction> history,
                                      MetadataStorage storage, File file) throws IOException {
      FileOutputStream fos = new FileOutputStream(file);
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      osw.write(CSV_HEADER);
      String accountLabel = storage.getLabelByAccount(account.getId());
      for (GenericTransaction summary : history) {
         String txLabel = storage.getLabelByTransaction(summary.getId());
         osw.write(getTxLine(accountLabel, txLabel, summary));
      }
      osw.close();
      return file;
   }

   private static String getTxLine(String accountLabel, String txLabel, GenericTransaction transaction) {
      TimeZone tz = TimeZone.getDefault();
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
      df.setTimeZone(tz);
      String date = df.format(new Date(transaction.getTimestamp() * 1000L));
      double value = transaction.getTransferred().abs().value;
      return
            escape(accountLabel) + "," +
                  transaction.getId() + "," +
                  date + "," +
                  value + "," +
                  transaction.getType().getName() + "," +
                  escape(txLabel) + "\n";
   }

   private static String escape(String input) {
      String output = input.replaceAll("\"", "\"\""); //replace all " with "" to escape them
      if (output.contains("\"") || output.contains(",") || output.contains("\n")) {
         return "\"" + output + "\""; //enclose in double quotes, if double quotes or comma or newline is present
      } else {
         return output;
      }
   }
}
