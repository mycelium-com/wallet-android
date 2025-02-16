package com.mycelium.wallet.extsig.ledger.activity

import android.view.View
import com.mycelium.wallet.LedgerPinDialog
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.PinDialog
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.HdAccountSelectorActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.Pin
import com.mycelium.wallet.extsig.ledger.LedgerManager
import com.mycelium.wallet.extsig.ledger.LedgerManager.OnPinRequest
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.squareup.otto.Subscribe

abstract class LedgerAccountSelectorActivity : HdAccountSelectorActivity<LedgerManager>() {
    override fun initMasterseedManager(): LedgerManager =
        MbwManager.getInstance(this).ledgerManager

    override fun finish() {
        super.finish()
        masterseedScanManager?.stopBackgroundAccountScan()
    }

    override fun updateUi() {
        if ((masterseedScanManager?.currentState != AccountScanManager.Status.initializing) &&
            (masterseedScanManager?.currentState != AccountScanManager.Status.unableToScan)
        ) {
            findViewById<View?>(R.id.tvWaitForLedger).visibility = View.GONE
            findViewById<View?>(R.id.ivConnectLedger).setVisibility(View.GONE)
            txtStatus!!.text = getString(R.string.ledger_scanning_status)
        } else {
            super.updateUi()
        }

        if (masterseedScanManager?.currentAccountState == AccountScanManager.AccountStatus.scanning) {
            findViewById<View?>(R.id.llStatus).visibility = View.VISIBLE
            if (accounts.isNotEmpty()) {
                super.updateUi()
            } else {
                txtStatus!!.setText(getString(R.string.ledger_scanning_status))
            }
        } else if (masterseedScanManager?.currentAccountState == AccountScanManager.AccountStatus.done) {
            // DONE
            findViewById<View?>(R.id.llStatus).visibility = View.GONE
            findViewById<View?>(R.id.llSelectAccount).visibility = View.VISIBLE
            if (accounts.isEmpty()) {
                // no accounts found
                findViewById<View?>(R.id.tvNoAccounts).visibility = View.VISIBLE
                findViewById<View?>(R.id.lvAccounts).visibility = View.GONE
            } else {
                findViewById<View?>(R.id.tvNoAccounts).visibility = View.GONE
                findViewById<View?>(R.id.lvAccounts).visibility = View.VISIBLE
            }
        }

        accountsAdapter!!.notifyDataSetChanged()
    }

    @Subscribe
    open fun onPinRequest(event: OnPinRequest?) {
        val pin = LedgerPinDialog(this, true)
        pin.setTitle(R.string.ledger_enter_pin)
        pin.setOnPinValid(object : PinDialog.OnPinEntered {
            override fun pinEntered(dialog: PinDialog, pin: Pin) {
                (masterseedScanManager as LedgerManager).enterPin(pin.getPin())
                dialog.dismiss()
            }
        })
        pin.show()
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
        val walletManager = MbwManager.getInstance(this).getWalletManager(false)
        if (walletManager.hasAccount(event.account.accountId)) {
            val upgraded = masterseedScanManager?.upgradeAccount(
                event.account.accountsRoots,
                walletManager, event.account.accountId
            )
            if (upgraded == true) {
                // If it's migrated it's 100% that it's HD
                val accountIndex =
                    (walletManager.getAccount(event.account.accountId) as HDAccount).accountIndex
                Toaster(this).toast(getString(R.string.account_upgraded, accountIndex + 1), false)
            }
        }
    }

    @Subscribe
    override fun onPassphraseRequest(event: OnPassphraseRequest?) {
        super.onPassphraseRequest(event)
    }
}
