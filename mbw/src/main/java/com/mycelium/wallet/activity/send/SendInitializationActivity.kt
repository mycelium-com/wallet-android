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
package com.mycelium.wallet.activity.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.Window
import com.google.common.base.Preconditions
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.SendCoinsActivity.Companion.getIntent
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.wallet.WalletAccount
import com.squareup.otto.Subscribe
import java.util.*

class SendInitializationActivity : Activity() {
    private var _mbwManager: MbwManager? = null
    private var _account: WalletAccount<*>? = null
    private var _uri: AssetUri? = null
    private var _isColdStorage = false
    private var _synchronizingHandler: Handler? = null
    private var _slowNetworkHandler: Handler? = null
    private var _rawPr: ByteArray?
    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_initialization_activity)
        _mbwManager = MbwManager.getInstance(application)
        // Get intent parameters
        val accountId = Preconditions.checkNotNull(intent.getSerializableExtra("account") as UUID)
        _uri = intent.getSerializableExtra("uri") as AssetUri
        _rawPr = intent.getByteArrayExtra("rawPr")
        _isColdStorage = intent.getBooleanExtra("isColdStorage", false)
        val crashHint = TextUtils.join(
            ", ", intent.extras!!
                .keySet()
        ) + " (account id was " + accountId + ")"
        val account = _mbwManager!!.getWalletManager(_isColdStorage).getAccount(accountId)
        _account = Preconditions.checkNotNull(account, crashHint)
        if (!_isColdStorage) {
            continueIfReadyOrNonUtxos()
        }
    }

    override fun onResume() {
        if (isFinishing) {
            return
        }
        MbwManager.getEventBus().register(this)

        // Show delayed messages so the user does not grow impatient
        _synchronizingHandler = Handler()
        _synchronizingHandler!!.postDelayed(showSynchronizing, 2000)
        _slowNetworkHandler = Handler()
        _slowNetworkHandler!!.postDelayed(showSlowNetwork, 6000)

        // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
        if (_mbwManager!!.currencySwitcher.getExchangeRatePrice(_account!!.coinType) == null) {
            _mbwManager!!.exchangeRateManager.requestRefresh()
        }

        // If we are in cold storage spending mode we wish to synchronize the wallet
        if (_isColdStorage) {
            _mbwManager!!.getWalletManager(true).startSynchronization()
        } else {
            continueIfReadyOrNonUtxos()
        }
        super.onResume()
    }

    override fun onPause() {
        if (_synchronizingHandler != null) {
            _synchronizingHandler!!.removeCallbacks(showSynchronizing)
        }
        if (_slowNetworkHandler != null) {
            _slowNetworkHandler!!.removeCallbacks(showSlowNetwork)
        }
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    private val showSynchronizing =
        Runnable { findViewById<View>(R.id.tvSynchronizing).visibility = View.VISIBLE }
    private val showSlowNetwork = Runnable {
        findViewById<View>(R.id.tvSlowNetwork).visibility =
            View.VISIBLE
    }

    @Subscribe
    fun syncFailed(event: SyncFailed?) {
        Utils.toastConnectionError(this)
        // If we are in cold storage spending mode there is no point in continuing.
        // If we continued we would think that there were no funds on the private key
        if (_isColdStorage) {
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
        if (_account!!.isSyncing && (_account!!.coinType.isUtxosBased || _isColdStorage)) {
            // wait till its finished syncing
            // no need wait for non utxo's based accounts
            return
        }
        goToSendActivity()
    }

    private fun goToSendActivity() {
        if (_isColdStorage) {
            ColdStorageSummaryActivity.callMe(this, _account!!.id)
        } else {
            val intent: Intent
            intent = if (_rawPr != null) {
                getIntent(this, _account!!.id, _rawPr!!, false)
            } else if (_uri != null) {
                getIntent(this, _account!!.id, _uri!!, false)
            } else {
                SendCoinsActivity.getIntent(this, _account!!.id, false)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            this.startActivity(intent)
        }
        finish()
    }

    companion object {
        fun callMe(currentActivity: Activity, account: UUID, isColdStorage: Boolean) {
            //we dont know anything specific yet
            val intent =
                prepareSendingIntent(currentActivity, account, null as AssetUri?, isColdStorage)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            currentActivity.startActivity(intent)
        }

        fun getIntent(currentActivity: Activity, account: UUID, isColdStorage: Boolean): Intent {
            return prepareSendingIntent(currentActivity, account, null as AssetUri?, isColdStorage)
        }

        fun callMeWithResult(
            currentActivity: Activity,
            account: UUID,
            uri: AssetUri?,
            isColdStorage: Boolean,
            request: Int
        ) {
            val intent = prepareSendingIntent(currentActivity, account, uri, isColdStorage)
            currentActivity.startActivityForResult(intent, request)
        }

        fun callMeWithResult(
            currentActivity: Activity,
            account: UUID,
            isColdStorage: Boolean,
            request: Int
        ) {
            val intent =
                prepareSendingIntent(currentActivity, account, null as AssetUri?, isColdStorage)
            currentActivity.startActivityForResult(intent, request)
        }

        fun callMe(
            currentActivity: Activity,
            account: UUID,
            uri: AssetUri?,
            isColdStorage: Boolean
        ) {
            val intent = prepareSendingIntent(currentActivity, account, uri, isColdStorage)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            currentActivity.startActivity(intent)
        }

        fun callMe(
            currentActivity: Activity,
            account: UUID,
            rawPaymentRequest: ByteArray,
            isColdStorage: Boolean
        ) {
            val intent =
                prepareSendingIntent(currentActivity, account, rawPaymentRequest, isColdStorage)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            currentActivity.startActivity(intent)
        }

        private fun prepareSendingIntent(
            currentActivity: Activity,
            account: UUID,
            uri: AssetUri?,
            isColdStorage: Boolean
        ): Intent {
            return Intent(currentActivity, SendInitializationActivity::class.java)
                .putExtra("account", account)
                .putExtra("uri", uri)
                .putExtra("isColdStorage", isColdStorage)
        }

        fun callMeWithResult(
            currentActivity: Activity,
            account: UUID,
            rawPaymentRequest: ByteArray,
            isColdStorage: Boolean,
            request: Int
        ) {
            val intent =
                prepareSendingIntent(currentActivity, account, rawPaymentRequest, isColdStorage)
            currentActivity.startActivityForResult(intent, request)
        }

        private fun prepareSendingIntent(
            currentActivity: Activity,
            account: UUID,
            rawPaymentRequest: ByteArray,
            isColdStorage: Boolean
        ): Intent {
            return Intent(currentActivity, SendInitializationActivity::class.java)
                .putExtra("account", account)
                .putExtra("rawPr", rawPaymentRequest)
                .putExtra("isColdStorage", isColdStorage)
        }
    }
}