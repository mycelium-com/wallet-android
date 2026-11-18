package com.mycelium.wallet

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.StrictMode
import android.util.Log
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.mycelium.modularizationtools.CommunicationManager.Companion.init
import com.mycelium.modularizationtools.ModuleMessageReceiver
import com.mycelium.wallet.UpdateConfigWorker.Companion.end
import com.mycelium.wallet.UpdateConfigWorker.Companion.start
import com.mycelium.wallet.activity.PinProtectedActivity
import com.mycelium.wallet.activity.StartupActivity
import com.mycelium.wallet.activity.export.ShamirSharingActivity
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.Toaster.Companion.onStop
import com.mycelium.wallet.activity.settings.SettingsPreference.getLanguage
import com.mycelium.wallet.activity.settings.SettingsPreference.mediaFlowEnabled
import com.mycelium.wallet.external.mediaflow.NewsSyncUtils.startNewsUpdateRepeating
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Level
import java.util.logging.Logger

class WalletApplication : Application(), ModuleMessageReceiver {
    private var moduleMessageReceiver: ModuleMessageReceiver? = null
    private var networkChangedReceiver: NetworkChangedReceiver? = null
    private var checkNetworkTimer: Timer? = null
    private val logger: Logger = Logger.getLogger(WalletApplication::class.java.simpleName)

    override fun onCreate() {
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        val loadedBouncy = Security.insertProviderAt(BouncyCastleProvider(), 1)
        if (loadedBouncy == -1) {
            Log.e("WalletApplication", "Failed to insert security provider")
        } else {
            Log.d("WalletApplication", "Inserted security provider")
        }
        INSTANCE = this
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        super.onCreate()
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: GooglePlayServicesRepairableException) {
            // Prompt the user to install/update/enable Google Play services.
            GoogleApiAvailability.getInstance().showErrorNotification(this, e.connectionStatusCode)
        } catch (ignore: GooglePlayServicesNotAvailableException) {
        }
        init(this)
        moduleMessageReceiver = MbwMessageReceiver(this)
        Companion.applyLanguageChange(baseContext, getLanguage()!!)
        val connectivityChangeFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        initNetworkStateHandler(connectivityChangeFilter)
        registerActivityLifecycleCallbacks(ApplicationLifecycleHandler())
        PackageRemovedReceiver.register(applicationContext)
        if (isMainProcess()) {
            // Shamir share PDFs are left in the cache if the process dies while the share chooser is open
            // (ShamirSharingActivity.onDestroy never runs then)
            Thread { ShamirSharingActivity.deleteExportedPdfs(this) }.start()
            NewsDatabase.initialize(this)
            if (mediaFlowEnabled) {
                startNewsUpdateRepeating(this)
            }
        }
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().subscribeToTopic("all")

