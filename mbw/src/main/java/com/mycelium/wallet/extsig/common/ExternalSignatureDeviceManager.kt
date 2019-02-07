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

package com.mycelium.wallet.extsig.common

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.common.base.Optional
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.Message
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AbstractAccountScanManager
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProvider
import com.satoshilabs.trezor.lib.ExtSigDeviceConnectionException
import com.satoshilabs.trezor.lib.ExternalSignatureDevice
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.SignTx
import com.satoshilabs.trezor.lib.protobuf.TrezorMessage.TxRequest
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.squareup.otto.Bus
import org.bitcoinj.core.ScriptException
import org.bitcoinj.script.ScriptBuilder
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

import com.mycelium.wallet.Constants.TAG
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnStatusUpdate.CurrentStatus.SHOW_CHANGE_ADDRESS
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnStatusUpdate.CurrentStatus.WARNING
import com.mycelium.wapi.wallet.AccountScanManager
import com.satoshilabs.trezor.lib.protobuf.TrezorType.RequestType.TXOUTPUT
import org.bitcoinj.core.NetworkParameters.ID_MAINNET
import org.bitcoinj.core.NetworkParameters.ID_TESTNET

abstract class ExternalSignatureDeviceManager(context: Context, network: NetworkParameters, eventBus: Bus) : AbstractAccountScanManager(context, network, eventBus), ExternalSignatureProvider {
    private val pinMatrixEntry = LinkedBlockingQueue<String>(1)
    private val signatureDevice by lazy { createDevice() }

    @Volatile
    var features: TrezorMessage.Features? = null
        private set

    val labelOrDefault: String
        get() = if (features != null && !features!!.label.isEmpty()) {
            features!!.label
        } else signatureDevice.defaultAccountName

    // we dont know...
    val isMostRecentVersion: Boolean
        get() {
            return if (features != null) {
                !signatureDevice.mostRecentFirmwareVersion.isNewerThan(features!!.majorVersion,
                        features!!.minorVersion, features!!.patchVersion)
            } else {
                true
            }
        }

    val modelName: String
        get() {
            when (features!!.model) {
                "K1-14AM" -> return "KeepKey"
                "1" -> return "Trezor One"
                "T" -> return "Trezor Model T"
            }
            throw IllegalStateException("Unsupported model")
        }

    class OnButtonRequest

    class OnStatusUpdate internal constructor(val outputIndex: Int, val status: CurrentStatus) {
        enum class CurrentStatus {
            SHOW_CHANGE_ADDRESS,
            CONFIRM_OUTPUT,
            CONFIRM_CHANGE,
            SIGN_TRANSACTION,
            WARNING
        }
    }

    class OnPinMatrixRequest

    protected abstract fun createDevice(): ExternalSignatureDevice

    fun hasExternalConfigurationTool() = signatureDevice.deviceConfiguratorAppName != null

    fun openExternalConfigurationTool(context: Context, msg: String, onClose: Runnable?) {
        // see if we know how to init that device
        val packageName = signatureDevice.deviceConfiguratorAppName
        if (packageName != null) {
            val downloadDialog = AlertDialog.Builder(context)
            downloadDialog.setTitle(R.string.ext_sig_configuration_dialog_title)
            downloadDialog.setMessage(msg)
            downloadDialog.setPositiveButton(R.string.button_ok) { _, _ ->
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                context.startActivity(intent)
                onClose?.run()
            }
            downloadDialog.setNegativeButton(R.string.button_cancel) { _, _ ->
                onClose?.run()
            }
            downloadDialog.show()
        }
    }

    override fun onBeforeScan(): Boolean {
        return initialize()
    }

