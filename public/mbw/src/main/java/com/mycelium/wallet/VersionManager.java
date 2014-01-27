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
