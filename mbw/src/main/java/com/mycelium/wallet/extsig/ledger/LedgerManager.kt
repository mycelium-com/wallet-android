package com.mycelium.wallet.extsig.ledger

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.nfc.Tag
import android.util.Log
import com.btchip.BTChipDongle
import com.btchip.BTChipDongle.BTChipInput
import com.btchip.BTChipDongle.BTChipOutput
import com.btchip.BTChipDongle.BTChipPublicKey
import com.btchip.BTChipDongle.OperationMode
import com.btchip.BTChipException
import com.btchip.comm.BTChipTransportFactory
import com.btchip.comm.BTChipTransportFactoryCallback
import com.btchip.comm.android.BTChipTransportAndroid
import com.btchip.utils.BufferUtils
import com.btchip.utils.Dump
import com.btchip.utils.KeyUtils
import com.btchip.utils.SignatureUtils
import com.ledger.tbase.comm.LedgerTransportTEEProxy
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory
import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.ScriptOutput
import com.mrd.bitlib.model.ScriptOutputP2PKH
import com.mrd.bitlib.model.ScriptOutputP2SH
import com.mrd.bitlib.model.ScriptOutputP2WPKH
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.model.TransactionOutput.TransactionOutputParsingException
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.ByteWriter
import com.mycelium.wallet.Constants
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.AbstractAccountScanManager
import com.mycelium.wallet.extsig.ledger.LedgerManager.On2FaRequest
import com.mycelium.wallet.extsig.ledger.LedgerManager.OnPinRequest
import com.mycelium.wallet.extsig.ledger.LedgerManager.OnShowTransactionVerification
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProvider
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignaturesAccountConfig
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.squareup.otto.Bus
import nordpol.android.OnDiscoveredTagListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