    private fun initialize(): Boolean {
        // check if a trezor is attached and connect to it, otherwise loop and check periodically

        // wait until a device is connected
        while (!signatureDevice.isDevicePluggedIn) {
            try {
                setState(AccountScanManager.Status.unableToScan, currentAccountState)
                Thread.sleep(4000)
            } catch (e: InterruptedException) {
                break
            }
        }

        // set up the connection and afterwards send a Features-Request
        if (signatureDevice.connect(context)) {
            val req = TrezorMessage.Initialize.newBuilder().build()
            val resp = signatureDevice.send(req)
            when (resp) {
                is TrezorMessage.Features -> {
                    features = resp
                    return true
                }
                null -> Log.e("trezor", "Got null-response from trezor")
                else -> Log.e("trezor", "Got wrong response from trezor " + resp.javaClass.toString())
            }
        }
        return false
    }

    private fun getChangeAddress(unsigned: UnsignedTransaction, forAccount: HDAccountExternalSignature) =
            unsigned.outputs.firstOrNull { forAccount.isOwnInternalAddress(it.script.getAddress(network)) }
                    ?.script
                    ?.getAddress(network)

    // based on https://github.com/trezor/python-trezor/blob/a2a5b6a4601c6912166ef7f85f04fa1101c2afd4/trezorlib/client.py
    override fun getSignedTransaction(unsigned: UnsignedTransaction, forAccount: HDAccountExternalSignature): Transaction? {
        Log.d("trezor", "getting this transaction signed: $unsigned")
        if (!initialize()) {
            return null
        }
        if (features == null) {
            return null
        }

        setState(AccountScanManager.Status.readyToScan, currentAccountState)

        mainThreadHandler.post { eventBus.post(OnStatusUpdate(0, WARNING)) }

        // Trezor model T does not support showing "Transferring between accounts" message. We must show user change
        // address before signing
        val changeAddress = getChangeAddress(unsigned, forAccount)
        val changeWouldBeShown = showChange(unsigned, network, forAccount)
        if (features!!.model == "T" && changeAddress != null && changeWouldBeShown) {
            val getAddressBuilder = TrezorMessage.GetAddress.newBuilder()
            when (changeAddress.type) {
                AddressType.P2SH_P2WPKH -> getAddressBuilder.scriptType = TrezorType.InputScriptType.SPENDP2SHWITNESS
                AddressType.P2WPKH -> getAddressBuilder.scriptType = TrezorType.InputScriptType.SPENDWITNESS
                AddressType.P2PKH -> getAddressBuilder.scriptType = TrezorType.InputScriptType.SPENDADDRESS
                else -> {
                    postErrorMessage("Unknown script type")
                    return null
                }
            }
            getAddressBuilder.setCoinName(network.coinName).showDisplay = true
            val purpose = BipDerivationType.getDerivationTypeByAddress(changeAddress).purpose.toInt()
            GetAddressSetter(getAddressBuilder)
                    .setAddressN(purpose, forAccount.accountIndex, forAccount.getAddressId(changeAddress).get())
            mainThreadHandler.post { eventBus.post(OnStatusUpdate(0, SHOW_CHANGE_ADDRESS)) }
            filterMessages(signatureDevice.send(getAddressBuilder.build()))
        }

        // send initial signing-request
        val signTx = SignTx.newBuilder()
                .setCoinName(network.coinName)
                .setInputsCount(unsigned.fundingOutputs.size)
                .setOutputsCount(unsigned.outputs.size)
                .build()

        var response = try {
            signatureDevice.send(signTx)
        } catch (ex: ExtSigDeviceConnectionException) {
            postErrorMessage(ex.message!!)
            return null
        }

        val signingRequests = unsigned.signingRequests

        val signedTx = ByteWriter(1024)

        while (true) {
            // check for common response and handle them
            try {
                response = filterMessages(response, unsigned, forAccount)
            } catch (ex: ExtSigDeviceConnectionException) {
                postErrorMessage(ex.message!!)
                return null
            }

            if (response == null) {
                // Something went wrong while talking with trezor - get out of here
                return null
            }

            if (response !is TxRequest) {
                Log.e("trezor", "Trezor: Unexpected Response " + response.javaClass.toString())
                return null
            }

            // response had a part of the signed tx - write it to our buffer
            if (response.hasSerialized() && response.serialized.hasSerializedTx()) {
                signedTx.putBytes(response.serialized.serializedTx.toByteArray())
            }

            if (response.requestType == TrezorType.RequestType.TXFINISHED) {
                // We are done here...
                break
            }

            // Device asked for more information, let's process it.
            val txRequestDetailsType = response.details
            Log.d("trezor", "RequestTyp: " + response.requestType.toString())


            val requestedTx: Transaction
            requestedTx = if (txRequestDetailsType.hasTxHash()) {
                // trezor requested information about a related tx - get it from the account backing
                val requestHash = Sha256Hash.of(txRequestDetailsType.txHash.toByteArray())
                TransactionEx.toTransaction(forAccount.getTransaction(requestHash))
            } else {
                // trezor requested information about the to-be-signed tx
                Transaction.fromUnsignedTransaction(unsigned)
            }

            // Lets see, what trezor wants to know (request type)
            Log.d(TAG, "getSignedTransaction: $response")
            val txType = when (response.requestType) {
                TrezorType.RequestType.TXMETA ->
                    // Send transaction metadata
                    TrezorType.TransactionType.newBuilder()
                            .setInputsCnt(requestedTx.inputs.size)
                            .setOutputsCnt(requestedTx.outputs.size)
                            .setVersion(requestedTx.version)
                            .setLockTime(requestedTx.lockTime)
                            .build()

                TrezorType.RequestType.TXINPUT -> {
                    val akInput = requestedTx.inputs[txRequestDetailsType.requestIndex]
                    val prevHash = ByteString.copyFrom(akInput.outPoint.txid.bytes)
                    val scriptSig = ByteString.copyFrom(akInput.script.scriptBytes)
                    val txInputBuilder = TrezorType.TxInputType.newBuilder()
                            .setPrevHash(prevHash)
                            .setPrevIndex(akInput.outPoint.index)
                            .setSequence(akInput.sequence)
                            .setAmount(akInput.value)
                            .setScriptSig(scriptSig)

                    // get the bip32 path for the address, so that trezor knows with what key to sign it
                    // only for the unsigned txin
                    if (!txRequestDetailsType.hasTxHash()) {
                        val signingRequest = signingRequests[txRequestDetailsType.requestIndex]
                        val fundingUtxoScript = unsigned.fundingOutputs[txRequestDetailsType.requestIndex].script
                        val derivationType: BipDerivationType
                        txInputBuilder.scriptType = when (fundingUtxoScript) {
                            is ScriptOutputP2SH -> {
                                derivationType = BipDerivationType.BIP49
                                TrezorType.InputScriptType.SPENDP2SHWITNESS
                            }
                            is ScriptOutputP2WPKH -> {
                                derivationType = BipDerivationType.BIP84
                                TrezorType.InputScriptType.SPENDWITNESS
                            }
                            is ScriptOutputP2PKH -> {
                                derivationType = BipDerivationType.BIP44
                                TrezorType.InputScriptType.SPENDADDRESS
                            }
                            else -> {
                                postErrorMessage("Unhandled funding $fundingUtxoScript")
                                return null
                            }
                        }
                        val toSignWith = signingRequest?.publicKey?.toAddress(network, derivationType.addressType)
                        if (toSignWith == null) {
                            postErrorMessage("No address found for signing InputIDX " + txRequestDetailsType.requestIndex)
                            return null
                        }
                        val addId = forAccount.getAddressId(toSignWith)
                        if (addId.isPresent) {
                            InputAddressSetter(txInputBuilder).setAddressN(derivationType.purpose.toInt(),
                                    forAccount.accountIndex, addId.get())
                        }
                    }

                    val txInput = txInputBuilder.build()

                    TrezorType.TransactionType.newBuilder()
                            .addInputs(txInput)
                            .build()
                }

                TXOUTPUT -> {
                    val akOutput = requestedTx.outputs[txRequestDetailsType.requestIndex]

                    if (txRequestDetailsType.hasTxHash()) {
                        // request has an hash -> requests data for an existing output
                        val scriptPubKey = ByteString.copyFrom(akOutput.script.scriptBytes)
                        val txOutput = TrezorType.TxOutputBinType.newBuilder()
                                .setScriptPubkey(scriptPubKey)
                                .setAmount(akOutput.value)
                                .build()

                        TrezorType.TransactionType.newBuilder()
                                .addBinOutputs(txOutput)
                                .build()

                    } else {
                        // request has no hash -> trezor wants informations about the outputs of the new tx
                        val address = akOutput.script.getAddress(network)
                        val txOutput = TrezorType.TxOutputType.newBuilder()
                                .setAmount(akOutput.value)
                                .setScriptType(mapScriptType(akOutput.script, forAccount.isOwnInternalAddress(address)))

                        val addId = forAccount.getAddressId(address)
                        val derivationType = BipDerivationType.getDerivationTypeByAddress(address)
                        if (addId.isPresent && addId.get()[0] == 1) {
                            // If it is one of our internal change addresses, add the HD-PathID
                            // so that trezor knows, this is the change txout and can calculate the value of the tx correctly
                            OutputAddressSetter(txOutput).setAddressN(derivationType.purpose.toInt(), forAccount.accountIndex, addId.get())
                        } else {
                            // If it is regular address (non-change), set address instead of address_n
                            txOutput.address = address.toString()
                        }

                        TrezorType.TransactionType.newBuilder()
                                .addOutputs(txOutput.build())
                                .build()
                    }
                }

                TrezorType.RequestType.TXFINISHED -> {
                    Log.d(TAG, "getSignedTransaction: trezor finished")
                    null
                }

                else -> {
                    Log.e(TAG, "getSignedTransaction: We don't understand what trezor wants. Type is " + response.requestType)
                    null
                }
            }

            if (txType != null) {
                val txAck = TrezorMessage.TxAck.newBuilder()
                        .setTx(txType)
                        .build()
                response = signatureDevice.send(txAck)
            }
        }

        return try {
            Transaction.fromByteReader(ByteReader(signedTx.toBytes()))
            // TODO: 13.10.18 add this check back in and make it work with segwit.
            //checkSignedTransaction(unsigned, signedTx);
        } catch (e: Transaction.TransactionParsingException) {
            postErrorMessage("Trezor TX not valid.")
            Log.e("trezor", "Trezor TX not valid " + e.message, e)
            return null
        } catch (e: ScriptException) {
            postErrorMessage("Probably wrong passphrase.")
            Log.e(TAG, "bitcoinJ doesn't like this transaction: ", e)
            return null
        }
    }

