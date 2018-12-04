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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.mycelium.wallet.activity.UpdateNotificationActivity;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AsynchronousApi;
import com.mycelium.wallet.event.FeatureWarningsAvailable;
import com.mycelium.wallet.event.NewWalletVersionAvailable;
import com.mycelium.wallet.event.WalletVersionExEvent;
import com.mycelium.wapi.api.request.VersionInfoExRequest;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.api.response.FeatureWarning;
import com.mycelium.wapi.api.response.VersionInfoExResponse;
import com.mycelium.wapi.api.response.WarningKind;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VersionManager {
   private static final long PERIODIC_BACKGROUND_CHECK_MS = 1000 * 60 * 60 * 4;  // every 4h
   public static final String BRANCH = "android";

   private final SharedPreferences preferences;
   private final Set<String> ignoredVersions;
   private Context context;
   private final String language;
   private final AsynchronousApi asyncApi;
   private final Handler backgroundHandler;
   private final Bus eventBus;
   private WalletVersionExEvent lastVersionResult;
   private Dialog lastDialog;

   public VersionManager(Context context, String language, AsynchronousApi asyncApi, final Bus eventBus) {
      this.context = context;
      this.language = language;
      this.asyncApi = asyncApi;
      this.preferences = context.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
      String ignored = preferences.getString(Constants.IGNORED_VERSIONS, "");
      ignoredVersions = Sets.newHashSet(Splitter.on("\n").omitEmptyStrings().split(ignored));

      this.eventBus = eventBus;
      new Handler(context.getMainLooper()).post(new Runnable() {
         @Override
         public void run() {
            eventBus.register(VersionManager.this);
         }
      });

      backgroundHandler = new Handler(context.getMainLooper());
   }

   @Override
   protected void finalize() throws Throwable {
      new Handler(context.getMainLooper()).post(new Runnable() {
         @Override
         public void run() {
            eventBus.unregister(VersionManager.this);
         }
      });
      super.finalize();
   }

   public void showVersionDialog(final VersionInfoExResponse response, final Context activity) {
      Intent intent = new Intent(activity, UpdateNotificationActivity.class);
      intent.putExtra(UpdateNotificationActivity.RESPONSE,response);
      activity.startActivity(intent);
   }

   public void initBackgroundVersionChecker() {
      backgroundCheck.run();
   }

   // if app is currently in foreground, make an api call to check if there are some warnings
   // or a version update
   private Runnable backgroundCheck = new Runnable(){
      @Override
      public void run() {
         if (isAppOnForeground()){
            checkForUpdate();
         }

         // call this runnable periodically
         backgroundHandler.postDelayed(backgroundCheck, PERIODIC_BACKGROUND_CHECK_MS);
      }

      private boolean isAppOnForeground() {
         ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
         List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
         if (appProcesses == null) {
            return false;
         }
         final String packageName = context.getPackageName();
         for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
               return true;
            }
         }
         return false;
      }
   };

   public void checkForUpdate() {
      VersionInfoExRequest req = new VersionInfoExRequest(BRANCH, BuildConfig.VERSION_NAME, new Locale(language));
      //asyncApi.getWalletVersionExTestHelper(req);
      asyncApi.getWalletVersionEx(req);
   }

   public void checkForUpdateSync(AbstractCallbackHandler<VersionInfoExResponse> callback) {
      VersionInfoExRequest req = new VersionInfoExRequest(BRANCH, BuildConfig.VERSION_NAME, new Locale(language));
      asyncApi.getWalletVersionEx(req, callback);
   }

   private boolean isIgnored(String versionNumber) {
      return isSameVersion(versionNumber) || isIgnoredVersion(versionNumber);
   }

   @Subscribe
   public void getWalletVersionExResult(final WalletVersionExEvent result) {
      // if the last result had no featureWarnings and now we have some, broadcast them.
      if ((lastVersionResult == null || lastVersionResult.response.featureWarnings == null || lastVersionResult.response.featureWarnings.size() == 0)
            && (result.response.featureWarnings != null && result.response.featureWarnings.size() > 0) )
      {
         eventBus.post(new FeatureWarningsAvailable(result.response));
      }

      lastVersionResult = result;
      // is the reported version newer than we and did the user not ignore the information
      NewWalletVersionAvailable walletUpdateAvailable = isWalletUpdateAvailable();
      if (walletUpdateAvailable != null){
         eventBus.post(walletUpdateAvailable);
      }
   }

   @Produce
   public FeatureWarningsAvailable areFeatureWarningsAvailable(){
      if (lastVersionResult != null && lastVersionResult.response.featureWarnings != null && lastVersionResult.response.featureWarnings.size() > 0){
         return new FeatureWarningsAvailable(lastVersionResult.response);
      } else {
         return null;
      }
   }

   @Produce
   public NewWalletVersionAvailable isWalletUpdateAvailable(){
      if (lastVersionResult != null
            && lastVersionResult.response.versionNumber != null
            && !isIgnored(lastVersionResult.response.versionNumber)){
         return new NewWalletVersionAvailable(lastVersionResult.response);
      } else {
         return null;
      }
   }

   private Optional<VersionInfoExResponse> getLastVersionResult(){
      if (lastVersionResult == null) {
         return Optional.absent();
      } else {
         return Optional.of(lastVersionResult.response);
      }
   }

   // check if there is a warning issued for a certain feature
   private Optional<FeatureWarning> getFeatureWarning(Feature forFeature){
      if (!getLastVersionResult().isPresent()
            || getLastVersionResult().get().featureWarnings == null || getLastVersionResult().get().featureWarnings.size() == 0 ) {
         return Optional.absent();
      } else {
         List<FeatureWarning> featureWarnings = lastVersionResult.response.featureWarnings;
         for (FeatureWarning w : featureWarnings){
            if (w.feature.equals(Feature.GENERAL) || w.feature.equals(forFeature)){
               return Optional.of(w);
            }
         }
         return Optional.absent();
      }
   }

   /**
    * Check if we know about a warning for a certain feature/component of our app or external service
    * if so, show the user a warning dialog with the message from the server.
    *
    * @param context calling context
    * @param forFeature check if we have a warning for this feature
    */
   public void showFeatureWarningIfNeeded(Context context, Feature forFeature){
      showFeatureWarningIfNeeded(context, forFeature, false, null);
   }

   /**
    * Check if we know about a warning for a certain feature/component of our app or external service
    * if so, show the user a warning dialog with the message from the server.
    *
    * Depending on the kind of the warning the feature might be blocked and will not be executed
    *
    * @param context calling context
    * @param forFeature check if we have a warning for this feature
    * @param allowBlocking set to false if this is a core function and should never get blocked, no matter what the server reports
    * @param runFeature this runnable gets called if there is no warning or the user accepts it
    */
   public void showFeatureWarningIfNeeded(final Context context, final Feature forFeature, final boolean allowBlocking, final Runnable runFeature){
      final Optional<FeatureWarning> featureWarning = getFeatureWarning(forFeature);
      if (featureWarning.isPresent()){
         // if dialog is still shown, dont create a new one
         if (lastDialog != null && lastDialog.isShowing()){
            return;
         }

         CharSequence msg = new SpannableString(featureWarning.get().warningMessage);

         // include clickable URI in text if one is set
         if (featureWarning.get().link != null) {
            SpannableString link = new SpannableString(featureWarning.get().link.toString());
            Linkify.addLinks(link, Linkify.WEB_URLS);
            msg = TextUtils.concat(msg, "\n\n", link);
         }

         TextView text = new TextView(context);
         text.setText(msg);
         text.setMovementMethod(LinkMovementMethod.getInstance());

         int padding = (int) (context.getResources().getDisplayMetrics().density * 10.0 + 0.5);
         text.setPadding(padding, padding, padding, padding);

         AlertDialog.Builder dialog = new AlertDialog.Builder(context);

         // build the dialog depending on the kind of the warning
         if (featureWarning.get().warningKind.equals(WarningKind.WARN)) {
            dialog.setTitle("Warning");

            // add a additional cancel button to emphasize that you might cancel this action (back button works always)
            if (runFeature != null){
               dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                  }
               });
            }

            //text.setTextColor(context.getResources().getColor(R.color.red));
            dialog.setIcon(R.drawable.holo_dark_ic_action_warning_yellow);
         } else if (featureWarning.get().warningKind.equals(WarningKind.BLOCK)) {
            dialog.setTitle("Temporary deactivated");
         } else {
            dialog.setTitle("Information");
         }

         // only allow to execute the feature if its WARN or INFO or we dont allow a blocking warning here
         if (!allowBlocking ||
               featureWarning.get().warningKind.equals(WarningKind.WARN) ||
               featureWarning.get().warningKind.equals(WarningKind.INFO)) {

            dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                  if (runFeature != null) {
                     runFeature.run();
                  }
                  dialog.dismiss();
               }
            });
         } else {
            dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                  dialog.dismiss();
               }
            });
         }

         // set the content
         dialog.setView(text);

         // show dialog
         lastDialog = dialog.create();
         lastDialog.show();
      } else {
         // no warning issued - call runFeature unconditionally
         if (runFeature != null) {
            runFeature.run();
         }
      }
   }

   // closes the dialog, if any is shown
   public void closeDialog(){
      if (lastDialog != null) {
         lastDialog.dismiss();
         lastDialog = null;
      }
   }

   public boolean isSameVersion(String versionNumber) {
      return versionNumber.equals(BuildConfig.VERSION_NAME);
   }

   public void ignoreVersion(String versionNumber) {
      ignoredVersions.add(versionNumber);
      getEditor()
              .putString(Constants.IGNORED_VERSIONS, Joiner.on("\n").join(ignoredVersions))
              .apply();
   }

   private boolean isIgnoredVersion(String versionNumber) {
      return ignoredVersions.contains(versionNumber);
   }

   private SharedPreferences.Editor getEditor() {
      return preferences.edit();
   }

   public void showIfRelevant(VersionInfoExResponse response, Context modernMain) {
      if (isIgnored(response.versionNumber)) {
         return;
      }
      showVersionDialog(response, modernMain);
   }
}
