package com.mycelium.wallet.extsig.common.activity

import android.app.Activity
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.NORMAL
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.common.base.Joiner
import com.mycelium.view.Denomination
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.MasterseedPasswordDialog
import com.mycelium.wallet.activity.send.SignTransactionActivity
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.activity.util.Pin
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.databinding.SignExtSigTransactionActivityBinding
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnStatusUpdate.CurrentStatus.*
import com.mycelium.wallet.extsig.common.showChange
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.squareup.otto.Subscribe

abstract class ExtSigSignTransactionActivity : SignTransactionActivity(), MasterseedPasswordSetter {
    protected abstract val extSigManager: ExternalSignatureDeviceManager

    private var showTx = false
    private var feeString = ""
    private var totalString = ""
    private var amountString = ""   // Sent to destination address
    private var changeString = ""
    private var amountSendingString = ""   // Sent from our wallet
    lateinit var binding: SignExtSigTransactionActivityBinding
    private var passDialog: MasterseedPasswordDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.status.movementMethod = LinkMovementMethod.getInstance()
        binding.summaryHeader.movementMethod = LinkMovementMethod.getInstance()
        binding.summaryTransactionValues.setOnClickListener {
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
        }
    }

    private fun updateUi() {
        if (extSigManager.currentState != AccountScanManager.Status.unableToScan) {
            binding.ivConnectExtSig.visibility = View.GONE
            binding.tvPluginDevice.visibility = View.GONE
        } else {
            binding.ivConnectExtSig.visibility = View.VISIBLE
            binding.tvPluginDevice.visibility = View.VISIBLE
        }

        val unsigned = (_transaction as BtcTransaction).unsignedTx

        if (showTx) {
            binding.ivConnectExtSig.visibility = View.GONE
            binding.llShowTx.visibility = View.VISIBLE

            val toAddresses = ArrayList<String>(1)
            var changeAddress = ""

            var amountSending: Long = 0
            var changeValue: Long = 0

            val hdAccount = _account as HDAccount

            for (o in unsigned!!.outputs) {
                val toAddress = o.script.getAddress(_mbwManager.network)!!

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
            amountString = Utils.getBtcCoinType().value(amountSending).toString(Denomination.UNIT)
            val fee = unsigned.calculateFee()
            val showChange = showChange(unsigned, _mbwManager.network, _account as HDAccount)
            val totalAmount = amountSending + fee + if (showChange) { changeValue } else { 0 }
            totalString = Utils.getBtcCoinType().value(totalAmount).toString(Denomination.UNIT)
            changeString = Utils.getBtcCoinType().value(changeValue).toString(Denomination.UNIT)
            feeString = Utils.getBtcCoinType().value(fee).toString(Denomination.UNIT)

            if ((changeAddress != "") && showChange) {
                showChangeProperties(changeAddress, amountSending, fee)
            }

            binding.tvAmount.text = amountString
            binding.tvToAddress.text = toAddress
            binding.tvFee.text = feeString
            binding.tvTotal.text = totalString
        }
    }

    private fun showChangeProperties(changeAddress: String, amountSending: Long, fee: Long) {
        // Address for change. Required as nor KeepKey, nor Trezor supports
        val model = extSigManager.features?.model
        when {
            listOf("K1-14AM", "1").contains(model) -> {
                val accountIndex = (_account as HDAccount).accountIndex
                binding.changeToAddress.text = getString(R.string.transferToAccountN, accountIndex)
            }
            model == "T" -> binding.changeToAddress.text = changeAddress
            else -> throw IllegalStateException("Device unsupported")
        }
        binding.changeToAddress.visibility = View.VISIBLE
        binding.sendingValue.visibility = View.VISIBLE
        binding.sendingValueLabel.visibility = View.VISIBLE
        amountSendingString = Utils.getBtcCoinType().value(amountSending + fee).toString(Denomination.UNIT)
        binding.sendingValue.text = amountSendingString
        binding.changeToAddressLabel.visibility = View.VISIBLE
        binding.summaryTransactionValues.visibility = View.VISIBLE
    }

    @Subscribe
    open fun onPassphraseRequest(event: AccountScanManager.OnPassphraseRequest) {
        if(passDialog?.isAdded == true) {
            passDialog?.dismissAllowingStateLoss()
        }
        passDialog = MasterseedPasswordDialog()
        passDialog?.show(supportFragmentManager, PASSPHRASE_FRAGMENT_TAG)
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
        val pinDialog = TrezorPinDialog(this, true)
        pinDialog.setOnPinValid(object : PinDialog.OnPinEntered {
            override fun pinEntered(dialog: PinDialog, pin: Pin) {
                extSigManager.enterPin(pin.pin)
                dialog.dismiss()
            }
        })
        pinDialog.show()

        // update the UI, as the state might have changed
        updateUi()
    }

    @Subscribe
    open fun onStatusUpdate(event: ExternalSignatureDeviceManager.OnStatusUpdate) {
        val output = (_transaction as BtcTransaction).unsignedTx!!.outputs[event.outputIndex]
        val address = output.script.getAddress(_mbwManager.network)

        val statusText = when (event.status) {
            SHOW_CHANGE_ADDRESS -> {
                setChangeTypeface(BOLD)
                getString(R.string.check_change_address)
            }
            CONFIRM_OUTPUT -> {
                setChangeTypeface(NORMAL)
                getString(R.string.confirm_output_on_device,
                        Utils.getBtcCoinType().value(output.value).toString(Denomination.UNIT),
                        address.toDoubleLineString().replace("\n", "<br>"), extSigManager.modelName)
            }
            CONFIRM_CHANGE -> {
                setChangeTypeface(BOLD)
                getString(R.string.confirm_change_on_device,
                        Utils.getBtcCoinType().value(output.value).toString(Denomination.UNIT),
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
        binding.status.text = Html.fromHtml(statusText)
        binding.pbProgress.visibility = View.GONE
        showTx = true
        updateUi()
    }

    private fun setChangeTypeface(typeface: Int) {
        binding.changeToAddressLabel.setTypeface(null, typeface)
        binding.changeToAddress.setTypeface(null, typeface)
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