open class LedgerManager(context: Context, network: NetworkParameters, eventBus: Bus) :
    AbstractAccountScanManager(context, network, eventBus), ExternalSignatureProvider,
    OnDiscoveredTagListener {
    private var transportFactory: BTChipTransportFactory? = null
    private var dongle: BTChipDongle? = null
    private var disableTee: Boolean
    private var aid: ByteArray?
    private val pinRequestEntry: LinkedBlockingQueue<String> = LinkedBlockingQueue<String>(1)
    private val tx2FaEntry: LinkedBlockingQueue<String> = LinkedBlockingQueue<String>(1)

    // EventBus classes
    class OnPinRequest

    class OnShowTransactionVerification

    data class On2FaRequest(@JvmField val output: BTChipOutput)

    init {
        val preferences = this.context.getSharedPreferences(
            Constants.LEDGER_SETTINGS_NAME,
            Activity.MODE_PRIVATE
        )
        disableTee = preferences.getBoolean(Constants.LEDGER_DISABLE_TEE_SETTING, false)
        aid = Dump.hexToBin(
            preferences.getString(
                Constants.LEDGER_UNPLUGGED_AID_SETTING,
                DEFAULT_UNPLUGGED_AID
            )
        )
    }

    fun setTransportFactory(transportFactory: BTChipTransportFactory?) {
        this.transportFactory = transportFactory
    }

    private fun isTee(): Boolean {
        if (getTransport()!!.getTransport() !is LedgerTransportTEEProxy) {
            return false
        }
        return (getTransport()!!.getTransport() as LedgerTransportTEEProxy).hasTeeImplementation()
    }

    fun getTransport(): BTChipTransportFactory? {
        // Simple demo mode
        // If the device has the Trustlet, and it's not disabled, use it. Otherwise revert to the usual transport
        // For the full integration, bind this to accounts
        if (transportFactory == null) {
            var initialized = false
            if (!disableTee) {
                transportFactory = LedgerTransportTEEProxyFactory(context)
                val proxy = transportFactory!!.getTransport() as LedgerTransportTEEProxy
                val nvm = proxy.loadNVM(NVM_IMAGE)
                if (nvm != null) {
                    proxy.nvm = nvm
                }
                // Check if the TEE can be connected
                val waitConnected = LinkedBlockingQueue<Boolean?>(1)
                val result =
                    transportFactory!!.connect(context, object : BTChipTransportFactoryCallback {
                        override fun onConnected(success: Boolean) {
                            try {
                                waitConnected.put(success)
                            } catch (_: InterruptedException) {
                            }
                        }
                    })
                if (result) {
                    try {
                        initialized = waitConnected.poll(
                            CONNECT_TIMEOUT.toLong(),
                            java.util.concurrent.TimeUnit.MILLISECONDS
                        )!!
                    } catch (_: InterruptedException) {
                    }
                    if (initialized) {
                        initialized = proxy.init()
                    }
                }
            }
            if (!initialized) {
                transportFactory = BTChipTransportAndroid(context)
                (transportFactory as BTChipTransportAndroid).setAID(aid)
            }
            Log.d(LOG_TAG, "Using transport " + transportFactory!!.javaClass)
        }
        return transportFactory
    }

    override fun getSignedTransaction(
        unsigned: UnsignedTransaction,
        forAccount: HDAccountExternalSignature
    ): BitcoinTransaction? {
        try {
            return signInternal(unsigned, forAccount)
        } catch (e: Exception) {
            postErrorMessage(e.message!!)
            return null
        }
    }

    fun enterPin(pin: String?) {
        pinRequestEntry.clear()
        pinRequestEntry.offer(pin)
    }

    fun enterTransaction2FaPin(tx2FaPin: String?) {
        tx2FaEntry.clear()
        tx2FaEntry.offer(tx2FaPin)
    }

    @Throws(BTChipException::class, TransactionOutputParsingException::class)
    private fun signInternal(
        unsigned: UnsignedTransaction,
        forAccount: HDAccountExternalSignature
    ): BitcoinTransaction? {
        if (!initialize()) {
            postErrorMessage("Failed to connect to Ledger device")
            return null
        }
        setState(AccountScanManager.Status.readyToScan, currentAccountState)

        if (isTeePinLocked(isTee())) {
            return null
        }

        val rawOutputsWriter = ByteWriter(1024)
        writeOutputs(unsigned, rawOutputsWriter)
        val rawOutputs = rawOutputsWriter.toBytes()

        val commonPath =
            "%s" + "'/" + network.bip44CoinType + "'/" + forAccount.accountIndex + "'/"
        val changePath = getChangePath(unsigned, forAccount, commonPath)

        // Legacy firmwares only
        class LegacyParams(
            unsigned: UnsignedTransaction,
            forAccount: HDAccountExternalSignature
        ) {
            val outputAddress: String? = getOutputAddressString(unsigned, forAccount)
            val amount: String? =
                Utils.getBtcCoinType().value(calculateTotalSending(unsigned, forAccount))
                    .toString()
            val fees: String? =
                Utils.getBtcCoinType().value(unsigned.calculateFee()).toString()
        }

        val legacyParams = LegacyParams(unsigned, forAccount)

        // Prepare for creating inputs
        val unsignedTx = BitcoinTransaction.fromUnsignedTransaction(unsigned)
        val inputsNumber = unsignedTx.inputs.size
        val inputs = arrayOfNulls<BTChipInput>(inputsNumber)

        var txPin = ""
        var outputData: BTChipOutput? = null
        val signatures: MutableList<ByteArray> = ArrayList<ByteArray>(inputsNumber)


        // Fetch trusted inputs
        val isSegwit = unsigned.isSegwit()
        for (i in 0 until inputsNumber) {
            // In case of SegWit transaction inputs must be created manually
            if (isSegwit) {
                val currentInput = unsignedTx.inputs[i]
                val txOutpoint = currentInput.outPoint
                val inputHash = txOutpoint.txid.reverse().bytes
                val prevOut = unsigned.fundingOutputs[i]
                val inputBuf = ByteArrayOutputStream()
                inputBuf.write(inputHash, 0, inputHash.size)
                val index = txOutpoint.index.toLong()
                BufferUtils.writeUint32LE(inputBuf, index)
                BufferUtils.writeUint64LE(inputBuf, prevOut.value)

                inputs[i] =
                    dongle!!.createInput(inputBuf.toByteArray(), currentInput.sequence, false, true)
            } else {
                val currentInput = unsignedTx.inputs[i]
                val currentTransaction =
                    TransactionEx.toTransaction(forAccount.getTransaction(currentInput.outPoint.txid))
                val bis = ByteArrayInputStream(currentTransaction.toBytes())
                inputs[i] = dongle!!.getTrustedInput(
                    com.btchip.BitcoinTransaction(bis),
                    currentInput.outPoint.index.toLong(),
                    currentInput.sequence
                )
            }
        }

        if (isSegwit) {
            // Sending first input is kind of mark of p2sh/SegWit transaction
            var currentInput = unsignedTx.inputs[0]
            if (!tryStartingUntrustedTransaction(inputs, 0, currentInput, isSegwit)) {
                return null
            }

            // notify the activity to show the transaction details on screen
            mainThreadHandler.post(object : Runnable {
                override fun run() {
                    eventBus.post(OnShowTransactionVerification())
                }
            })

            var byteStream = ByteWriter(1024)
            byteStream.putCompactInt(unsigned.outputs.size.toLong())
            for (out in unsigned.outputs) {
                out.toByteWriter(byteStream)
            }
            outputData = dongle!!.finalizeInputFull(byteStream.toBytes(), changePath, false)

            val output = outputData
            // Check OTP confirmation
            if (outputData.isConfirmationNeeded) {
                txPin = requestConfirmation(output)

                dongle!!.startUntrustedTransction(true, 0, inputs, currentInput.getScriptCode())

                byteStream = ByteWriter(1024)
                byteStream.putCompactInt(unsigned.outputs.size.toLong())
                for (out in unsigned.outputs) {
                    out.toByteWriter(byteStream)
                }
                dongle!!.finalizeInputFull(byteStream.toBytes(), changePath, false)
            }

            for (i in 0 until inputsNumber) {
                currentInput = unsignedTx.inputs[i]
                val singleInput = arrayOf<BTChipInput?>(inputs[i])
                dongle!!.startUntrustedTransction(
                    false,
                    0,
                    singleInput,
                    currentInput.getScriptCode()
                )

                val signature = signOutput(unsigned, forAccount, commonPath, txPin, i)
                // Java Card does not canonicalize, could be enforced per platform
                signatures.add(SignatureUtils.canonicalize(signature, true, 0x01))
            }
        } else {
            for (i in unsignedTx.inputs.indices) {
                val currentInput = unsignedTx.inputs[i]
                if (!tryStartingUntrustedTransaction(inputs, i, currentInput, isSegwit)) {
                    return null
                }

                // notify the activity to show the transaction details on screen
                mainThreadHandler.post(object : Runnable {
                    override fun run() {
                        eventBus.post(OnShowTransactionVerification())
                    }
                })

                outputData = dongle!!.finalizeInput(
                    rawOutputs, legacyParams.outputAddress, legacyParams.amount,
                    legacyParams.fees, changePath
                )
                val output = outputData
                // Check OTP confirmation
                if ((i == 0) && outputData.isConfirmationNeeded) {
                    txPin = requestConfirmation(output)
                    dongle!!.startUntrustedTransction(
                        false,
                        i.toLong(),
                        inputs,
                        currentInput.script.scriptBytes
                    )
                    dongle!!.finalizeInput(
                        rawOutputs,
                        legacyParams.outputAddress,
                        legacyParams.amount,
                        legacyParams.fees,
                        changePath
                    )
                }

                val signature = signOutput(unsigned, forAccount, commonPath, txPin, i)
                // Java Card does not canonicalize, could be enforced per platform
                signatures.add(SignatureUtils.canonicalize(signature, true, 0x01))
            }
        }

        // Check if the randomized change output position was swapped compared to the one provided
        // Fully rebuilding the transaction might also be better ...
        // (kept for compatibility with the old API only)
        if ((unsigned.outputs.size == 2) && (outputData!!.value != null) && (outputData.value.size != 0)) {
            val firstOutput = unsigned.outputs[0]
            val byteReader = ByteReader(outputData.value, 1)
            val dongleOutput = TransactionOutput.fromByteReader(byteReader)
            if ((firstOutput.value != dongleOutput.value) ||
                (!firstOutput.script.scriptBytes
                    .contentEquals(dongleOutput.script.scriptBytes))
            ) {
                unsigned.outputs[0] = unsigned.outputs[1]
                unsigned.outputs[1] = firstOutput
            }
        }
        // Add signatures
        return StandardTransactionBuilder.finalizeTransaction(unsigned, signatures)
    }

    @Throws(BTChipException::class)
    private fun signOutput(
        unsigned: UnsignedTransaction, forAccount: HDAccountExternalSignature, commonPath: String?,
        txPin: String?, outputIndex: Int
    ): ByteArray? {
        // Sign
        val signatureInfo = unsigned.signingRequests
        val signingRequest = signatureInfo[outputIndex]
        val fundingUtxoScript = unsigned.fundingOutputs[outputIndex].script
        val derivationType = getBipDerivationType(fundingUtxoScript)

        requireNotNull(derivationType) { "Can't sign one of the inputs" }
        val toSignWith = signingRequest?.publicKey?.toAddress(network, derivationType.addressType)!!
        val addressId = forAccount.getAddressId(toSignWith)
        val keyPath = String.format(
            commonPath + addressId.get()!![0] + "/" + addressId.get()!![1],
            derivationType.purpose.toString()
        )
        return dongle!!.untrustedHashSign(keyPath, txPin)
    }

    private fun requestConfirmation(output: BTChipOutput): String {
        mainThreadHandler.post(object : Runnable {
            override fun run() {
                eventBus.post(On2FaRequest(output))
            }
        })
        val txPin = try {
            // wait for the user to enter the pin
            tx2FaEntry.take()
        } catch (_: InterruptedException) {
            ""
        }
        Log.d(LOG_TAG, "Reinitialize transport")
        initialize()
        Log.d(LOG_TAG, "Reinitialize transport done")
        return txPin
    }

    @Throws(BTChipException::class)
    private fun tryStartingUntrustedTransaction(
        inputs: Array<BTChipInput?>, i: Int, currentInput: TransactionInput,
        isSegwit: Boolean
    ): Boolean {
        val scriptBytes = if (isSegwit) {
            currentInput.getScriptCode()
        } else {
            currentInput.getScript().scriptBytes
        }
        try {
            dongle!!.startUntrustedTransction(i == 0, i.toLong(), inputs, scriptBytes)
        } catch (e: BTChipException) {
            // If pin was not entered wait for pin being entered and try again.
            if (e.sw == SW_PIN_NEEDED) {
                if (isTee()) {
                    //if (dongle.hasScreenSupport()) {
                    // PIN request is prompted on screen
                    if (!waitForTeePin()) {
                        return false
                    }
                    dongle!!.startUntrustedTransction(i == 0, i.toLong(), inputs, scriptBytes)
                } else {
                    val pin = waitForPin()
                    try {
                        Log.d(LOG_TAG, "Reinitialize transport")
                        initialize()
                        Log.d(LOG_TAG, "Reinitialize transport done")
                        dongle!!.verifyPin(pin.toByteArray())
                        dongle!!.startUntrustedTransction(i == 0, i.toLong(), inputs, scriptBytes)
                    } catch (e1: BTChipException) {
                        Log.d(LOG_TAG, "2fa error", e1)
                        postErrorMessage("Invalid second factor")
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun waitForPin(): String {
        mainThreadHandler.post(object : Runnable {
            override fun run() {
                eventBus.post(OnPinRequest())
            }
        })
        return try {
            // wait for the user to enter the pin
            pinRequestEntry.take()
        } catch (_: InterruptedException) {
            ""
        }
    }

    private fun waitForTeePin(): Boolean {
        try {
            dongle!!.verifyPin(DUMMY_PIN.toByteArray())
        } catch (e1: BTChipException) {
            if ((e1.sw and 0xfff0) == SW_INVALID_PIN) {
                postErrorMessage("Invalid PIN - " + (e1.getSW() - SW_INVALID_PIN) + " attempts remaining")
                return false
            }
        } finally {
            // Poor man counter
            val proxy = getTransport()!!.getTransport() as LedgerTransportTEEProxy
            try {
                val updatedNvm = proxy.requestNVM().get()
                proxy.writeNVM(NVM_IMAGE, updatedNvm)
                proxy.nvm = updatedNvm
            } catch (_: Exception) {
            }
        }
        return true
    }

    private fun getBipDerivationType(fundingUtxoScript: ScriptOutput?): BipDerivationType? {
        var derivationType: BipDerivationType?
        if (fundingUtxoScript is ScriptOutputP2SH) {
            derivationType = BipDerivationType.BIP49
        } else if (fundingUtxoScript is ScriptOutputP2WPKH) {
            derivationType = BipDerivationType.BIP84
        } else if (fundingUtxoScript is ScriptOutputP2PKH) {
            derivationType = BipDerivationType.BIP44
        } else {
            postErrorMessage("Unhandled funding $fundingUtxoScript")
            return null
        }
        return derivationType
    }

    private fun getChangePath(
        unsigned: UnsignedTransaction,
        forAccount: HDAccountExternalSignature,
        commonPath: String
    ): String {
        var changePath = ""
        unsigned.outputs.forEach { o ->
            val toAddress = o.script.getAddress(network)
            val purpose =
                BipDerivationType.Companion.getDerivationTypeByAddress(toAddress).purpose.toString()
            val addressId = forAccount.getAddressId(toAddress)
            if (addressId.isPresent && addressId.get()[0] == 1) {
                changePath = String.format(
                    String.format(
                        "%s%d/%d",
                        commonPath,
                        addressId.get()[0],
                        addressId.get()[1]
                    ), purpose
                )
            }
        }
        return changePath
    }

    private fun getOutputAddressString(
        unsigned: UnsignedTransaction,
        forAccount: HDAccountExternalSignature
    ): String? {
        var outputAddress: String? = null
        for (output in unsigned.outputs) {
            val toAddress = output.script.getAddress(network)
            val addressId = forAccount.getAddressId(toAddress)
            if (!(addressId.isPresent && addressId.get()!![0] == 1)) {
                // this output goes to a foreign address (addressId[0]==1 means its internal change)
                outputAddress = toAddress.toString()
            }
        }
        return outputAddress
    }

    private fun writeOutputs(unsigned: UnsignedTransaction, rawOutputsWriter: ByteWriter) {
        rawOutputsWriter.putCompactInt(unsigned.outputs.size.toLong())
        for (output in unsigned.outputs) {
            output.toByteWriter(rawOutputsWriter)
        }
    }

    private fun calculateTotalSending(
        unsigned: UnsignedTransaction,
        forAccount: HDAccountExternalSignature
    ): Long {
        var totalSending: Long = 0
        for (output in unsigned.outputs) {
            val toAddress = output.script.getAddress(network)
            val addressId = forAccount.getAddressId(toAddress)
            if (!(addressId.isPresent && addressId.get()!![0] == 1)) {
                // this output goes to a foreign address (addressId[0]==1 means its internal change)
                totalSending += output.value
            }
        }
        return totalSending
    }

    private fun isTeePinLocked(isTEE: Boolean): Boolean {
        if (isTEE) {
            val PIN_IS_TERMINATED = "PIN is terminated"
            // Check that the TEE PIN is not blocked
            try {
                val attempts = dongle!!.getVerifyPinRemainingAttempts()
                if (attempts == 0) {
                    postErrorMessage(PIN_IS_TERMINATED)
                    return true
                }
            } catch (e: BTChipException) {
                if (conditionsAreNotSatisfied(e)) {
                    return true
                }
                if (e.sw == SW_HALTED) {
                    val proxy = getTransport()!!.getTransport() as LedgerTransportTEEProxy
                    try {
                        proxy.close()
                        proxy.init()
                        val attempts = dongle!!.getVerifyPinRemainingAttempts()
                        if (attempts == 0) {
                            postErrorMessage(PIN_IS_TERMINATED)
                            return true
                        }
                    } catch (e1: BTChipException) {
                        if (conditionsAreNotSatisfied(e1)) {
                            return true
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return false
    }

    private fun conditionsAreNotSatisfied(e: BTChipException): Boolean {
        if (e.sw == SW_CONDITIONS_NOT_SATISFIED) {
            postErrorMessage("PIN is terminated")
            return true
        }
        return false
    }

    override fun onBeforeScan(): Boolean {
        val initialized = initialize()
        if (!initialized) {
            postErrorMessage("Failed to connect to Ledger device")
            return false
        }
        // we have found a device with ledger USB ID, but some devices (eg NanoS) have more than one
        // application, the call to getFirmwareVersion will fail, if it isn't in the bitcoin app
        try {
            dongle!!.getFirmwareVersion()
        } catch (e: BTChipException) {
            // this error is expected for ledger unplugged, just continue
            if (e.sw != 0x6700) {
                postErrorMessage("Unable to get firmware version - if your ledger supports multiple applications please open the bitcoin app")
                return false
            }
        }
        return true
    }

    private fun initialize(): Boolean {
        Log.d(LOG_TAG, "Initialize")
        val waitConnected = LinkedBlockingQueue<Boolean?>(1)
        while (!getTransport()!!.isPluggedIn()) {
            dongle = null
            try {
                setState(AccountScanManager.Status.unableToScan, currentAccountState)
                Thread.sleep(PAUSE_RESCAN.toLong())
            } catch (_: InterruptedException) {
                break
            }
        }
        val connectResult =
            getTransport()!!.connect(context, object : BTChipTransportFactoryCallback {
                override fun onConnected(success: Boolean) {
                    try {
                        waitConnected.put(success)
                    } catch (_: InterruptedException) {
                    }
                }
            })
        if (!connectResult) {
            return false
        }
        val connected: Boolean = try {
            waitConnected.poll(
                CONNECT_TIMEOUT.toLong(),
                java.util.concurrent.TimeUnit.MILLISECONDS
            )!!
        } catch (_: InterruptedException) {
            false
        }
        if (connected) {
            Log.d(LOG_TAG, "Connected")
            getTransport()!!.getTransport().setDebug(true)
            dongle = BTChipDongle(getTransport()!!.getTransport())
            dongle!!.setKeyRecovery(MyceliumKeyRecovery())
        }
        Log.d(LOG_TAG, "Initialized $connected")
        return connected
    }

    fun isPluggedIn(): Boolean = getTransport()!!.isPluggedIn()

    override fun upgradeAccount(
        accountRoots: List<HdKeyNode>,
        walletManager: WalletManager,
        uuid: UUID
    ): Boolean {
        val account = walletManager.getAccount(uuid)
        if (account is HDAccountExternalSignature) {
            // TODO make the module name defined programmatically
            return (walletManager.getModuleById(BitcoinHDModule.ID) as BitcoinHDModule)
                .upgradeExtSigAccount(accountRoots, account)
        }
        return false
    }

    override fun createOnTheFlyAccount(
        accountRoots: List<HdKeyNode>,
        walletManager: WalletManager,
        accountIndex: Int
    ): UUID? {
        var account: UUID? = null
        accountRoots.forEach { accountRoot ->
            if (walletManager.hasAccount(accountRoot.getUuid())) {
                // Account already exists
                account = accountRoot.getUuid()
            }
        }
        if (account == null) {
            account = walletManager.createAccounts(
                ExternalSignaturesAccountConfig(accountRoots, this, accountIndex)
            ).first()
        }
        return account
    }

    override fun getAccountPubKeyNode(
        keyPath: HdKeyPath,
        derivationType: BipDerivationType
    ): HdKeyNode? {
        val isTEE = isTee()
        // ledger needs it in the format "/44'/0'/0'" - our default toString format
        // is with leading "m/" -> replace the "m" away
        val keyPathString = keyPath.toString().replace("m/", "")
        if (isTEE) {
            // Verify that the TEE is set up properly - PIN cannot be locked here
            // as this is called directly after the account creation
            try {
                val attempts = dongle!!.getVerifyPinRemainingAttempts()
            } catch (e: BTChipException) {
                if (e.sw == SW_HALTED) {
                    val proxy = getTransport()!!.getTransport() as LedgerTransportTEEProxy
                    try {
                        proxy.close()
                        proxy.init()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        val addressByte = getAddressByte(derivationType)
        try {
            var publicKey: BTChipPublicKey
            try {
                publicKey = dongle!!.getWalletPublicKey(keyPathString, addressByte)
            } catch (e: BTChipException) {
                if (isTEE && (e.sw == SW_CONDITIONS_NOT_SATISFIED)) {
                    val proxy = getTransport()!!.getTransport() as LedgerTransportTEEProxy
                    // Not setup ? We can do it on the fly
                    dongle!!.setup(
                        arrayOf<OperationMode>(OperationMode.WALLET),
                        arrayOf<BTChipDongle.Feature>(BTChipDongle.Feature.RFC6979),  // TEE doesn't need NO_2FA_P2SH
                        network.standardAddressHeader,
                        network.multisigAddressHeader,
                        ByteArray(4), null,
                        null,
                        null, null
                    )
                    try {
                        val updatedNvm = proxy.requestNVM().get()
                        proxy.writeNVM(NVM_IMAGE, updatedNvm)
                        proxy.nvm = updatedNvm
                    } catch (_: Exception) {
                    }
                    try {
                        dongle!!.verifyPin(DUMMY_PIN.toByteArray())
                    } catch (e1: BTChipException) {
                        if ((e1.sw and 0xfff0) == SW_INVALID_PIN) {
                            postErrorMessage("Invalid PIN - " + (e1.sw - SW_INVALID_PIN) + " attempts remaining")
                            return null
                        }
                    } finally {
                        try {
                            val updatedNvm = proxy.requestNVM().get()
                            proxy.writeNVM(NVM_IMAGE, updatedNvm)
                            proxy.nvm = updatedNvm
                        } catch (_: Exception) {
                        }
                    }
                    publicKey = dongle!!.getWalletPublicKey(keyPathString, addressByte)
                } else if (e.sw == SW_PIN_NEEDED) {
                    //if (dongle.hasScreenSupport()) {
                    if (isTEE) {
                        if (!waitForTeePin()) {
                            return null
                        }
                        publicKey = dongle!!.getWalletPublicKey(keyPathString, addressByte)
                    } else {
                        val pin = waitForPin()
                        try {
                            Log.d(LOG_TAG, "Reinitialize transport")
                            initialize()
                            Log.d(LOG_TAG, "Reinitialize transport done")
                            dongle!!.verifyPin(pin.toByteArray())
                            publicKey = dongle!!.getWalletPublicKey(keyPathString, addressByte)
                        } catch (e1: BTChipException) {
                            if ((e1.sw and 0xfff0) == SW_INVALID_PIN) {
                                postErrorMessage("Invalid PIN - " + (e1.sw - SW_INVALID_PIN) + " attempts remaining")
                                return null
                            } else {
                                Log.d(LOG_TAG, "Connect error", e1)
                                postErrorMessage("Error connecting to Ledger device")
                                return null
                            }
                        }
                    }
                } else {
                    postErrorMessage("Internal error $e")
                    return null
                }
            }
            val pubKey = PublicKey(KeyUtils.compressPublicKey(publicKey.publicKey))
            val accountRootNode = HdKeyNode(
                pubKey,
                publicKey.chainCode,
                3,
                0,
                keyPath.lastIndex,
                derivationType
            )
            return accountRootNode
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Generic error", e)
            postErrorMessage(e.message!!)
            return null
        }
    }

    private fun getAddressByte(derivationType: BipDerivationType): Byte {
        var addressByte: Byte = 0x00
        addressByte = when (derivationType) {
            BipDerivationType.BIP44 -> 0x00
            BipDerivationType.BIP49 -> 0x01
            BipDerivationType.BIP84 -> 0x02
            BipDerivationType.BIP86 -> 0x00 // TODO check what byte need for 86
        }
        return addressByte
    }

    override fun getBIP44AccountType(): Int =
        HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER

    override fun getLabelOrDefault(): String = context.getString(R.string.ledger)

    fun getDisableTEE(): Boolean = disableTee

    fun setDisableTEE(disabled: Boolean) {
        val editor = getEditor()
        disableTee = disabled
        editor.putBoolean(Constants.LEDGER_DISABLE_TEE_SETTING, disabled)
        editor.commit()
    }

    fun getUnpluggedAID(): String? = Dump.dump(aid)

    fun setUnpluggedAID(aid: String) {
        val editor = getEditor()
        this.aid = Dump.hexToBin(aid)
        editor.putString(Constants.LEDGER_UNPLUGGED_AID_SETTING, aid)
        editor.commit()
    }

    private fun getEditor(): SharedPreferences.Editor =
        context.getSharedPreferences(Constants.LEDGER_SETTINGS_NAME, Activity.MODE_PRIVATE)
            .edit()

    override fun tagDiscovered(tag: Tag?) {
        Log.d(LOG_TAG, "NFC Card detected")
        if (getTransport() is BTChipTransportAndroid) {
            val transport = getTransport() as BTChipTransportAndroid
            transport.setDetectedTag(tag)
        }
    }

    companion object {
        private const val LOG_TAG = "LedgerManager"

        private const val CONNECT_TIMEOUT = 2000

        private const val PAUSE_RESCAN = 4000
        private const val SW_PIN_NEEDED = 0x6982
        private const val SW_CONDITIONS_NOT_SATISFIED = 0x6985
        private const val SW_INVALID_PIN = 0x63C0
        private const val SW_HALTED = 0x6faa

        private const val NVM_IMAGE = "nvm.bin"

        private val ACTIVATE_ALT_2FA: ByteArray? = byteArrayOf(
            0xE0.toByte(),
            0x26.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x01.toByte()
        )

        private const val DUMMY_PIN = "0000"

        private const val DEFAULT_UNPLUGGED_AID = "a0000006170054bf6aa94901"
    }
}
