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
package com.mycelium.wallet.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.google.common.io.ByteStreams
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.HdKeyNode.KeyGenerationException
import com.mrd.bitlib.crypto.InMemoryPrivateKey.Companion.fromBase58String
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.Constants
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.PinDialog
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.AccountCreatorHelper.AccountCreationObserver
import com.mycelium.wallet.activity.AccountCreatorHelper.CreateAccountAsyncTask
import com.mycelium.wallet.activity.export.DecryptBip38PrivateKeyActivity
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.pop.PopActivity
import com.mycelium.wallet.activity.send.GetSpendingRecordActivity
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.callMe
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.callMeWithResult
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity
import com.mycelium.wallet.bitid.BitIDSignRequest
import com.mycelium.wallet.content.actions.HdNodeAction.Companion.isKeyNode
import com.mycelium.wallet.content.actions.PrivateKeyAction.Companion.getPrivateKey
import com.mycelium.wallet.databinding.StartupActivityBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountCreated
import com.mycelium.wallet.event.MigrationPercentChanged
import com.mycelium.wallet.event.MigrationStatusChanged
import com.mycelium.wallet.fio.FioRequestNotificator
import com.mycelium.wallet.pop.PopRequest
import com.mycelium.wapi.content.PrivateKeyUri
import com.mycelium.wapi.content.WithCallback
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig
import com.squareup.otto.Bus
import com.squareup.otto.Subscribe
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.TimeUnit

class StartupActivity : AppCompatActivity(), AccountCreationObserver {
    private var _mbwManager: MbwManager? = null
    private var _alertDialog: AlertDialog? = null
    private var _pinDialog: PinDialog? = null
    private var _progress: ProgressDialog? = null
    private var eventBus: Bus = MbwManager.getEventBus()

    lateinit var binding: StartupActivityBinding

