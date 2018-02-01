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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.ModuleMessageReceiver;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WalletApplication extends MultiDexApplication implements ModuleMessageReceiver {
   private ModuleMessageReceiver moduleMessageReceiver;
   private static WalletApplication INSTANCE;

   private static Map<WalletAccount.Type, String> spvModulesMapping = initTrustedSpvModulesMapping();

   public static WalletApplication getInstance() {
      if (INSTANCE == null) {
         throw new IllegalStateException();
      }
      return INSTANCE;
   }

   @Override
   public void onCreate() {
      INSTANCE = this;
      if (BuildConfig.DEBUG) {
         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                 .detectAll()
                 .penaltyLog()
                 .build());
      }
      super.onCreate();
      pairSpvModules(CommunicationManager.getInstance(this));
      moduleMessageReceiver = new MbwMessageReceiver(this);
      MbwManager mbwManager = MbwManager.getInstance(this);
      applyLanguageChange(getBaseContext(), mbwManager.getLanguage());
   }

   private void pairSpvModules(CommunicationManager communicationManager) {
      for (Map.Entry<WalletAccount.Type, String> entry : spvModulesMapping.entrySet()) {
         communicationManager.requestPair(entry.getValue());
      }
   }

   @Override
   public void onConfigurationChanged(Configuration newConfig) {
      String setLanguage = MbwManager.getInstance(this).getLanguage();
      if (!Locale.getDefault().getLanguage().equals(setLanguage)) {
         applyLanguageChange(getBaseContext(), setLanguage);
      }
      super.onConfigurationChanged(newConfig);
   }

   public static void applyLanguageChange(Context context, String lang) {
      Log.i(Constants.TAG, "switching to lang " + lang);
      Configuration config = context.getResources().getConfiguration();
      if (!"".equals(lang)) {
         Locale locale = stringToLocale(lang);
         if (!config.locale.equals(locale)) {
            Locale.setDefault(locale);
            config.setLocale(locale);
            context.getResources().updateConfiguration(config,
                  context.getResources().getDisplayMetrics());
         }
      }
   }

   private static Locale stringToLocale(String lang) {
      switch (lang) {
         case "zh-CN":
         case "zh":
            return Locale.SIMPLIFIED_CHINESE;
         case "zh-TW":
            return Locale.TRADITIONAL_CHINESE;
         default:
            return new Locale(lang);
      }
   }

   @Override
   public void onMessage(@NonNull String callingPackageName, @NonNull Intent intent) {
      moduleMessageReceiver.onMessage(callingPackageName, intent);
   }

   public static String getSpvModuleName(WalletAccount.Type accountType) {
      if (spvModulesMapping.containsKey(accountType)) {
         return spvModulesMapping.get(accountType);
      } else {
         throw new RuntimeException("No spv module defined for account type " + accountType);
      }
   }

   public static void sendToSpv(Intent intent, WalletAccount.Type accountType) {
      CommunicationManager.getInstance(INSTANCE).send(getSpvModuleName(accountType), intent);
   }

   private static Map<WalletAccount.Type, String> initTrustedSpvModulesMapping() {
      Map<WalletAccount.Type, String> spvModulesMapping = new HashMap<>();
      switch (BuildConfig.APPLICATION_ID) {
         case "com.mycelium.wallet":
            spvModulesMapping.put(WalletAccount.Type.BCHBIP44, "com.mycelium.module.spvbch");
            spvModulesMapping.put(WalletAccount.Type.BCHSINGLEADDRESS, "com.mycelium.module.spvbch");
            break;
         case "com.mycelium.wallet.debug":
            spvModulesMapping.put(WalletAccount.Type.BCHBIP44, "com.mycelium.module.spvbch.debug");
            spvModulesMapping.put(WalletAccount.Type.BCHSINGLEADDRESS, "com.mycelium.module.spvbch.debug");
            break;
         case "com.mycelium.testnetwallet":
            spvModulesMapping.put(WalletAccount.Type.BCHBIP44, "com.mycelium.module.spvbch.testnet");
            spvModulesMapping.put(WalletAccount.Type.BCHSINGLEADDRESS, "com.mycelium.module.spvbch.testnet");
            break;
         case "com.mycelium.testnetwallet.debug":
            spvModulesMapping.put(WalletAccount.Type.BCHBIP44, "com.mycelium.module.spvbch.testnet.debug");
            spvModulesMapping.put(WalletAccount.Type.BCHSINGLEADDRESS, "com.mycelium.module.spvbch.testnet.debug");
            break;
         default:
            throw new RuntimeException("No spv module defined for BuildConfig " + BuildConfig.APPLICATION_ID);
      }
      return spvModulesMapping;
   }
}
