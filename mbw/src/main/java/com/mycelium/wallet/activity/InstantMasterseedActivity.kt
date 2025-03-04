package com.mycelium.wallet.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import com.mrd.bitlib.crypto.Bip39.MasterSeed
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.HdAccountSelectorActivity.HdAccountWrapper
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.getIntent
import com.mycelium.wallet.activity.util.MasterseedScanManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.squareup.otto.Subscribe

class InstantMasterseedActivity : HdAccountSelectorActivity<MasterseedScanManager>() {
    private var masterSeed: MasterSeed? = null
    private var words: Array<String>? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        masterSeed = intent.getSerializableExtra(MASTERSEED) as MasterSeed?
        if (masterSeed == null) {
            words = intent.getStringArrayExtra(WORDS)
            password = intent.getStringExtra(PASSWORD)
        }
        super.onCreate(savedInstanceState)
    }

    override fun setView() {
        setContentView(R.layout.activity_instant_masterseed)
    }

    override fun initMasterseedManager(): MasterseedScanManager {
        val mbwManager = MbwManager.getInstance(this)
        val walletManager = mbwManager.getWalletManager(true)
        if (walletManager.accountScanManager == null) {
            walletManager.accountScanManager = if (masterSeed != null) {
                MasterseedScanManager(
                    this,
                    mbwManager.network,
                    masterSeed!!,
                    MbwManager.getEventBus(),
                    coinType!!
                )
            } else {
                // only provide the words - the manager will ask for a passphrase
                MasterseedScanManager(
                    this,
                    mbwManager.network,
                    words!!,
                    password,
                    MbwManager.getEventBus(),
                    coinType!!
                )
            }
        }
        return walletManager.accountScanManager as MasterseedScanManager
    }

    override fun accountClickListener(): OnItemClickListener? =
        object : OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                (adapterView.getItemAtPosition(i) as? HdAccountWrapper)?.run {
                    val intent = getIntent(
                        this@InstantMasterseedActivity,
                        id!!,
                        true
                    )
                    this@InstantMasterseedActivity.startActivityForResult(intent, REQUEST_SEND)
                }
            }
        }

    // Otto.EventBus does not traverse class hierarchy to find subscribers
    @Subscribe
    override fun onScanError(event: OnScanError) {
        super.onScanError(event)
    }

    @Subscribe
    override fun onStatusChanged(event: OnStatusChanged?) {
        super.onStatusChanged(event)
    }

    @Subscribe
    override fun onAccountFound(event: OnAccountFound) {
        super.onAccountFound(event)
    }

    @Subscribe
    override fun onPassphraseRequest(event: OnPassphraseRequest?) {
        super.onPassphraseRequest(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        MbwManager.getInstance(this).forgetColdStorageWalletManager()
    }

    companion object {
        const val PASSWORD: String = "password"
        const val WORDS: String = "words"
        const val MASTERSEED: String = "masterseed"


        // if password is null, the scan manager will ask the user for a password later on
        fun callMe(currentActivity: Activity, masterSeedWords: Array<String>, password: String?) {
            currentActivity.selectCoin { coinType ->
                currentActivity.startActivity(
                    Intent(currentActivity, InstantMasterseedActivity::class.java)
                        .putExtra(WORDS, masterSeedWords)
                        .putExtra(PASSWORD, password)
                        .putExtra(COIN_TYPE, coinType)
                )
            }
        }

        fun callMe(currentActivity: Activity, requestCode: Int, masterSeed: MasterSeed?) {
            currentActivity.selectCoin { coinType ->
                val intent = Intent(currentActivity, InstantMasterseedActivity::class.java)
                    .putExtra(MASTERSEED, masterSeed)
                    .putExtra(COIN_TYPE, coinType)
                currentActivity.startActivityForResult(intent, requestCode)
            }
        }
    }
}
