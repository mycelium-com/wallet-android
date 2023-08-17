package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.colu.json.AddressTransactionsInfo
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex
import com.mycelium.wapi.wallet.colu.json.Tx
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bitcoinj.script.ScriptBuilder
import java.io.IOException
import java.util.*

class ColuAccount(val context: ColuAccountContext, val privateKey: InMemoryPrivateKey?
                  , private val type: CryptoCurrency
                  , val networkParameters: NetworkParameters
                  , private val coluClient: ColuApi
                  , private val accountBacking: ColuAccountBacking
                  , val backing: WalletBacking<ColuAccountContext>
                  , val listener: AccountListener? = null
                  , val wapi: Wapi) : WalletAccount<BtcAddress>, ExportableAccount, SyncPausableAccount() {
    override fun queueTransaction(transaction: Transaction) {
    }

    override val basedOnCoinType: CryptoCurrency
        get() = if (networkParameters.isProdnet) BitcoinMain else BitcoinTest

    var linkedAccount: SingleAddressAccount? = null

    override val dependentAccounts: List<WalletAccount<*>>
        get() = listOfNotNull(linkedAccount)

    override fun isSpendingUnconfirmed(tx: Transaction): Boolean = false

    override fun getTx(txid: ByteArray): Transaction {
        val txJson = accountBacking.getTx(Sha256Hash(txid))
        val coluTx = ColuTransaction(coinType, null, null, null)
        val bitcoinTx = BitcoinTransaction.fromBytes(HexUtils.toBytes(txJson.hex))
        coluTx.transaction = bitcoinTx
        return coluTx
    }

    override fun isSyncing(): Boolean {
        //TODO: implement later
        return false
    }

    override fun getDummyAddress(subType: String): BtcAddress {
        val address = BitcoinAddress.getNullAddress(networkParameters, AddressType.valueOf(subType))
        return BtcAddress(coinType, address)
    }

    override val dummyAddress: BtcAddress =
        BtcAddress(coinType, BitcoinAddress.getNullAddress(networkParameters))

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExchangeable(): Boolean {
        return false
    }

    protected var uuid: UUID
    var coluLabel: String? = null

    protected var cachedBalance: Balance
    private val addressList: Map<AddressType, BtcAddress> = context.address ?: mapOf()

    init {
        uuid = ColuUtils.getGuidForAsset(type, addressList[AddressType.P2PKH]?.getBytes())
        val transactions = accountBacking.getTransactions(0, 2000)
        val transactionSummaries = getGenericListFromJsonTxList(transactions)
        cachedBalance = calculateBalance(emptyList(), transactionSummaries)
    }

    override fun getTransactionsSince(receivingSince: Long): List<TransactionSummary> {
        val transactionsSince = accountBacking.getTransactionsSince(receivingSince)
        return getGenericListFromJsonTxList(transactionsSince)
    }

    private fun convert(publicKey: PublicKey, coinType: ColuMain?): Map<AddressType, BtcAddress> {
        val btcAddress = mutableMapOf<AddressType, BtcAddress>()
        for (address in publicKey.getAllSupportedAddresses(networkParameters)) {
            btcAddress[address.key] = BtcAddress(coinType, address.value)
        }
        return btcAddress
    }

    override val id: UUID
        get() = uuid

    override var label: String = "Unknown"

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val receiveAddress: BtcAddress
        get() = addressList[AddressType.P2PKH] ?: addressList.values.toList()[0]

    override val coinType: CryptoCurrency
        get() = type

    override val accountBalance: Balance
        get() = cachedBalance

    override fun isMineAddress(address: Address?): Boolean {
        for (btcAddress in addressList) {
            if (btcAddress.value == address) {
                return true
            }
        }
        return false
    }

    override fun getTxSummary(transactionId: ByteArray): TransactionSummary? {
        checkNotArchived()
        val transaction = accountBacking.getTx(Sha256Hash.of(transactionId))
        return genericTransactionSummaryFromJson(transaction)
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): List<TransactionSummary> {
        val transactions = accountBacking.getTransactions(offset, limit)
        return getGenericListFromJsonTxList(transactions)
    }

    override fun getBlockChainHeight(): Int = context.blockHeight

    override fun calculateMaxSpendableAmount(minerFeeToUse: Value, destinationAddress: BtcAddress?, txData: TransactionData?): Value {
        return accountBalance.spendable
    }

    @Synchronized
    override suspend fun synchronize(mode: SyncMode?): Boolean {
        // retrieve history from colu server
        try {
            if (!maySync) {
                return false
            }
            val json = coluClient.getAddressTransactions(receiveAddress)
            val genericTransactionSummaries = getGenericListFromJsonTxList(json.transactions)
            val utxosFromJson = utxosFromJson(json, receiveAddress)
            accountBacking.clear()
            accountBacking.putTransactions(json.transactions)
            cachedBalance = calculateBalance(utxosFromJson, genericTransactionSummaries)
            listener?.balanceUpdated(this)
            lastSyncInfo = SyncStatusInfo(SyncStatus.SUCCESS)
            return true
        } catch (e: IOException) {
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            return false
        }
    }

    private fun getGenericListFromJsonTxList(transactions: MutableList<Tx.Json>) =
            transactions.mapNotNull { genericTransactionSummaryFromJson(it) }

    private fun calculateBalance(unspent: List<TransactionOutputEx>, transactions: List<TransactionSummary>): Balance {
        var confirmed = Value.zeroValue(coinType)
        var receiving = Value.zeroValue(coinType)
        var sending = Value.zeroValue(coinType)
        var change = Value.zeroValue(coinType)

        for (tx in transactions) {
            var s = Value.zeroValue(coinType)
            var r = Value.zeroValue(coinType)
            tx.inputs.forEach {
                if (tx.confirmations == 0 && isMineAddress(it.address)) {
                    sending += it.value
                    s += it.value
                }
            }
            tx.outputs.forEach {
                if (tx.confirmations == 0 && isMineAddress(it.address)) {
                    receiving += it.value
                    r += it.value
                }
            }
            if (s > r) {
                change += r
                receiving -= r
            }
        }
        unspent.forEach {
            if (it.height != -1) {
                confirmed += it.value
            }
        }
        return Balance(confirmed, receiving, sending, change)
    }


    override fun canSpend(): Boolean = privateKey != null

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        var privKey = Optional.absent<String>()
        val publicDataMap = HashMap<BipDerivationType, String>()
        if (canSpend()) {
            try {
                privKey = Optional.of(this.privateKey!!.getBase58EncodedPrivateKey(networkParameters))
            } catch (ignore: KeyCipher.InvalidKeyCipher) {
            }

        }
        publicDataMap[BipDerivationType.getDerivationTypeByAddressType(AddressType.P2PKH)] = receiveAddress.toString()
        return ExportableAccount.Data(privKey, publicDataMap)
    }

    override val isArchived: Boolean
        get() = context.isArchived()

    override val isActive: Boolean
        get() = !context.isArchived()

    override fun archiveAccount() {
        context.setArchived(true)
        backing.updateAccountContext(context)
    }

    protected fun checkNotArchived() {
        val usingArchivedAccount = "Using archived account"
        if (isArchived) {
            throw RuntimeException(usingArchivedAccount)
        }
    }

    override fun activateAccount() {
        context.setArchived(false)
        backing.updateAccountContext(context)
    }

    override fun dropCachedData() {
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed(): Boolean = false

    override fun broadcastOutgoingTransactions(): Boolean = true

    override val syncTotalRetrievedTransactions: Int = 0

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        val feePerKb = (fee as FeePerKbFee).feePerKb
        val coluTx = ColuTransaction(coinType, address as BtcAddress, amount, feePerKb)
        val fromAddresses = mutableListOf(receiveAddress)
        val json = coluClient.prepareTransaction(coluTx.destination!!, fromAddresses, coluTx.amount!!, feePerKb)
        coluTx.baseTransaction = json
        return coluTx
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        if (request is ColuTransaction) {
            val signedTransaction = signTransaction(request.baseTransaction, this, keyCipher)
            request.transaction = signedTransaction
        } else {
            TODO("signTx not implemented for ${request.javaClass.simpleName}")
        }
    }

    private fun signTransaction(txid: ColuBroadcastTxHex.Json?, coluAccount: ColuAccount?, keyCipher: KeyCipher): BitcoinTransaction? {
        if (txid == null) {
//            Log.e(TAG, "signTx: No transaction to sign !")
            return null
        }
        if (coluAccount == null) {
//            Log.e(TAG, "signTx: No colu account associated to transaction to sign !")
            return null
        }

        // use bitcoinj classes and two methods above to generate signatures
        // and sign transaction
        // then convert to mycelium wallet transaction format
        // Step 1: map to bitcoinj classes

        // DEV only 1 key
        val txBytes: ByteArray?

        try {
            txBytes = Hex.decodeHex(txid.txHex.toCharArray())
        } catch (e: org.apache.commons.codec.DecoderException) {
//            Log.e(TAG, "signTx: exception while decoding transaction hex code.")
            return null
        }

        if (txBytes == null) {
//            Log.e(TAG, "signTx: failed to decode transaction hex code.")
            return null
        }


        val id =
                when (networkParameters.networkType){
                    NetworkParameters.NetworkType.PRODNET -> org.bitcoinj.core.NetworkParameters.ID_MAINNET
                    NetworkParameters.NetworkType.TESTNET -> org.bitcoinj.core.NetworkParameters.ID_TESTNET
                    NetworkParameters.NetworkType.REGTEST -> TODO()
                }
        val parameters = org.bitcoinj.core.NetworkParameters.fromID(id)
        val signTx = org.bitcoinj.core.Transaction(parameters, txBytes)

        val privateKeyBytes = coluAccount.getPrivateKey(keyCipher).privateKeyBytes
        val publicKeyBytes = coluAccount.getPrivateKey(keyCipher).publicKey.publicKeyBytes
        val ecKey = ECKey.fromPrivateAndPrecalculatedPublic(privateKeyBytes, publicKeyBytes)

        val inputScript = ScriptBuilder.createOutputScript(ecKey.toAddress(parameters))
        for (i in 0 until signTx.inputs.size) {
            val signature = signTx.calculateSignature(i, ecKey, inputScript, org.bitcoinj.core.Transaction.SigHash.ALL, false)
            val scriptSig = ScriptBuilder.createInputScript(signature, ecKey)
            signTx.getInput(i.toLong()).scriptSig = scriptSig
        }

        val signedTransactionBytes = signTx.bitcoinSerialize()
        val signedBitlibTransaction: BitcoinTransaction
        try {
            signedBitlibTransaction = BitcoinTransaction.fromBytes(signedTransactionBytes)
        } catch (e: BitcoinTransaction.TransactionParsingException) {
//            Log.e(TAG, "signTx: Error parsing bitcoinj transaction ! msg: " + e.message)
            return null
        }

        return signedBitlibTransaction
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        val coluTx = tx as ColuTransaction
        return if (coluTx.transaction != null && coluClient.broadcastTx(coluTx.transaction!!) != null) {
            BroadcastResult(BroadcastResultType.SUCCESS)
        } else {
            BroadcastResult(BroadcastResultType.REJECTED)
        }
    }

    override val typicalEstimatedTransactionSize: Int =
        // Colu transaction is a typical bitcoin transaction containing additional inputs and OP_RETURN data
        // Typical Colu transaction has 2 inputs and 4 outputs
        FeeEstimatorBuilder().setLegacyInputs(2)
            .setLegacyOutputs(4)
            .createFeeEstimator()
            .estimateTransactionSize()

    override fun getUnspentOutputViewModels(): List<OutputViewModel> {
        val result = mutableListOf<OutputViewModel>()
        accountBacking.unspentOutputs.forEach {
            result.add(OutputViewModel(receiveAddress, Value.valueOf(coinType, it.value), false))
        }
        return result
    }

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        return privateKey!!
    }

    private fun genericTransactionSummaryFromJson(transaction: Tx.Json): TransactionSummary? {
        var transferred = Value.zeroValue(coinType)
        val destinationAddresses = arrayListOf<Address>()

        val input = mutableListOf<InputViewModel>()
        transaction.vin.forEach { vin ->
            vin.assets.filter { it.assetId == coinType.id }.forEach { asset ->
                val value = Value.valueOf(coinType, asset.amount)
                val address = BitcoinAddress.fromString(vin.previousOutput.addresses[0])
                input.add(InputViewModel(
                        BtcAddress(coinType, address), value, false))
                if (vin.previousOutput.addresses.contains(receiveAddress.toString())) {
                    transferred -= value
                }
            }
        }

        val output = mutableListOf<OutputViewModel>()
        transaction.vout.forEach { vout ->
            vout.assets.filter { it.assetId == coinType.id }.forEach { asset ->
                val value = Value.valueOf(coinType, asset.amount)
                val address = BitcoinAddress.fromString(vout.scriptPubKey.addresses[0])
                val genAddress = AddressUtils.from(coinType,address.toString())
                if (!isMineAddress(genAddress)) {
                    destinationAddresses.add(genAddress)
                }

                output.add(OutputViewModel(
                        BtcAddress(coinType, address), value, false))
                if (vout.scriptPubKey.addresses.contains(receiveAddress.toString())) {
                    transferred += value
                }
            }
        }

        if (input.size > 0 || output.size > 0) {

            return TransactionSummary(
                    coinType,
                    Sha256Hash.fromString(transaction.txid).bytes,
                    Sha256Hash.fromString(transaction.hash).bytes,
                    transferred,
                    transaction.time / 1000,
                    transaction.blockheight.toInt(),
                    transaction.confirmations,
                    false,
                    input,
                    output,
                    destinationAddresses,
                    ConfirmationRiskProfileLocal(0, false, false),
                    0,
                    Value.valueOf(basedOnCoinType, transaction.fee.toLong()))
        }

        return null
    }

    private fun utxosFromJson(json: AddressTransactionsInfo.Json, address: Address): MutableList<TransactionOutputEx> {
        val utxos = mutableListOf<TransactionOutputEx>()
        for (utxo in json.utxos) {
            utxo.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                utxos.add(TransactionOutputEx(OutPoint(Sha256Hash.fromString(utxo.txid), utxo.index), utxo.blockheight,
                        asset.amount, utxo.scriptPubKey.asm.toByteArray(), false))
            }
        }
        return utxos
    }

    override fun canSign(): Boolean = privateKey != null

    override fun signMessage(message: String, address: Address?): String {
        return privateKey!!.signMessage(message).base64Signature
    }
}