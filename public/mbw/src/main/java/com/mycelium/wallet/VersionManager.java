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

package com.mycelium.wallet;

import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import com.mrd.mbwapi.api.WalletVersionRequest;
import com.mrd.mbwapi.api.WalletVersionResponse;
import com.mycelium.wallet.activity.UpdateNotificationActivity;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AsynchronousApi;

public class VersionManager {
   private static final long ONE_WEEK_IN_MILLIS = 1000 * 60 * 60 * 24 * 7;

   private final SharedPreferences preferences;
   private final Set<String> ignoredVersions;
   private final String version;
   private final String language;
   private final AsynchronousApi asyncApi;
   private long lastUpdateCheck;

   public VersionManager(Context context, String language, AsynchronousApi asyncApi, String version) {
      this.language = language;
      this.asyncApi = asyncApi;
      this.preferences = context.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
      this.version = version;
      String ignored = preferences.getString(Constants.IGNORED_VERSIONS, "");
      ignoredVersions = Sets.newHashSet(Splitter.on("\n").omitEmptyStrings().split(ignored));
      lastUpdateCheck = preferences.getLong(Constants.LAST_UPDATE_CHECK, 0);

   }

   public static String determineVersion(Context applicationContext) {
      try {
         PackageManager packageManager = applicationContext.getPackageManager();
         if (packageManager != null) {
            final PackageInfo pInfo;
            pInfo = packageManager.getPackageInfo(applicationContext.getPackageName(), 0);
            return pInfo.versionName;
         } else {
            Log.i(Constants.TAG, "unable to obtain packageManager");
         }
      } catch (PackageManager.NameNotFoundException ignored) {
      }
      return "unknown";
   }

   public void showVersionDialog(final WalletVersionResponse response, final Context activity) {
      Intent intent = new Intent(activity, UpdateNotificationActivity.class);
      intent.putExtra(UpdateNotificationActivity.RESPONSE,response);
      activity.startActivity(intent);
   }

   public void checkForUpdate() {
      if (isWeeklyCheckDue()) {
         WalletVersionRequest req = new WalletVersionRequest(version, new Locale(language));
         asyncApi.getWalletVersion(req);
         checkedForVersionUpdate();
      }
   }

   public void forceCheckForUpdate(AbstractCallbackHandler<WalletVersionResponse> callback) {
      WalletVersionRequest req = new WalletVersionRequest(version, new Locale(language));
      asyncApi.getWalletVersion(req, callback);
   }

   public String getVersion() {
      return version;
   }

   boolean isIgnored(String versionNumber) {
      return isSameVersion(versionNumber) || isIgnoredVersion(versionNumber);

   }

   public boolean isSameVersion(String versionNumber) {
      return versionNumber.equals(version);
   }

   public void ignoreVersion(String versionNumber) {
      SharedPreferences.Editor editor = getEditor();
      ignoredVersions.add(versionNumber);
      editor.putString(Constants.IGNORED_VERSIONS, Joiner.on("\n").join(ignoredVersions));
      editor.commit();
   }


   private boolean isIgnoredVersion(String versionNumber) {
      return ignoredVersions.contains(versionNumber);
   }

   private SharedPreferences.Editor getEditor() {
      return preferences.edit();
   }

   private boolean isWeeklyCheckDue() {
      return (System.currentTimeMillis() - ONE_WEEK_IN_MILLIS > lastUpdateCheck);
   }

   private void checkedForVersionUpdate() {
      lastUpdateCheck = System.currentTimeMillis();
      getEditor().putLong(Constants.LAST_UPDATE_CHECK, lastUpdateCheck).commit();
   }


   public void showIfRelevant(WalletVersionResponse response, Context modernMain) {
      if (isIgnored(response.versionNumber)) {
         return;
      }
      showVersionDialog(response, modernMain);
   }


}
