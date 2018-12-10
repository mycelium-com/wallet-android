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

import android.app.Activity
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.NORMAL
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import android.view.View
import com.google.common.base.Joiner
import com.mrd.bitlib.util.CoinUtil
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.MasterseedPasswordDialog
import com.mycelium.wallet.activity.send.SignTransactionActivity
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnStatusUpdate.CurrentStatus.*
import com.mycelium.wallet.extsig.common.showChange
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.sign_ext_sig_transaction_activity.*
import java.lang.IllegalStateException
import android.text.method.LinkMovementMethod
import android.widget.TextView

abstract class ExtSigSignTransactionActivity : SignTransactionActivity(), MasterseedPasswordSetter {
    protected abstract val extSigManager: ExternalSignatureDeviceManager

    private var showTx = false
    private var feeString = ""
    private var totalString = ""
    private var amountString = ""   // Sent to destination address
    private var changeString = ""
    private var amountSendingString = ""   // Sent from our wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status.movementMethod = LinkMovementMethod.getInstance()
        summaryHeader.movementMethod = LinkMovementMethod.getInstance()
        summaryTransactionValues.setOnClickListener {
            val message = Html.fromHtml(getString(R.string.ext_sig_you_sending_message, amountString, feeString,
                    amountSendingString, changeString, totalString, extSigManager.modelName))
            AlertDialog.Builder(this@ExtSigSignTransactionActivity)
                    .setTitle(R.string.ext_sig_you_sending_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.accept) { _, _ ->  }
                    .create()
                    .show()
        }
    }

    override fun onStart() {
        super.onStart()
        MbwManager.getEventBus().register(this)
        updateUi()
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }


    override fun setPassphrase(passphrase: String?) {
        extSigManager.setPassphrase(passphrase)

        if (passphrase == null) {
            // user choose cancel -> leave this activity
            finish()
        } else {
            // close the dialog fragment
            val fragPassphrase = fragmentManager.findFragmentByTag(PASSPHRASE_FRAGMENT_TAG)
            if (fragPassphrase != null) {
                fragmentManager.beginTransaction().remove(fragPassphrase).commit()
            }
        }
    }

    private fun updateUi() {
        if (extSigManager.currentState != AccountScanManager.Status.unableToScan) {
            ivConnectExtSig.visibility = View.GONE
            tvPluginDevice.visibility = View.GONE
        } else {
            ivConnectExtSig.visibility = View.VISIBLE
            tvPluginDevice.visibility = View.VISIBLE
        }

        if (showTx) {
            ivConnectExtSig.visibility = View.GONE
            llShowTx.visibility = View.VISIBLE

            val toAddresses = ArrayList<String>(1)
            var changeAddress = ""

            var amountSending: Long = 0
            var changeValue: Long = 0

            for (o in _unsigned.outputs) {
                val toAddress = o.script.getAddress(_mbwManager.network)!!
                val hdAccount = _account as HDAccount

                if (!(hdAccount.isOwnInternalAddress(toAddress))) {
                    // this output goes to a foreign address (addressId[0]==1 means its internal change)
                    amountSending += o.value
                    toAddresses.add(toAddress.toDoubleLineString())
                } else {
                    // We need to track this to display additional info. Nor trezor, nor KeepKey supports mixed mode.
                    changeAddress = toAddress.toDoubleLineString()
                    changeValue = o.value
                }
            }

            val toAddress = Joiner.on(",\n").join(toAddresses)
            amountString = "${CoinUtil.valueString(amountSending, false)} BTC"
            val fee = _unsigned.calculateFee()
            val showChange = showChange(_unsigned, _mbwManager.network, _account as HDAccount)
            val totalAmount = amountSending + fee + if (showChange) { changeValue } else { 0 }
            totalString = "${CoinUtil.valueString(totalAmount, false)} BTC"
            changeString = "${CoinUtil.valueString(changeValue, false)} BTC"
            feeString = "${CoinUtil.valueString(fee, false)} BTC"

            if ((changeAddress != "") && showChange) {
                showChangeProperties(changeAddress, amountSending, fee)
            }

            tvAmount.text = amountString
            tvToAddress.text = toAddress
            tvFee.text = feeString
            tvTotal.text = totalString
        }
    }

    private fun showChangeProperties(changeAddress: String, amountSending: Long, fee: Long) {
        // Address for change. Required as nor KeepKey, nor Trezor supports
        val model = extSigManager.features?.model
        when {
            listOf("K1-14AM", "1").contains(model) -> {
                val accountIndex = (_account as HDAccount).accountIndex
                changeToAddress.text = getString(R.string.transferToAccountN, accountIndex)
            }
            model == "T" -> changeToAddress.text = changeAddress
            else -> throw IllegalStateException("Device unsupported")
        }
        changeToAddress.visibility = View.VISIBLE
        sendingValue.visibility = View.VISIBLE
        sendingValueLabel.visibility = View.VISIBLE
        amountSendingString = "${CoinUtil.valueString(amountSending + fee, false)} BTC"
        sendingValue.text = amountSendingString
        changeToAddressLabel.visibility = View.VISIBLE
        summaryTransactionValues.visibility = View.VISIBLE
    }

    @Subscribe
    open fun onPassphraseRequest(event: AccountScanManager.OnPassphraseRequest) {
        val pwd = MasterseedPasswordDialog()
        pwd.show(fragmentManager, PASSPHRASE_FRAGMENT_TAG)
    }

    @Subscribe
    open fun onScanError(event: AccountScanManager.OnScanError) {
        Utils.showSimpleMessageDialog(this, event.errorMessage) {
            this.setResult(Activity.RESULT_CANCELED)
            // close this activity and let the user try again
            this.finish()
        }

        // kill the signing task
        cancelSigningTask()
    }

    @Subscribe
    open fun onPinMatrixRequest(event: ExternalSignatureDeviceManager.OnPinMatrixRequest) {
        val pin = TrezorPinDialog(this, true)
        pin.setOnPinValid { dialog, pin ->
            extSigManager.enterPin(pin.pin)
            dialog.dismiss()
        }
        pin.show()

        // update the UI, as the state might have changed
        updateUi()
    }

    @Subscribe
    open fun onStatusUpdate(event: ExternalSignatureDeviceManager.OnStatusUpdate) {
        val output = _unsigned.outputs[event.outputIndex]
        val address = output.script.getAddress(_mbwManager.network)

        val statusText = when (event.status) {
            SHOW_CHANGE_ADDRESS -> {
                setChangeTypeface(BOLD)
                getString(R.string.check_change_address)
            }
            CONFIRM_OUTPUT -> {
                setChangeTypeface(NORMAL)
                getString(R.string.confirm_output_on_device,"${CoinUtil.valueString(output.value, false)} BTC",
                        address.toDoubleLineString().replace("\n", "<br>"), extSigManager.modelName)
            }
            CONFIRM_CHANGE -> {
                setChangeTypeface(BOLD)
                getString(R.string.confirm_change_on_device,"${CoinUtil.valueString(output.value, false)} BTC",
                        address.toDoubleLineString().replace("\n", "<br>"), extSigManager.modelName)
            }
            SIGN_TRANSACTION -> {
                setChangeTypeface(NORMAL)
                getString(R.string.extsig_confrim_on_hardware, extSigManager.modelName)
            }
            WARNING -> {
                val mixedModeWarning = Html.fromHtml(getString(R.string.mixed_mode_explanation, extSigManager.modelName))
                val alertDialog: AlertDialog = AlertDialog.Builder(this)
                        .setTitle(R.string.signing_transaction_instruction)
                        .setMessage(mixedModeWarning)
                        .setPositiveButton(R.string.button_continue) { _, _ -> }
                        .create()
                alertDialog.show()
                ((alertDialog.findViewById<TextView>(android.R.id.message)))?.movementMethod = LinkMovementMethod.getInstance()
                ""
            }
            else -> throw IllegalStateException("This status is not supported")
        }
        status.text = Html.fromHtml(statusText)
        pbProgress.visibility = View.GONE
        showTx = true
        updateUi()
    }

    private fun setChangeTypeface(typeface: Int) {
        changeToAddressLabel.setTypeface(null, typeface)
        changeToAddress.setTypeface(null, typeface)
    }

    @Subscribe
    open fun onButtonRequest(event: ExternalSignatureDeviceManager.OnButtonRequest) {
        showTx = true
        updateUi()
    }

    @Subscribe
    open fun onStatusChanged(event: AccountScanManager.OnStatusChanged) = updateUi()

    companion object {
        private const val PASSPHRASE_FRAGMENT_TAG = "pass"
    }
}