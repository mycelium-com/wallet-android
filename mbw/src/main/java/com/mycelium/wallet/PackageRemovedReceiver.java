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

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.mycelium.wallet.activity.RestartPopupActivity;
import com.mycelium.wallet.activity.StartupActivity;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.List;

public class PackageRemovedReceiver extends BroadcastReceiver {
    public static void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        context.registerReceiver(new PackageRemovedReceiver(), filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getData() != null) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            String spvModuleName = WalletApplication.getSpvModuleName(WalletAccount.Type.BCHBIP44);
            String gebModuleName = BuildConfig.appIdGeb;

            int moduleString;
            boolean restartOnChange;
            if (packageName.equals(spvModuleName)) {
                moduleString = R.string.bch_module_change;
                restartOnChange = true;
            } else if (packageName.equals(gebModuleName)) {
                moduleString = R.string.geb_module_change;
                restartOnChange = false;
            } else {
                return;
            }
            switch (intent.getAction()) {
                case Intent.ACTION_PACKAGE_ADDED:
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        handlePackageChange(context, moduleString, R.string.installed, restartOnChange);
                    }
                    break;
                case Intent.ACTION_PACKAGE_REPLACED:
                    handlePackageChange(context, moduleString, R.string.updated, restartOnChange);
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        handlePackageChange(context, moduleString, R.string.removed, restartOnChange);
                    }
            }
        }
    }

    private void handlePackageChange(Context context, int moduleChangeStringId, int statusStringId, boolean restartRequired) {
        if (isAppOnForeground(context)) {
            showNotification(context, String.format(context.getString(moduleChangeStringId), context.getString(statusStringId)), restartRequired);
        } else if (statusStringId == R.string.installed){
            restart(context);
        } else {
            Runtime.getRuntime().exit(0);
        }
    }

    private void showNotification(Context context, String warningHeader, boolean restartRequired) {
        Intent intent = new Intent(context, RestartPopupActivity.class)
                .putExtra(RestartPopupActivity.RESTART_WARNING_HEADER, warningHeader)
                .putExtra(RestartPopupActivity.RESTART_REQUIRED, restartRequired)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void restart(Context context) {
        Intent futureActivity = new Intent(context, StartupActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                futureActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        Runtime.getRuntime().exit(0);
    }
}