    /**
     * At least Trezor and KeepKey have no way of knowing the input scripts as they see only the outpoints that they are asked to sign against. Therefore, they tend to sign with wrong keys, if using a passphrase that is not stored in the app. See https://github.com/mycelium-com/wallet/issues/169
     */
    private fun checkSignedTransaction(unsigned: UnsignedTransaction, signedTx: ByteWriter) {
        val networkParameters = org.bitcoinj.core.NetworkParameters.fromID(if (network.isProdnet) ID_MAINNET else ID_TESTNET)
        val tx = org.bitcoinj.core.Transaction(networkParameters, signedTx.toBytes())
        for (i in 0 until tx.inputs.size) {
            val input = tx.getInput(i.toLong())
            val scriptSig = input.scriptSig

            val addressString = unsigned.fundingOutputs[i].script.getAddress(network).toString()
            val outputScript = ScriptBuilder.createOutputScript(org.bitcoinj.core.Address.fromBase58(networkParameters, addressString))
            scriptSig.correctlySpends(tx, i.toLong(), outputScript, org.bitcoinj.script.Script.ALL_VERIFY_FLAGS)
        }
    }

    private fun mapScriptType(script: ScriptOutput, isChange: Boolean): TrezorType.OutputScriptType {
        if (isChange) {
            if (script is ScriptOutputP2SH) {
                return TrezorType.OutputScriptType.PAYTOP2SHWITNESS
            } else if (script is ScriptOutputP2WPKH) {
                return TrezorType.OutputScriptType.PAYTOWITNESS
            }
        }
        return TrezorType.OutputScriptType.PAYTOADDRESS
    }

