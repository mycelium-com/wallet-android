package com.mycelium.wallet.activity.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.send.SendCoinsActivity.Companion.getIntent
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.interruptSync
import com.squareup.otto.Subscribe
import java.lang.NullPointerException
import java.util.*

class SendInitializationActivity : Activity() {
    private val mbwManager: MbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    private lateinit var account: WalletAccount<*>
    private var uri: AssetUri? = null
    private var isColdStorage = false
    private var synchronizingHandler: Handler? = null
    private var slowNetworkHandler: Handler? = null
    private var rawPr: ByteArray? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_initialization_activity)
        // Get intent parameters
        val accountId = intent.getSerializableExtra("account") as UUID
        uri = intent.getSerializableExtra("uri") as? AssetUri
        rawPr = intent.getByteArrayExtra("rawPr")
        isColdStorage = intent.getBooleanExtra("isColdStorage", false)
        val crashHint = intent.extras!!.keySet().joinToString() + " (account id was $accountId)"
        val walletManager = mbwManager.getWalletManager(isColdStorage)
        account = walletManager.getAccount(accountId)
            ?: throw NullPointerException(crashHint)
        if (!isColdStorage) {
            continueIfReadyOrNonUtxos()
        }
        interruptOtherSyncs(account, walletManager)
    }

    /**
     * Interrupt all syncing accounts to free up resources for the current user interaction.
     * Do so without blocking the current thread.
     */
    private fun interruptOtherSyncs(account: WalletAccount<*>, walletManager: WalletManager) {
        walletManager.getAllActiveAccounts().filter { it != account }.interruptSync()
    }

    override fun onResume() {
        if (isFinishing) {
            return
        }
        MbwManager.getEventBus().register(this)

        // Show delayed messages so the user does not grow impatient
        synchronizingHandler = Handler().also {
            it.postDelayed(showSynchronizing, 2000)
        }
        slowNetworkHandler = Handler().also {
            it.postDelayed(showSlowNetwork, 6000)
        }

        // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
        if (mbwManager.currencySwitcher.getExchangeRatePrice(account.coinType) == null) {
            mbwManager.exchangeRateManager.requestRefresh()
        }

        // If we are in cold storage spending mode we wish to synchronize the wallet
        if (isColdStorage) {
            mbwManager.getWalletManager(true).startSynchronization()
        } else {
            continueIfReadyOrNonUtxos()
        }
        super.onResume()
    }

    override fun onPause() {
        synchronizingHandler?.removeCallbacks(showSynchronizing)
        slowNetworkHandler?.removeCallbacks(showSlowNetwork)
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    private val showSynchronizing = Runnable { findViewById<View>(R.id.tvSynchronizing).visibility = View.VISIBLE }
    private val showSlowNetwork = Runnable { findViewById<View>(R.id.tvSlowNetwork).visibility = View.VISIBLE }

    @Subscribe
    fun syncFailed(event: SyncFailed?) {
        Utils.toastConnectionError(this)
        // If we are in cold storage spending mode there is no point in continuing.
        // If we continued we would think that there were no funds on the private key
        if (isColdStorage) {
            finish()
        }
    }

    @Subscribe
    fun syncStopped(sync: SyncStopped?) {
        continueIfReadyOrNonUtxos()
    }

    private fun continueIfReadyOrNonUtxos() {
        if (isFinishing) {
            return
        }
        if (account.isSyncing() && (account.coinType.isUtxosBased || isColdStorage)) {
            // wait till its finished syncing
            // no need wait for non utxo's based accounts
            return
        }
        goToSendActivity()
    }

    private fun goToSendActivity() {
        if (isColdStorage) {
            ColdStorageSummaryActivity.callMe(this, account.id)
        } else {
            val intent = when {
                rawPr != null -> getIntent(this, account.id, rawPr!!, false)
                uri != null -> getIntent(this, account.id, uri!!, false)
                else -> SendCoinsActivity.getIntent(this, account.id, false)
            }.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            startActivity(intent)
        }
        finish()
    }

    companion object {
        @JvmStatic
        fun callMe(currentActivity: Activity, account: UUID, isColdStorage: Boolean) {
            //we don't know anything specific yet
            val intent = prepareSendingIntent(currentActivity, account, null as AssetUri?, isColdStorage)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            currentActivity.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(currentActivity: Activity, account: UUID, isColdStorage: Boolean): Intent {
            return prepareSendingIntent(currentActivity, account, null as AssetUri?, isColdStorage)
        }

        @JvmStatic
        fun callMeWithResult(currentActivity: Activity, account: UUID, uri: AssetUri?,
                             isColdStorage: Boolean, request: Int) {
            val intent = prepareSendingIntent(currentActivity, account, uri, isColdStorage)
            currentActivity.startActivityForResult(intent, request)
        }

        @JvmStatic
        fun callMeWithResult(currentActivity: Activity, account: UUID, isColdStorage: Boolean, request: Int) {
            val intent = prepareSendingIntent(currentActivity, account, null as AssetUri?, isColdStorage)
            currentActivity.startActivityForResult(intent, request)
        }

        @JvmStatic
        fun callMe(currentActivity: Activity, account: UUID, uri: AssetUri?, isColdStorage: Boolean) {
            val intent = prepareSendingIntent(currentActivity, account, uri, isColdStorage)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            currentActivity.startActivity(intent)
        }

        @JvmStatic
        fun callMe(currentActivity: Activity, account: UUID, rawPaymentRequest: ByteArray, isColdStorage: Boolean) {
            val intent = prepareSendingIntent(currentActivity, account, rawPaymentRequest, isColdStorage)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            currentActivity.startActivity(intent)
        }

        private fun prepareSendingIntent(currentActivity: Activity, account: UUID, uri: AssetUri?,
                                         isColdStorage: Boolean): Intent {
            return Intent(currentActivity, SendInitializationActivity::class.java)
                .putExtra("account", account)
                .putExtra("uri", uri)
                .putExtra("isColdStorage", isColdStorage)
        }

        @JvmStatic
        fun callMeWithResult(currentActivity: Activity, account: UUID, rawPaymentRequest: ByteArray,
                             isColdStorage: Boolean, request: Int) {
            val intent = prepareSendingIntent(currentActivity, account, rawPaymentRequest, isColdStorage)
            currentActivity.startActivityForResult(intent, request)
        }

        private fun prepareSendingIntent(currentActivity: Activity, account: UUID,
                                         rawPaymentRequest: ByteArray, isColdStorage: Boolean) =
            Intent(currentActivity, SendInitializationActivity::class.java)
                .putExtra("account", account)
                .putExtra("rawPr", rawPaymentRequest)
                .putExtra("isColdStorage", isColdStorage)
    }
}