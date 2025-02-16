package com.mycelium.wallet.extsig.ledger.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.TextView
import com.mycelium.wallet.LedgerPinDialog
import com.mycelium.wallet.PinDialog
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.HdAccountSelectorActivity.HdAccountWrapper
import com.mycelium.wallet.activity.util.Pin
import com.mycelium.wallet.extsig.ledger.LedgerManager
import com.mycelium.wallet.extsig.ledger.LedgerManager.OnPinRequest
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.squareup.otto.Subscribe
import nordpol.android.TagDispatcher

class LedgerAccountImportActivity : LedgerAccountSelectorActivity() {

    private var dispatcher: TagDispatcher? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatcher = TagDispatcher.get(this, masterseedScanManager)
    }

    override fun onResume() {
        super.onResume()
        dispatcher!!.enableExclusiveNfc()
    }

    override fun onPause() {
        super.onPause()
        dispatcher!!.disableExclusiveNfc()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatcher!!.interceptIntent(intent)
    }

    override fun accountClickListener(): OnItemClickListener? =
        object : OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                val item = adapterView.getItemAtPosition(i) as HdAccountWrapper
                createAccountAndFinish(
                    item.publicKeyNodes,
                    item.accountHdKeysPaths.first().lastIndex
                )

                ProgressDialog(this@LedgerAccountImportActivity).apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    setTitle(getString(R.string.hardware_account_create))
                    setMessage(getString(R.string.please_wait_hardware))
                }.show()
            }
        }

    override fun updateUi() {
        super.updateUi()
        if (masterseedScanManager!!.currentAccountState == AccountScanManager.AccountStatus.done) {
            findViewById<View?>(R.id.btNextAccount).setEnabled(true)
        } else {
            findViewById<View?>(R.id.btNextAccount).setEnabled(false)
        }
    }

    override fun setView() {
        setContentView(R.layout.activity_instant_ledger)
        (findViewById<View?>(R.id.tvCaption) as TextView).text =
            getString(R.string.ledger_import_account_caption)
        (findViewById<View?>(R.id.tvSelectAccount) as TextView).text =
            getString(R.string.ledger_select_account_to_import)
        (findViewById<View?>(R.id.btNextAccount) as TextView).visibility = View.VISIBLE

        findViewById<View?>(R.id.btNextAccount).setOnClickListener {
            Utils.showSimpleMessageDialog(
                this@LedgerAccountImportActivity,
                getString(R.string.ledger_next_unused_account_info)
            ) {
                // The TEE calls are asynchronous, and simulate a blocking call
                // To avoid locking up the UI thread, a new one is started
                val nextAccounts = masterseedScanManager!!.getNextUnusedAccounts()
                if (nextAccounts.isNotEmpty()) {
                    createAccountAndFinish(nextAccounts, nextAccounts.first().index)
                }
            }
        }
    }


    @Subscribe
    override fun onPinRequest(event: OnPinRequest?) {
        LedgerPinDialog(this, true).apply {
            setTitle(R.string.ledger_enter_pin)
            setOnPinValid(object : PinDialog.OnPinEntered {
                override fun pinEntered(dialog: PinDialog, pin: Pin) {
                    (masterseedScanManager as LedgerManager).enterPin(pin.pin)
                    dialog.dismiss()
                }
            })
        }.show()
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

    companion object {
        @JvmStatic
        fun callMe(currentActivity: Activity, requestCode: Int) {
            val intent = Intent(currentActivity, LedgerAccountImportActivity::class.java)
            currentActivity.startActivityForResult(intent, requestCode)
        }
    }
}
