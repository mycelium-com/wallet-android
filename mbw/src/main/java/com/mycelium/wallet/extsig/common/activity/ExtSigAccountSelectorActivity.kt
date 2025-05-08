package com.mycelium.wallet.extsig.common.activity

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.google.common.base.Strings
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.PinDialog
import com.mycelium.wallet.R
import com.mycelium.wallet.TrezorPinDialog
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.HdAccountSelectorActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.AbstractAccountScanManager
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.activity.util.Pin
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnPinMatrixRequest
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.squareup.otto.Subscribe

abstract class ExtSigAccountSelectorActivity<AccountScanManager : AbstractAccountScanManager> :
    HdAccountSelectorActivity<AccountScanManager>(),
    MasterseedPasswordSetter {

    @SuppressLint("DefaultLocale") // It's only for display.
    override fun updateUi() {
        if (masterseedScanManager!!.currentState == AccountScanManager.Status.readyToScan) {
            findViewById<View?>(R.id.tvWaitForExtSig).visibility = View.GONE
            findViewById<View?>(R.id.ivConnectExtSig).visibility = View.GONE
            txtStatus?.text = getString(R.string.ext_sig_scanning_status)
        } else {
            super.updateUi()
        }

        if (masterseedScanManager!!.currentAccountState == AccountScanManager.AccountStatus.scanning) {
            findViewById<View?>(R.id.llStatus).visibility = View.VISIBLE
            if (accounts.isNotEmpty()) {
                super.updateUi()
            } else {
                txtStatus?.text = getString(R.string.ext_sig_scanning_status)
            }
        } else if (masterseedScanManager!!.currentAccountState == AccountScanManager.AccountStatus.done) {
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

            // Show the label and version of the connected Trezor
            findViewById<View?>(R.id.llExtSigInfo).visibility = View.VISIBLE
            val extSigDevice = masterseedScanManager as ExternalSignatureDeviceManager

            if (extSigDevice.features != null && !Strings.isNullOrEmpty(extSigDevice.features!!.getLabel())) {
                (findViewById<View?>(R.id.tvExtSigName) as TextView).text = extSigDevice.features!!.getLabel()
            } else {
                (findViewById<View?>(R.id.tvExtSigName) as TextView).text = getString(R.string.ext_sig_unnamed)
            }

            var version: String?
            val tvTrezorSerial = findViewById<TextView>(R.id.tvExtSigSerial)
            if (extSigDevice.isMostRecentVersion) {
                version = if (extSigDevice.features != null) {
                    String.format(
                        "%s, V%d.%d.%d",
                        extSigDevice.features!!.getDeviceId(),
                        extSigDevice.features!!.majorVersion,
                        extSigDevice.features!!.minorVersion,
                        extSigDevice.features!!.patchVersion
                    )
                } else {
                    ""
                }
            } else {
                version = getString(R.string.ext_sig_new_firmware)
                tvTrezorSerial.setTextColor(getResources().getColor(R.color.semidarkgreen))
                tvTrezorSerial.setOnClickListener {
                    if (extSigDevice.hasExternalConfigurationTool()) {
                        extSigDevice.openExternalConfigurationTool(
                            this@ExtSigAccountSelectorActivity,
                            getString(R.string.external_app_needed),
                            null
                        )
                    } else {
                        Utils.showSimpleMessageDialog(
                            this@ExtSigAccountSelectorActivity,
                            getFirmwareUpdateDescription()
                        )
                    }
                }
            }
            tvTrezorSerial.text = version
        }

        accountsAdapter!!.notifyDataSetChanged()
    }

    protected abstract fun getFirmwareUpdateDescription(): String


    override fun setPassphrase(passphrase: String?) {
        masterseedScanManager!!.setPassphrase(passphrase)

        if (passphrase == null) {
            // user choose cancel -> leave this activity
            finish()
        }
    }


    @Subscribe
    open fun onPinMatrixRequest(event: OnPinMatrixRequest?) {
        val pin = TrezorPinDialog(this@ExtSigAccountSelectorActivity, true)
        pin.setOnPinValid(object : PinDialog.OnPinEntered {
            override fun pinEntered(dialog: PinDialog, pin: Pin) {
                (masterseedScanManager as ExternalSignatureDeviceManager).enterPin(pin.getPin())
                dialog.dismiss()
            }
        })
        pin.show()

        // update the UI, as the state might have changed
        updateUi()
    }


    @Subscribe
    override fun onScanError(event: OnScanError) {
        val extSigDevice = masterseedScanManager as ExternalSignatureDeviceManager?
        // see if we know how to init that device
        if (event.errorType == OnScanError.ErrorType.NOT_INITIALIZED &&
            extSigDevice?.hasExternalConfigurationTool() == true
        ) {
            extSigDevice.openExternalConfigurationTool(
                this,
                getString(R.string.ext_sig_device_not_initialized)
            ) {
                // close this activity and let the user restart it after the tool ran
                this@ExtSigAccountSelectorActivity.finish()
            }
        } else {
            super.onScanError(event)
        }
    }

    @Subscribe
    override fun onStatusChanged(event: OnStatusChanged?) {
        super.onStatusChanged(event)
    }

    @Subscribe
    override fun onAccountFound(event: OnAccountFound) {
        super.onAccountFound(event)
        val walletManager = MbwManager.getInstance(this).getWalletManager(false)
        val id = event.account.accountId
        if (id != null && walletManager.hasAccount(id)) {
            val upgraded = masterseedScanManager!!.upgradeAccount(
                event.account.accountsRoots,
                walletManager, id
            )
            if (upgraded) {
                // If it's migrated it's 100% that it's HD
                val accountIndex =
                    (walletManager.getAccount(id) as HDAccount).accountIndex
                Toaster(this).toast(getString(R.string.account_upgraded, accountIndex + 1), false)
            }
        }
    }

    @Subscribe
    override fun onPassphraseRequest(event: OnPassphraseRequest?) {
        super.onPassphraseRequest(event)
    }
}


