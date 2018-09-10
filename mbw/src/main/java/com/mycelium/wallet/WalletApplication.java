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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.ModuleMessageReceiver;
import com.mycelium.wallet.activity.settings.SettingsPreference;
import com.mycelium.wallet.modularisation.BCHHelper;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletApplication extends MultiDexApplication implements ModuleMessageReceiver {
    private ModuleMessageReceiver moduleMessageReceiver;
    private static WalletApplication INSTANCE;
    private NetworkChangedReceiver networkChangedReceiver;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private MbwManager mbwManager;

    public static WalletApplication getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException();
        }
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        int loadedBouncy = Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        if(loadedBouncy == -1) {
            Log.e("WalletApplication", "Failed to insert spongy castle provider");
        } else {
            Log.d("WalletApplication", "Inserted spongy castle provider");
        }
        SettingsPreference.getInstance().init(this);
        INSTANCE = this;
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                   .detectAll()
                                   .penaltyLog()
                                   .build());
        }
        super.onCreate();
        CommunicationManager.init(this);
        pairSpvModules(CommunicationManager.getInstance());
        cleanModulesIfFirstRun(this, getSharedPreferences(BCHHelper.BCH_PREFS, MODE_PRIVATE));
        moduleMessageReceiver = new MbwMessageReceiver(this);
        mbwManager = MbwManager.getInstance(this);
        applyLanguageChange(getBaseContext(), mbwManager.getLanguage());
        IntentFilter connectivityChangeFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        initNetworkStateHandler(connectivityChangeFilter);
        registerActivityLifecycleCallbacks(new ApplicationLifecycleHandler());
    }

    private void initNetworkStateHandler(IntentFilter connectivityChangeFilter) {
        networkChangedReceiver = new NetworkChangedReceiver();
        registerReceiver(networkChangedReceiver, connectivityChangeFilter);
    }

    public List<ModuleVersionError> moduleVersionErrors = new ArrayList<>();
    private void pairSpvModules(CommunicationManager communicationManager) {

        try {
            communicationManager.requestPair(BuildConfig.appIdSpvBch);
        } catch(SecurityException se) {
            String message = se.getMessage();
            if(message.contains("Version conflict")) {
                String[] strings = message.split("\\|");
                int otherModuleApiVersion = Integer.decode(strings[1]);
                moduleVersionErrors.add(new ModuleVersionError(BuildConfig.appIdSpvBch, otherModuleApiVersion));
            } else {
                Log.w("WalletApplication", message);
            }
        }

    }

    private void cleanModulesIfFirstRun(Context context, SharedPreferences sharedPreferences) {
        if (!sharedPreferences.getBoolean(BCHHelper.BCH_FIRST_UPDATE, false) && BCHHelper.isModulePaired(context)) {
            MbwManager.getInstance(context).getSpvBchFetcher().forceCleanCache();
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

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(networkChangedReceiver);
    }

    public static <T> String getSpvModuleName(Class<T> type) {
        if (type.isAssignableFrom(Bip44BCHAccount.class) ||
                type.isAssignableFrom(SingleAddressBCHAccount.class)) {
            return BuildConfig.appIdSpvBch;
        } else {
            throw new RuntimeException("No spv module defined for account type " + type.getSimpleName());
        }
    }

    public static <T> void sendToSpv(Intent intent, Class<T> type) {
        CommunicationManager.getInstance().send(getSpvModuleName(type), intent);
    }

    public static void sendToTsm(Intent intent) {
        CommunicationManager.getInstance().send(BuildConfig.appIdTsm, intent);
    }

    private class ApplicationLifecycleHandler implements ActivityLifecycleCallbacks {
        private int numStarted = 0;
        private int numOfCreated = 0;
        // so we would understand if app was just created, or restored from background
        private boolean isBackground = false;

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            numOfCreated++;
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (numStarted == 0 && isBackground) {
                // app returned from background
                WalletApplication.this.mbwManager.getWapi().setAppInForeground(true);
                isBackground = false;
            }
            numStarted++;
        }

        @Override
        public void onActivityResumed(Activity activity) {}

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivityStopped(Activity activity) {
            numStarted--;
            if (numStarted == 0) {
                // app is going background
                WalletApplication.this.mbwManager.getWapi().setAppInForeground(false);
                isBackground = true;
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

        @Override
        public void onActivityDestroyed(Activity activity) {
            numOfCreated--;
        }
    }

    public class ModuleVersionError {
        public final String moduleId;
        public final int expected;

        private ModuleVersionError(String moduleId, int expected) {
            this.moduleId = moduleId;
            this.expected = expected;
        }
    }
}