    override fun getAccountPubKeyNode(keyPath: HdKeyPath, derivationType: BipDerivationType): Optional<HdKeyNode> {
        val msgGetPubKey = TrezorMessage.GetPublicKey.newBuilder()
                .addAllAddressN(keyPath.addressN)
                .build()

        return try {
            val resp = filterMessages(signatureDevice.send(msgGetPubKey))
            if (resp is TrezorMessage.PublicKey) {
                val pubKeyNode = resp as TrezorMessage.PublicKey?
                val pubKey = PublicKey(pubKeyNode!!.node.publicKey.toByteArray())
                val accountRootNode = HdKeyNode(
                        pubKey,
                        pubKeyNode.node.chainCode.toByteArray(),
                        3, 0,
                        keyPath.lastIndex,
                        derivationType
                )
                Optional.of(accountRootNode)
            } else {
                Optional.absent()
            }
        } catch (ex: ExtSigDeviceConnectionException) {
            postErrorMessage(ex.message!!)
            Optional.absent()
        }

    }

    override fun upgradeAccount(accountRoots: List<HdKeyNode>, walletManager: WalletManager,
                                uuid: UUID): Boolean {
        val account = walletManager.getAccount(uuid)
        return if (account is HDAccountExternalSignature) {
            walletManager.upgradeExtSigAccount(accountRoots, account)
        } else {
            false
        }
    }

