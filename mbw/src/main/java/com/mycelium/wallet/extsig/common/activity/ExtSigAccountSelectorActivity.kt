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

    override fun finish() {
        super.finish()
        masterseedScanManager!!.stopBackgroundAccountScan()
    }

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
        } else {
            // close the dialog fragment
            val fragPassphrase = supportFragmentManager.findFragmentByTag(PASSPHRASE_FRAGMENT_TAG)
            if (fragPassphrase != null) {
                supportFragmentManager.beginTransaction().remove(fragPassphrase).commit()
            }
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
        if (walletManager.hasAccount(event.account.accountId)) {
            val upgraded = masterseedScanManager!!.upgradeAccount(
                event.account.accountsRoots,
                walletManager, event.account.accountId
            )
            if (upgraded) {
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


