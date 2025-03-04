package com.mycelium.wallet.activity.send

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.*
import com.mycelium.wallet.activity.StringHandlerActivity.ParseAbility
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.callMeWithResult
import com.mycelium.wallet.activity.util.*
import com.mycelium.wallet.content.HandleConfigFactory.spendFromColdStorage
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.databinding.InstantWalletActivityBinding
import com.mycelium.wallet.extsig.keepkey.activity.InstantKeepKeyActivity
import com.mycelium.wallet.extsig.trezor.activity.InstantTrezorActivity
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig
import java.util.*

class InstantWalletActivity : AppCompatActivity() {

    private lateinit var binding: InstantWalletActivityBinding

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(InstantWalletActivityBinding.inflate(layoutInflater).apply { binding = this }.root)
        binding.btClipboard.setOnClickListener { handleString(Utils.getClipboardString(this@InstantWalletActivity)) }
        binding.btMasterseed.setOnClickListener { EnterWordListActivity.callMe(this@InstantWalletActivity, IMPORT_WORDLIST, true) }
        binding.btScan.setOnClickListener { ScanActivity.callMe(this@InstantWalletActivity, REQUEST_SCAN, spendFromColdStorage()) }
        binding.btTrezor.setOnClickListener { InstantTrezorActivity.callMe(this@InstantWalletActivity, REQUEST_TREZOR) }
        binding.btKeepKey.setOnClickListener { InstantKeepKeyActivity.callMe(this@InstantWalletActivity, REQUEST_KEEPKEY) }
        supportActionBar?.hide()
    }

    private fun handleString(str: String) {
        startActivityForResult(StringHandlerActivity.getIntent(this, spendFromColdStorage(), str), REQUEST_SCAN)
    }

    override fun onResume() {
        super.onResume()
        val canHandle = StringHandlerActivity.canHandle(
                spendFromColdStorage(),
                Utils.getClipboardString(this),
                MbwManager.getInstance(this).network)
        binding.btClipboard.isEnabled = canHandle != ParseAbility.NO
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_SCAN) {
            if (resultCode != RESULT_OK) {
                ScanActivity.toastScanError(resultCode, intent, this)
            } else {
                val mbwManager = MbwManager.getInstance(this)
                val type = intent!!.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY) as ResultType?
                when (type) {
                    ResultType.PRIVATE_KEY -> {
                        val key = intent.getPrivateKey()
                        // ask user what WIF privkey he/she scanned as there are options
                        var selectedItem = 0
                        val choices = arrayOf("BTC", "FIO")
                        AlertDialog.Builder(this)
                                .setTitle("Choose blockchain")
                                .setSingleChoiceItems(choices, 0) { _: DialogInterface?, i: Int -> selectedItem = i }
                                .setPositiveButton(this.getString(R.string.ok)) { _: DialogInterface?, i: Int ->
                                    if (selectedItem == 0) {
                                        sendWithAccount(mbwManager.createOnTheFlyAccount(key, Utils.getBtcCoinType()))
                                    } else {
                                        sendWithAccount(mbwManager.createOnTheFlyAccount(key, Utils.getFIOCoinType()))
                                    }
                                }
                                .setNegativeButton(this.getString(R.string.cancel), null)
                                .show()
                    }
                    ResultType.ADDRESS -> {
                        val address = intent.getAddress()
                        sendWithAccount(mbwManager.createOnTheFlyAccount(address))
                    }
                    ResultType.ASSET_URI -> {
                        val uri = intent.getAssetUri()
                        sendWithAccount(mbwManager.createOnTheFlyAccount(uri.address))
                    }
                    ResultType.HD_NODE -> {
                        val hdKeyNode = intent.getHdKeyNode()
                        val tempWalletManager = mbwManager.getWalletManager(true)
                        val account = tempWalletManager.createAccounts(UnrelatedHDAccountConfig(listOf(hdKeyNode)))[0]
                        tempWalletManager.startSynchronization(account)
                        sendWithAccount(account)
                    }
                    ResultType.SHARE -> {
                        val share = intent.getShare()
                        BipSsImportActivity.callMe(this, share, StringHandlerActivity.IMPORT_SSS_CONTENT_CODE)
                    }
                    ResultType.WORD_LIST -> {
                        val wordList = intent.getWordList()
                        InstantMasterseedActivity.callMe(this, wordList, null)
                    }

                    else -> {}
                }
            }
        } else if (requestCode == StringHandlerActivity.SEND_INITIALIZATION_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toaster(this).toast(R.string.cancelled, false)
            }
            MbwManager.getInstance(this).forgetColdStorageWalletManager()
            // else {
            // We don't call finish() here, so that this activity stays on the back stack.
            // So the user can click back and scan the next cold storage.
            // }
        } else if (requestCode == REQUEST_TREZOR) {
            if (resultCode == RESULT_OK) {
                finish()
            }
        } else if (requestCode == REQUEST_KEEPKEY) {
            if (resultCode == RESULT_OK) {
                finish()
            }
        } else if (requestCode == IMPORT_WORDLIST) {
            if (resultCode == RESULT_OK) {
                val wordList = intent!!.getStringArrayListExtra(EnterWordListActivity.MASTERSEED)
                val password = intent.getStringExtra(EnterWordListActivity.PASSWORD)
                InstantMasterseedActivity.callMe(this, wordList!!.toTypedArray(), password)
            }
        } else if (requestCode == StringHandlerActivity.IMPORT_SSS_CONTENT_CODE) {
            if (resultCode == RESULT_OK) {
                finish()
            }
        } else {
            throw IllegalStateException("unknown return codes after scanning... $requestCode $resultCode")
        }
    }

    private fun sendWithAccount(account: UUID) {
        //we don't know yet where and what to send
        callMeWithResult(this, account, true,
                StringHandlerActivity.SEND_INITIALIZATION_CODE)
    }

    override fun finish() {
        // drop and create a new TempWalletManager so that no sensitive data remains in memory
        MbwManager.getInstance(this).forgetColdStorageWalletManager()
        super.finish()
    }

    companion object {
        const val REQUEST_SCAN = 0
        private const val REQUEST_TREZOR = 1
        private const val IMPORT_WORDLIST = 2
        private const val REQUEST_KEEPKEY = 3

        fun callMe(currentActivity: Activity) {
            currentActivity.startActivity(Intent(currentActivity, InstantWalletActivity::class.java))
        }
    }
}