        start(this)
    }

    private fun isMainProcess(): Boolean {
        var currentProcName: String? = ""
        val pid = Process.myPid()
        val manager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager?
        if (manager != null) {
            val runningAppProcesses = manager.runningAppProcesses
            if (runningAppProcesses != null) {
                for (processInfo in runningAppProcesses) {
                    if (processInfo.pid == pid) {
                        currentProcName = processInfo.processName
                        break
                    }
                }
            }
        }
        return packageName == currentProcName
    }

    private fun initNetworkStateHandler(connectivityChangeFilter: IntentFilter) {
        networkChangedReceiver = NetworkChangedReceiver()
        ContextCompat.registerReceiver(
            this,
            networkChangedReceiver,
            connectivityChangeFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    @JvmField
    var moduleVersionErrors: MutableList<ModuleVersionError?> = ArrayList<ModuleVersionError?>()

    override fun onConfigurationChanged(newConfig: Configuration) {
        setupLanguage()
        super.onConfigurationChanged(newConfig)
    }

    fun setupLanguage() {
        val setLanguage = MbwManager.getInstance(this).language
        if (Locale.getDefault().language != setLanguage) {
            applyLanguageChange(baseContext, setLanguage)
        }
    }

    override fun onMessage(callingPackageName: String, intent: Intent) {
        moduleMessageReceiver!!.onMessage(callingPackageName, intent)
    }

    override fun getIcon(): Int {
        return moduleMessageReceiver!!.getIcon()
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(networkChangedReceiver)
        end(this)
    }

    private inner class ApplicationLifecycleHandler : ActivityLifecycleCallbacks {
        private var numStarted = 0
        private var numOfCreated = 0

        // so we would understand if app was just created, or restored from background
        private var isBackground = true

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            numOfCreated++
            val mbwManager = MbwManager.getInstance(applicationContext)
            mbwManager.activityCount = numOfCreated
            if (activity !is StartupActivity && activity !is PinProtectedActivity && mbwManager.isUnlockPinRequired) {
                // The process was recreated (or the startup PIN was re-armed) while the system restores a deeper
                // screen: route through StartupActivity so "Require PIN on start" is enforced.
                val redirect = Intent(activity, StartupActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                val launchedPlain = activity is ModernMain &&
                        (activity.intent.action == null || activity.intent.action == Intent.ACTION_MAIN)
                // deep link (gift box, exchange, POP…) or a restored screen: reopen it after the PIN.
                // Handed over in memory, not as an extra (exported StartupActivity, Play "Intent Redirection").
                StartupActivity.pendingRedirect =
                        if (launchedPlain) null else Intent(activity.intent).setClass(activity, activity.javaClass)
                activity.startActivity(redirect)
                activity.finish()
            }
        }

        override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
            super.onActivityPostCreated(activity, savedInstanceState)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val view = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                view?.setOnApplyWindowInsetsListener { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
                    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = insets.top
                        leftMargin = insets.left
                        bottomMargin = insets.bottom
                        rightMargin = insets.right
                    }
                    WindowInsets.CONSUMED
                }
            }
        }

        override fun onActivityStarted(activity: Activity) {
            setupLanguage()
            if (numStarted == 0 && isBackground) {
                logger.log(Level.INFO, "Went to foreground")
                // app returned from background
                val mbwManager = MbwManager.getInstance(applicationContext)
                mbwManager.isAppInForeground = true
                // as monitoring the connection state doesn't work in background, establish the
                // right connection state here.
                // delay the check so that the state has room to switch between blocked and connected
                // statuses when returning back from idle (doze) mode
                checkNetworkTimer = Timer()
                checkNetworkTimer!!.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        val connected =
                            Utils.isConnected(applicationContext, "went foreground")
                        if (mbwManager.getWalletManager(false).isNetworkConnected != connected) {
                            mbwManager.getWalletManager(false).isNetworkConnected = connected
                            mbwManager.wapi.setNetworkConnected(connected)
                        }
                        if (connected) {
                            checkNetworkTimer!!.cancel()
                        }
                    }
                }, 0, 1000)
                isBackground = false
            }
            numStarted++
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            numStarted--
            if (numStarted == 0) {
                // app is going background
                MbwManager.getInstance(applicationContext).isAppInForeground = false
                if (checkNetworkTimer != null) {
                    checkNetworkTimer!!.cancel()
                }
                isBackground = true
                logger.log(Level.INFO, "Went to background")
                onStop()
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            numOfCreated--
            MbwManager.getInstance(applicationContext).activityCount = numOfCreated
        }
    }

    inner class ModuleVersionError private constructor(
        @JvmField val moduleId: String?,
        @JvmField val expected: Int
    )

    companion object {
        private var INSTANCE: WalletApplication? = null

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        @JvmStatic
        fun getInstance(): WalletApplication {
            checkNotNull(INSTANCE)
            return INSTANCE!!
        }

        @JvmStatic
        fun applyLanguageChange(context: Context, lang: String) {
            Log.i(Constants.TAG, "switching to lang $lang")
            val config = context.resources.configuration
            if ("" != lang) {
                val locale = stringToLocale(lang)
                if (config.locale != locale) {
                    Locale.setDefault(locale)
                    config.setLocale(locale)
                    context.resources.updateConfiguration(
                        config,
                        context.resources.displayMetrics
                    )
                }
            }
        }

        private fun stringToLocale(lang: String): Locale =
            when (lang) {
                "zh-CN", "zh" -> Locale.SIMPLIFIED_CHINESE
                "zh-TW" -> Locale.TRADITIONAL_CHINESE
                else -> Locale(lang)
            }
    }
}