    override fun createOnTheFlyAccount(accountRoots: List<HdKeyNode>, walletManager: WalletManager, accountIndex: Int) =
            accountRoots.firstOrNull { walletManager.hasAccount(it.uuid) }?.uuid
                    ?: walletManager.createExternalSignatureAccount(accountRoots, this, accountIndex)!!

    fun enterPin(pin: String) {
        pinMatrixEntry.clear()
        pinMatrixEntry.offer(pin)
    }

    private fun filterMessages(msg: Message?, transaction: UnsignedTransaction? = null, forAccount: HDAccount? = null): Message? {
        return when (msg) {
            is TrezorMessage.ButtonRequest -> processButtonRequest(msg, transaction, forAccount)
            is TrezorMessage.PinMatrixRequest -> processPinMatrixRequest(transaction, forAccount)
            is TrezorMessage.PassphraseRequest -> processPassphraseRequest(transaction, forAccount)
            is TrezorMessage.Failure -> if (postErrorMessage(msg.message, msg.code)) {
                null
            } else {
                throw RuntimeException("Trezor error:" + msg.code.toString() + "; " + msg.message)
            }
            is TxRequest -> {
                if (msg.requestType == TXOUTPUT && !msg.details.hasTxHash() && !msg.hasSerialized()) {
                    val outputIndex = msg.details.requestIndex
                    val address = transaction!!.outputs[outputIndex].script.getAddress(network)
                    if (forAccount!!.isOwnInternalAddress(address)) {
                        mainThreadHandler.post { eventBus.post(OnStatusUpdate(outputIndex, OnStatusUpdate.CurrentStatus.CONFIRM_CHANGE)) }
                    } else {
                        mainThreadHandler.post { eventBus.post(OnStatusUpdate(outputIndex, OnStatusUpdate.CurrentStatus.CONFIRM_OUTPUT)) }
                    }
                }
                msg
            }
            else -> {
                msg
            }
        }
    }

    private fun processPassphraseRequest(transaction: UnsignedTransaction?, account: HDAccount?): Message? {
        // get the user to enter a passphrase
        val passphrase = waitForPassphrase()

        val response: GeneratedMessageV3
        return if (!passphrase.isPresent) {
            // user has not provided a password - reset session on trezor and cancel
            response = TrezorMessage.ClearSession.newBuilder().build()
            signatureDevice.send(response)
            null
        } else {
            response = TrezorMessage.PassphraseAck.newBuilder()
                    .setPassphrase(passphrase.get())
                    .build()

            // send the Passphrase Response and get the response for the
            // previous requested action
            filterMessages(signatureDevice.send(response), transaction, account)
        }
    }

