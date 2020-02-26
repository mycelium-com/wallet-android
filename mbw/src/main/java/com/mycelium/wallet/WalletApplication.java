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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.ModuleMessageReceiver;
import com.mycelium.wallet.activity.settings.SettingsPreference;
import com.mycelium.wallet.external.mediaflow.NewsSyncUtils;
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class WalletApplication extends MultiDexApplication implements ModuleMessageReceiver {
    private ModuleMessageReceiver moduleMessageReceiver;
    private static WalletApplication INSTANCE;
    private NetworkChangedReceiver networkChangedReceiver;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static WalletApplication getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException();
        }
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        LogManager.getLogManager().reset();
        Logger rootLogger = LogManager.getLogManager().getLogger("");

        rootLogger.addHandler(new CommonLogHandler());
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        int loadedBouncy = Security.insertProviderAt(new BouncyCastleProvider(), 1);
        if(loadedBouncy == -1) {
            Log.e("WalletApplication", "Failed to insert security provider");
        } else {
            Log.d("WalletApplication", "Inserted security provider");
        }
        INSTANCE = this;
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        super.onCreate();
        CommunicationManager.init(this);
        moduleMessageReceiver = new MbwMessageReceiver(this);
        applyLanguageChange(getBaseContext(), SettingsPreference.getLanguage());
        IntentFilter connectivityChangeFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        initNetworkStateHandler(connectivityChangeFilter);
        registerActivityLifecycleCallbacks(new ApplicationLifecycleHandler());
        PackageRemovedReceiver.register(getApplicationContext());
        if(isMainProcess()) {
            NewsDatabase.INSTANCE.initialize(this);
            if(SettingsPreference.getMediaFlowEnabled()) {
                NewsSyncUtils.startNewsUpdateRepeating(this);
            }
        }
        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic("all");

        UpdateConfigWorker.start(this);
    }

    private boolean isMainProcess() {
        String currentProcName = "";
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    currentProcName = processInfo.processName;
                    break;
                }
            }
        }
        return getPackageName().equals(currentProcName);
    }

    private void initNetworkStateHandler(IntentFilter connectivityChangeFilter) {
        networkChangedReceiver = new NetworkChangedReceiver();
        registerReceiver(networkChangedReceiver, connectivityChangeFilter);
    }

    public List<ModuleVersionError> moduleVersionErrors = new ArrayList<>();

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
    public int getIcon() {
        return moduleMessageReceiver.getIcon();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(networkChangedReceiver);
        UpdateConfigWorker.end(this);
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
                MbwManager mbwManager = MbwManager.getInstance(getApplicationContext());
                mbwManager.setAppInForeground(true);
                // as monitoring the connection state doesn't work in background, establish the
                // right connection state here.
                boolean connected = Utils.isConnected(getApplicationContext());
                // checking state of networkConnected variable only for WalletManager is sufficient
                // as ColuManager, Wapi and WalletManager networkConnected variables are changed simultaneously
                if (mbwManager.getWalletManager(false).isNetworkConnected() != connected) {
                    mbwManager.getWalletManager(false).setNetworkConnected(connected);
                    mbwManager.getWapi().setNetworkConnected(connected);
                }
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
                MbwManager.getInstance(getApplicationContext()).setAppInForeground(false);
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