    private lateinit var sharedPreferences: SharedPreferences
    private var lastStartupTime: Long = 0
    private var isFirstRun = false


    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        sharedPreferences =
            applicationContext.getSharedPreferences(Constants.SETTINGS_NAME, MODE_PRIVATE)
        isFirstRun = (PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        ).getInt("ckChangeLog_last_version_code", -1) == -1)
        lastStartupTime =
            sharedPreferences.getLong(LAST_STARTUP_TIME, TimeUnit.SECONDS.toMillis(10))
        _progress = ProgressDialog(this)
        setContentView(StartupActivityBinding.inflate(layoutInflater).apply { binding = this }.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
    }

    override fun onStart() {
        super.onStart()
        eventBus.register(this)
        Thread(delayedInitialization).start()
    }

    public override fun onStop() {
        _progress?.dismiss()
        _pinDialog?.dismiss()
        eventBus.unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        if (_alertDialog?.isShowing == true) {
            _alertDialog?.dismiss()
        }
        super.onDestroy()
    }

    @Subscribe
    fun onMigrationProgressChanged(migrationPercent: MigrationPercentChanged) {
        binding.layoutProgress.progressBar.progress = migrationPercent.percent
    }

    @Subscribe
    fun onMigrationCommentChanged(migrationStatusChanged: MigrationStatusChanged) {
        binding.layoutProgress.status.text =
            migrationStatusChanged.newStatus.format(applicationContext)
    }

    private val delayedInitialization: Runnable = object : Runnable {
        override fun run() {
            if (lastStartupTime > TimeUnit.SECONDS.toMillis(5) && !isFirstRun) {
                Handler(mainLooper).post(object : Runnable {
                    override fun run() {
                        binding.layoutProgress.progressBar.isVisible = true
                        binding.layoutProgress.status.isVisible = true
                    }
                })
            }
            val startTime = System.currentTimeMillis()
            _mbwManager = MbwManager.getInstance(this@StartupActivity.getApplication())

            // in case this is a fresh startup, import backup or create new seed
            if (!_mbwManager!!.masterSeedManager.hasBip32MasterSeed()) {
                Handler(mainLooper).post(object : Runnable {
                    override fun run() {
                        initMasterSeed()
                    }
                })
                return
            }

            // in case the masterSeed was created but account does not exist yet (rotation problem)
            if (_mbwManager!!.getWalletManager(false).getActiveSpendingAccounts().isEmpty()) {
                CreateAccountAsyncTask(
                    this@StartupActivity,
                    this@StartupActivity,
                    mainAccounts
                ).execute()
                return
            }

            val needsToBeCreatedMasterSeedAccounts = _mbwManager!!.checkMainAccountsCreated()
            if (needsToBeCreatedMasterSeedAccounts.isNotEmpty()) {
                CreateAccountAsyncTask(
                    this@StartupActivity,
                    this@StartupActivity, needsToBeCreatedMasterSeedAccounts
                ).execute()
                return
            }

            // Calculate how much time we spent initializing, and do a delayed
            // finish so we display the splash a minimum amount of time
            val timeSpent = System.currentTimeMillis() - startTime
            var remainingTime = MINIMUM_SPLASH_TIME - timeSpent
            if (remainingTime < 0) {
                remainingTime = 0
            }

            Handler(mainLooper).postDelayed(delayedFinish, remainingTime)
            sharedPreferences.edit()
                .putLong(LAST_STARTUP_TIME, timeSpent)
                .apply()
        }
    }

    private fun initMasterSeed() {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.master_seed_configuration_title)
            .setMessage(getString(R.string.master_seed_configuration_description))
            .setNegativeButton(
                R.string.master_seed_restore_backup_button,
                object : DialogInterface.OnClickListener {
                    //import master seed from wordlist
                    override fun onClick(arg0: DialogInterface?, arg1: Int) {
                        EnterWordListActivity.callMe(this@StartupActivity, IMPORT_WORDLIST)
                    }
                })
            .setPositiveButton(
                R.string.master_seed_create_new_button,
                object : DialogInterface.OnClickListener {
                    //configure new random seed
                    override fun onClick(arg0: DialogInterface?, arg1: Int) {
                        startMasterSeedTask()
                    }
                })
            .show()
    }

    private fun startMasterSeedTask() {
        _progress!!.setCancelable(false)
        _progress!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        _progress!!.setMessage(getString(R.string.preparing_wallet_on_first_startup_info))
        _progress!!.show()
        ConfigureSeedAsyncTask(WeakReference<StartupActivity?>(this)).execute()
    }

    private class ConfigureSeedAsyncTask(private val startupActivity: WeakReference<StartupActivity?>) :
        AsyncTask<Void?, Int?, UUID?>() {

        override fun doInBackground(vararg params: Void?): UUID? {
            val activity = this.startupActivity.get()
            if (activity == null) {
                return null
            }
            val masterSeed = Bip39.createRandomMasterSeed(activity._mbwManager!!.getRandomSource())
            try {
                activity._mbwManager!!.masterSeedManager
                    .configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher())
                return activity._mbwManager!!.createAdditionalBip44AccountsUninterruptedly(
                    mainAccounts
                )[0]
            } catch (e: InvalidKeyCipher) {
                throw RuntimeException(e)
            }
        }

        override fun onPostExecute(accountId: UUID?) {
            val activity = this.startupActivity.get()
            if (accountId == null || activity == null) {
                return
            }
            activity._progress!!.dismiss()
            MbwManager.getEventBus().post(AccountCreated(accountId))
            MbwManager.getEventBus().post(AccountChanged(accountId))
            //finish initialization
            activity.delayedFinish.run()
        }
    }

    override fun onAccountCreated(accountId: UUID?) {
        MbwManager.getEventBus().post(AccountCreated(accountId))
        MbwManager.getEventBus().post(AccountChanged(accountId))
        delayedFinish.run()
    }

    private val delayedFinish: Runnable = object : Runnable {
        override fun run() {
            val manager = _mbwManager
            if (manager != null && manager.isUnlockPinRequired) {
                // set a click handler to the background, so that
                // if the PIN-Pad closes, you can reopen it by touching the background
                val delayRunnable = this
                window.decorView.findViewById<View?>(android.R.id.content)
                    ?.setOnClickListener { delayRunnable.run() }

                // set the pin dialog to not cancelable
                _pinDialog = manager.runPinProtectedFunction(
                    this@StartupActivity,
                    {
                        manager.setStartUpPinUnlocked(true)
                        start()
                    }, false
                )
            } else {
                start()
            }
        }

        private fun start() {
            // Check whether we should handle this intent in a special way if it
            // has a bitcoin URI in it
            if (handleIntent()) {
                return
            }

            // Check if we have lingering exported private keys, we want to warn
            // the user if that is the case
            val hasClipboardExportedPrivateKeys = hasPrivateKeyOnClipboard(_mbwManager!!.network)
            val hasClipboardExportedPublicKeys = hasPublicKeyOnClipboard(_mbwManager!!.network)

            if (hasClipboardExportedPublicKeys) {
                warnUserOnClipboardKeys(false)
            } else if (hasClipboardExportedPrivateKeys) {
                warnUserOnClipboardKeys(true)
            } else {
                normalStartup()
            }
        }

        private fun hasPrivateKeyOnClipboard(network: NetworkParameters): Boolean {
            // do we have a private key on the clipboard?
            try {
                val key = getPrivateKey(network, Utils.getClipboardString(this@StartupActivity))
                if (key != null) {
                    return true
                }
                HdKeyNode.parse(Utils.getClipboardString(this@StartupActivity), network)
                return true
            } catch (ex: KeyGenerationException) {
                return false
            }
        }

        private fun hasPublicKeyOnClipboard(network: NetworkParameters): Boolean {
            // do we have a public key on the clipboard?
            try {
                if (isKeyNode(network, Utils.getClipboardString(this@StartupActivity))) {
                    return true
                }
                HdKeyNode.parse(Utils.getClipboardString(this@StartupActivity), network)
                return true
            } catch (ex: KeyGenerationException) {
                return false
            }
        }
    }

    private fun warnUserOnClipboardKeys(isPrivate: Boolean) {
        _alertDialog = AlertDialog.Builder(this) // Set title
            .setTitle(
                if (isPrivate)
                    R.string.found_clipboard_private_key_title
                else
                    R.string.found_clipboard_public_key_title
            ) // Set dialog message
            .setMessage(
                if (isPrivate)
                    R.string.found_clipboard_private_keys_message
                else
                    R.string.found_clipboard_public_keys_message
            ) // Yes action
            .setCancelable(false)
            .setPositiveButton(R.string.yes, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    Utils.clearClipboardString(this@StartupActivity)
                    normalStartup()
                    dialog.dismiss()
                }
            }) // No action
            .setNegativeButton(R.string.no, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    normalStartup()
                    dialog.cancel()
                }
            })
            .create()
        _alertDialog!!.show()
    }

    private fun normalStartup() {
        // Normal startup, show the selected account in the BalanceActivity
        val intent = Intent(this@StartupActivity, ModernMain::class.java)
        if (getIntent().action == NewsUtils.MEDIA_FLOW_ACTION) {
            intent.action = NewsUtils.MEDIA_FLOW_ACTION
            getIntent().extras?.let { intent.putExtras(it) }
        } else if (getIntent().action == FioRequestNotificator.FIO_REQUEST_ACTION) {
            intent.action = FioRequestNotificator.FIO_REQUEST_ACTION
            if (getIntent().extras != null) {
                intent.putExtras(getIntent().extras!!)
            }
        } else if (getIntent().hasExtra("action")) {
            getIntent().extras?.let { intent.putExtras(it) }
        }
        startActivity(intent)
        finish()
    }

    private fun handleIntent(): Boolean {
        val intent = getIntent()
        val action = intent.getAction()

        if ("application/bitcoin-paymentrequest" == intent.type) {
            // called via paymentrequest-file
            handlePaymentRequest(intent.data!!)
            return true
        } else {
            val intentUri = intent.data
            val scheme = intentUri?.scheme

            if (intentUri != null && (Intent.ACTION_VIEW == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action)) {
                when (scheme) {
                    "bitcoin" -> handleUri(intentUri)
                    "bitid" -> handleBitIdUri(intentUri)
                    "btcpop" -> handlePopUri(intentUri)
                    "mycelium" -> handleMyceliumUri(intentUri)
                    else -> return false
                }
                return true
            }
        }
        return false
    }

    private fun handlePaymentRequest(paymentRequest: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(paymentRequest)
            val bytes = ByteStreams.toByteArray(inputStream)

            val mbwManager = MbwManager.getInstance(this@StartupActivity.application)

            var spendingAccounts =
                mbwManager.getWalletManager(false).getSpendingAccountsWithBalance()
            if (spendingAccounts.isEmpty()) {
                //if we dont have an account which can spend and has a balance, we fetch all accounts with priv keys
                spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccounts()
            }
            if (spendingAccounts.size == 1) {
                callMeWithResult(this, spendingAccounts[0].id, bytes, false, REQUEST_FROM_URI)
            } else {
                GetSpendingRecordActivity.callMeWithResult(this, bytes, REQUEST_FROM_URI)
            }
        } catch (e: FileNotFoundException) {
            Toaster(this).toast(R.string.file_not_found, false)
            finish()
        } catch (e: IOException) {
            Toaster(this).toast(R.string.payment_request_unable_to_read_payment_request, false)
            finish()
        }
    }

    private fun handleMyceliumUri(intentUri: Uri) {
        val host = intentUri.getHost()
        // If we dont understand the url, just call the balance screen
        val balanceIntent = Intent(this, ModernMain::class.java)
        startActivity(balanceIntent)
        // close the startup activity to not pollute the backstack
        finish()
    }

    private fun handleBitIdUri(intentUri: Uri) {
        //We have been launched by a bitid authentication request
        val bitid = BitIDSignRequest.parse(intentUri)
        if (!bitid.isPresent()) {
            //Invalid bitid URI
            Toaster(this).toast(R.string.invalid_bitid_uri, false)
            finish()
            return
        }
        val bitIdIntent = Intent(this, BitIDAuthenticationActivity::class.java)
            .putExtra("request", bitid.get())
        startActivity(bitIdIntent)

        finish()
    }

    private fun handlePopUri(intentUri: Uri) {
        // a proof of payment request
        val popRequest = PopRequest(intentUri.toString())
        val popIntent = Intent(this, PopActivity::class.java)
            .putExtra("popRequest", popRequest)
        startActivity(popIntent)
        finish()
    }

    private fun handleUri(intentUri: Uri) {
        // We have been launched by a Bitcoin URI
        val mbwManager = MbwManager.getInstance(application)
        val uri = mbwManager.contentResolver.resolveUri(intentUri.toString())
        if (uri == null) {
            // Invalid Bitcoin URI
            Toaster(this).toast(R.string.invalid_bitcoin_uri, false)
            finish()
            return
        }

        // the bitcoin uri might actually be encrypted private key, where the user wants to spend funds from
        if (uri is PrivateKeyUri) {
            val privateKeyUri = uri
            DecryptBip38PrivateKeyActivity.callMe(
                this,
                privateKeyUri.keyString,
                StringHandlerActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE
            )
        } else {
            if (uri.address == null && uri is WithCallback && Strings.isNullOrEmpty((uri as WithCallback).callbackURL)) {
                // Invalid Bitcoin URI
                Toaster(this).toast(R.string.invalid_bitcoin_uri, false)
                finish()
                return
            }

            var spendingAccounts =
                mbwManager.getWalletManager(false).getSpendingAccountsWithBalance()
            if (spendingAccounts.isEmpty()) {
                //if we dont have an account which can spend and has a balance, we fetch all accounts with priv keys
                spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccounts()
            }
            if (spendingAccounts.size == 1) {
                callMeWithResult(this, spendingAccounts[0].id, uri, false, REQUEST_FROM_URI)
            } else {
                GetSpendingRecordActivity.callMeWithResult(this, uri, REQUEST_FROM_URI)
            }
            //don't finish just yet we want to stay on the stack and observe that we emit a txid correctly.
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //todo make sure delayed init has finished (ev mit countdownlatch)
        when (requestCode) {
            IMPORT_WORDLIST -> {
                if (resultCode != RESULT_OK) {
                    //user cancelled the import, so just ask what he wants again
                    initMasterSeed()
                    return
                }
                //finish initialization
                delayedFinish.run()
                return
            }

            StringHandlerActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE -> {
                val content = data?.getStringExtra("base58Key")
                if (content != null) {
                    val optionalKey = fromBase58String(content, _mbwManager!!.network)
                    if (optionalKey != null) {
                        val onTheFlyAccount = MbwManager.getInstance(this).createOnTheFlyAccount(
                            optionalKey,
                            Utils.getBtcCoinType()
                        )
                        callMe(this, onTheFlyAccount, true)
                        finish()
                    }
                    return
                }
                // double-check result data, in case some downstream code messes up.
                if (resultCode == RESULT_OK) {
                    val extras = Preconditions.checkNotNull<Bundle>(data?.extras)
                    extras.keySet().forEach { key ->
                        // make sure we only share TRANSACTION_ID_INTENT_KEY with external caller
                        if (key != Constants.TRANSACTION_ID_INTENT_KEY) {
                            data?.removeExtra(key)
                        }
                    }
                    // return the tx hash to our external caller, if he cares...
                    setResult(RESULT_OK, data)
                } else {
                    setResult(RESULT_CANCELED)
                }
            }

            REQUEST_FROM_URI ->
                if (resultCode == RESULT_OK) {
                    val extras = Preconditions.checkNotNull<Bundle>(data?.extras)
                    extras.keySet().forEach { key ->
                        if (key != Constants.TRANSACTION_ID_INTENT_KEY) {
                            data?.removeExtra(key)
                        }
                    }
                    setResult(RESULT_OK, data)
                } else {
                    setResult(RESULT_CANCELED)
                }

            else -> setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        private const val MINIMUM_SPLASH_TIME = 100
        private const val REQUEST_FROM_URI = 2
        private const val IMPORT_WORDLIST = 0

        @JvmField
        val mainAccounts = listOf(
            AdditionalHDAccountConfig(),
            EthereumMasterseedConfig()
        )

        private const val LAST_STARTUP_TIME = "startupTme"
    }
}
