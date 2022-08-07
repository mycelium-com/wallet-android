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

package com.mycelium.wallet.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mycelium.generated.logger.database.Logs;
import com.mycelium.wallet.DataExport;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionLogsActivity extends Activity {

    private final static long MAX_TO_SHOW = 100;

    public static void callMe(Activity activity) {
        Intent intent = new Intent(activity, ConnectionLogsActivity.class);
        activity.startActivity(intent);
    }

    private Logger _logger = Logger.getLogger(ConnectionLogsActivity.class.getSimpleName());
    @SuppressLint("SimpleDateFormat")
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.show_logs_activity);

        final String logsForPrintString = Joiner.on("\n").join(getFormattedLogs(MAX_TO_SHOW));

        TextView tvLogDisplay = (TextView) findViewById(R.id.tvLogDisplay);
        tvLogDisplay.setText(logsForPrintString);
        tvLogDisplay.setHorizontallyScrolling(true);
        tvLogDisplay.setMovementMethod(new ScrollingMovementMethod());

        tvLogDisplay.setOnLongClickListener(view -> {
            Utils.setClipboardString(logsForPrintString, ConnectionLogsActivity.this);
            new Toaster(ConnectionLogsActivity.this).toast(R.string.copied_to_clipboard, true);
            return true;
        });
        findViewById(R.id.btShare).setOnClickListener(v -> shareLogs());
    }

    private ArrayList<FormattedLog> getFormattedLogs(@Nullable Long limit) {
        final MbwManager mbwManager = MbwManager.getInstance(this);
        ArrayList<FormattedLog> formattedLogs = new ArrayList<>();
        List<Logs> lastLogs = limit != null ? mbwManager.getLastLogsDesc(limit) : mbwManager.getLogsAsc();
        for (Logs log : lastLogs) {
            formattedLogs.add(new FormattedLog(log));
        }
        return formattedLogs;
    }

    private void shareLogs() {
        try {
            String fileName = "MBW-" + dateFormat.format(new Date()) + ".txt";
            File logsExport = DataExport.getLogsExport(getFormattedLogs(null), getFileStreamPath(fileName));
            PackageManager packageManager = Preconditions.checkNotNull(this.getPackageManager());
            PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), PackageManager.GET_PROVIDERS);
            for (ProviderInfo info : packageInfo.providers) {
                if (info.name.equals("androidx.core.content.FileProvider")) {
                    String authority = info.authority;
                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), authority, logsExport);
                    Intent intent = new ShareCompat.IntentBuilder(this)
                            .setStream(uri)  // uri from FileProvider
                            .setType("*/*")
                            .setSubject(getResources().getString(R.string.connection_logs))
                            .setText(getResources().getString(R.string.connection_logs))
                            .getIntent()
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    List<ResolveInfo> resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
                }
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            new Toaster(this).toast("Export failed. Check your logs", false);
            _logger.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
    }
}