    private fun processPinMatrixRequest(transaction: UnsignedTransaction?, account: HDAccount?): Message? {
        mainThreadHandler.post { eventBus.post(OnPinMatrixRequest()) }
        val pin: String = try {
            // wait for the user to enter the pin
            pinMatrixEntry.take()
        } catch (e: InterruptedException) {
            ""
        }

        val txPinAck = TrezorMessage.PinMatrixAck.newBuilder()
                .setPin(pin)
                .build()

        // send the Pin Response and (if everything is okay) get the response for the
        // previous requested action
        return filterMessages(signatureDevice.send(txPinAck), transaction, account)
    }

    private fun processButtonRequest(message: Message, transaction: UnsignedTransaction?, account: HDAccount?): Message? {

        mainThreadHandler.post { eventBus.post(OnButtonRequest()) }

        if (TrezorType.ButtonRequestType.ButtonRequest_SignTx == (message as TrezorMessage.ButtonRequest).code) {
            mainThreadHandler.post { eventBus.post(OnStatusUpdate(0, OnStatusUpdate.CurrentStatus.SIGN_TRANSACTION)) }
        }

        val txButtonAck = TrezorMessage.ButtonAck.newBuilder()
                .build()
        return filterMessages(signatureDevice.send(txButtonAck), transaction, account)
    }

    private abstract inner class AddressSetter {
        abstract fun addAddressN(addressPath: Int)

        fun setAddressN(purposeNumber: Int, accountNumber: Int, addId: Array<Int>) {
            // build the full bip32 path
            Log.d(TAG, "setAddressN: m/" + purposeNumber + "'/" + network.bip44CoinType + "'/" + accountNumber + "'/" + addId[0] + "/" + addId[1])
            val addressPath = arrayOf(purposeNumber or PRIME_DERIVATION_FLAG,
                    network.bip44CoinType or PRIME_DERIVATION_FLAG,
                    accountNumber or PRIME_DERIVATION_FLAG, addId[0], addId[1])
            for (b in addressPath) {
                this.addAddressN(b)
            }
        }
    }

    private inner class OutputAddressSetter constructor(private val txOutput: TrezorType.TxOutputType.Builder) : AddressSetter() {

        override fun addAddressN(addressPath: Int) {
            txOutput.addAddressN(addressPath)
        }
    }

    private inner class InputAddressSetter constructor(private val txInput: TrezorType.TxInputType.Builder) : AddressSetter() {

        override fun addAddressN(addressPath: Int) {
            txInput.addAddressN(addressPath)
        }
    }

    private inner class GetAddressSetter constructor(private val getAddress: TrezorMessage.GetAddress.Builder) : AddressSetter() {

        override fun addAddressN(addressPath: Int) {
            getAddress.addAddressN(addressPath)
        }
    }

    companion object {
        protected const val PRIME_DERIVATION_FLAG = -0x80000000
    }
}

/**
 * Change would appear on screen of hardware device if derivation path of change contains different purpose when inputs,
 * change really exists, or inputs are from different purpose accounts.
 */
fun showChange(unsigned: UnsignedTransaction, network: NetworkParameters, account: HDAccount): Boolean {
    val fundingOutputAddressesTypes = unsigned.fundingOutputs
            .groupBy { it.script.getAddress(network).type }
            .keys

    val changeAddressType = unsigned.outputs.asSequence()
            .map { it.script.getAddress(network) }
            .filter { account.isOwnInternalAddress(it) }
            .map { it.type }
            .firstOrNull()

    // If there's a change and there are different input derivation types or input type is different with change type.
    return changeAddressType != null &&
            ((fundingOutputAddressesTypes.size > 1) || fundingOutputAddressesTypes.first() != changeAddressType)
}
