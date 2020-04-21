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


import android.text.TextUtils;
import com.mycelium.wallet.activity.FormattedLog;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.OutputViewModel;
import com.mycelium.wapi.wallet.TransactionSummary;
import com.mycelium.wapi.wallet.WalletAccount;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DataExport {
    private static final String CSV_HEADER = "Account, Transaction ID, Destination Address, Timestamp, Value, Currency, Transaction Label\n";

    private DataExport() {}
    public static File getTxHistoryCsv(WalletAccount account, List<TransactionSummary> history,
                                       MetadataStorage storage, File file) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file))) {
        osw.write(CSV_HEADER);
        String accountLabel = storage.getLabelByAccount(account.getId());
        Collections.sort(history);
        for (tTransactionSummary transaction : history) {
            String txLabel = storage.getLabelByTransaction(transaction.getIdHex());
                List<String> destAddresses = new ArrayList<>();
            for (OutputViewModel output : transaction.getOutputs()) {
                if (!account.isMineAddress(output.getAddress())) {
                        destAddresses.add(output.getAddress().toString());
                }
            }
                osw.write(getTxLine(accountLabel, txLabel, TextUtils.join(" ", destAddresses), transaction));
        }
        }
        return file;
    }

    public static File getLogsExport(@NotNull List<FormattedLog> logs, File file) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file))) {

        for (FormattedLog formattedLog : logs) {
            osw.write(formattedLog.toString());
            osw.write("\n");
        }

        }
        return file;
    }

    private static String getTxLine(String accountLabel, String txLabel, String destAddresses, TransactionSummary transaction) {
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
        df.setTimeZone(tz);
        String date = df.format(new Date(transaction.getTimestamp() * 1000L));
        String value = transaction.getTransferred().toPlainString();
        String name = transaction.getTransferred().type.getName();
        return
                escape(accountLabel) + "," +
                        transaction.getIdHex() + "," +
                        destAddresses + "," +
                        date + "," +
                        value + "," +
                        name + "," +
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
