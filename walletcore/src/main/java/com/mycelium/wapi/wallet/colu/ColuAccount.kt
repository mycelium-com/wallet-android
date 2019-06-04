package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
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
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bitcoinj.script.ScriptBuilder
import java.util.*

class ColuAccount(val context: ColuAccountContext, val privateKey: InMemoryPrivateKey?
                  , private val type: CryptoCurrency
                  , val networkParameters: NetworkParameters
                  , val coluClient: ColuApi
                  , val accountBacking: ColuAccountBacking
                  , val backing: WalletBacking<ColuAccountContext>
                  , val listener: AccountListener? = null
                  , val wapi: Wapi) : WalletAccount<BtcAddress>, ExportableAccount {

    var linkedAccount: SingleAddressAccount? = null

    override fun getDependentAccounts(): MutableList<WalletAccount<*>> {
        val result = ArrayList<WalletAccount<*>>()
        linkedAccount?.let {
            result.add(it)
        }
        return result
    }

    override fun getTransactions(offset: Int, limit: Int): MutableList<GenericTransaction> {
        return ArrayList()
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        return false
    }

    override fun getTx(byte: ByteArray): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSyncing(): Boolean {
        //TODO: implement later
        return false
    }

    override fun getDefaultFeeEstimation(): FeeEstimationsGeneric {
        return FeeEstimationsGeneric(
                Value.valueOf(coinType, 1000),
                Value.valueOf(coinType, 3000),
                Value.valueOf(coinType, 6000),
                Value.valueOf(coinType, 8000),
                0
        )
    }

    override fun getDummyAddress(subType: String): BtcAddress {
        val address = Address.getNullAddress(networkParameters, AddressType.valueOf(subType))
        return BtcAddress(coinType, address)
    }

    override fun getDummyAddress(): BtcAddress {
        return BtcAddress(coinType, Address.getNullAddress(networkParameters))
    }

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExchangeable(): Boolean {
        return false
    }

    protected var uuid: UUID
    var coluLabel: String? = null

    @Volatile
    protected var _isSynchronizing: Boolean = false

    protected var cachedBalance: Balance
    private val addressList: Map<AddressType, BtcAddress>

    init {
        addressList = if (context.publicKey != null) {
            convert(context.publicKey, type as ColuMain)
        } else {
            context.address ?: mapOf()
        }
        uuid = ColuUtils.getGuidForAsset(type, addressList[AddressType.P2PKH]?.getBytes())
        cachedBalance = calculateBalance(emptyList(), accountBacking.getTransactionSummaries(0, 2000))
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> {
        return accountBacking.getTransactionsSince(receivingSince)
    }

    private fun convert(publicKey: PublicKey, coinType: ColuMain?): Map<AddressType, BtcAddress> {
        val btcAddress = mutableMapOf<AddressType, BtcAddress>()
        for (address in publicKey.getAllSupportedAddresses(networkParameters)) {
            btcAddress[address.key] = BtcAddress(coinType, address.value)
        }
        return btcAddress
    }

    override fun getId(): UUID = uuid

    override fun getLabel(): String {
        return coluLabel.toString()
    }

    override fun setLabel(label: String?) {
        coluLabel = label
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress(): GenericAddress {
        return addressList[AddressType.P2PKH] ?: addressList.values.toList()[0]
    }

    override fun getCoinType(): CryptoCurrency = type

    override fun getAccountBalance(): Balance = cachedBalance


    override fun isMineAddress(address: GenericAddress?): Boolean {
        for (btcAddress in addressList) {
            if (btcAddress.value == address) {
                return true
            }
        }
        return false
    }

    override fun getTxSummary(transactionId: ByteArray): GenericTransactionSummary? {
        checkNotArchived()
        return accountBacking.getTxSummary(Sha256Hash.of(transactionId))
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): List<GenericTransactionSummary> {
        return accountBacking.getTransactionSummaries(offset, limit)
    }

    override fun getBlockChainHeight(): Int = context.blockHeight

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long, destinationAddres: BtcAddress): Value {
        return accountBalance.spendable
    }

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        // we try to get fee estimation from server
        try {
            val response = wapi.minerFeeEstimations
            val oldStyleFeeEstimation = response.result.feeEstimation
            val lowPriority = oldStyleFeeEstimation.getEstimation(20)
            val normal = oldStyleFeeEstimation.getEstimation(3)
            val economy = oldStyleFeeEstimation.getEstimation(10)
            val high = oldStyleFeeEstimation.getEstimation(1)
            val result = FeeEstimationsGeneric(
                    Value.valueOf(coinType, lowPriority!!.longValue),
                    Value.valueOf(coinType, economy!!.longValue),
                    Value.valueOf(coinType, normal!!.longValue),
                    Value.valueOf(coinType, high!!.longValue),
                    System.currentTimeMillis()
            )
            //if all ok we return requested new fee estimation
            accountBacking.saveLastFeeEstimation(result, coinType)
            return result
        } catch (ex: WapiException) {
            //receiving data from the server failed then trying to read fee estimations from the DB
            //if a read error has occurred from the DB, then we return the predefined default fee
            return accountBacking.loadLastFeeEstimation(coinType) ?: defaultFeeEstimation
        }
    }


    @Synchronized
    override fun synchronize(mode: SyncMode?): Boolean {
        // retrieve history from colu server
        val txsInfo = coluClient.getAddressTransactions(receiveAddress)
        txsInfo?.let {
            accountBacking.clear()
            accountBacking.putTransactions(it.transactions)
            cachedBalance = calculateBalance(it.unspent, it.transactions)
            listener?.balanceUpdated(this)
        }
        return true
    }

    private fun calculateBalance(unspent: List<TransactionOutputEx>, transactions: List<GenericTransactionSummary>): Balance {
        var confirmed = Value.zeroValue(coinType)
        var receiving = Value.zeroValue(coinType)
        var sending = Value.zeroValue(coinType)
        var change = Value.zeroValue(coinType)

        for (tx in transactions) {
            var s = Value.zeroValue(coinType)
            var r = Value.zeroValue(coinType)
            tx.inputs.forEach {
                if (tx.confirmations == 0 && isMineAddress(it.address)) {
                    sending = sending.add(it.value)
                    s = s.add(it.value)
                }
            }
            tx.outputs.forEach {
                if (tx.confirmations == 0 && isMineAddress(it.address)) {
                    receiving = receiving.add(it.value)
                    r = r.add(it.value)
                }
            }
            if(s.add(r.negate()).isPositive) {
                change = change.add(r)
                receiving = receiving.add(r.negate())
            }
        }
        unspent.forEach {
            if (it.height != -1) {
                confirmed = confirmed.add(it.value)
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

    override fun isArchived() = context.isArchived()

    override fun isActive() = !context.isArchived()

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

    override fun isSynchronizing() = _isSynchronizing

    override fun broadcastOutgoingTransactions(): Boolean = false

    override fun getSyncTotalRetrievedTransactions(): Int {
        return 0
    }

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction? {
        val feePerKb = (fee as FeePerKbFee).feePerKb
        val coluTx = ColuTransaction(coinType, address as BtcAddress, amount!!, feePerKb)
        val fromAddresses = mutableListOf(receiveAddress as BtcAddress)
        val json = coluClient.prepareTransaction(coluTx.destination, fromAddresses, coluTx.amount, feePerKb)
        coluTx.baseTransaction = json
        return coluTx
    }

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        if (request is ColuTransaction) {
            val signTransaction = signTransaction(request.baseTransaction, this)
            request.transaction = signTransaction
        } else {
            TODO("signTx not implemented for ${request.javaClass.simpleName}")
        }
    }

    private fun signTransaction(txid: ColuBroadcastTxHex.Json?, coluAccount: ColuAccount?): Transaction? {
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

        val privateKeyBytes = coluAccount.getPrivateKey(null)!!.getPrivateKeyBytes()
        val publicKeyBytes = coluAccount.getPrivateKey(null)!!.publicKey.publicKeyBytes
        val ecKey = ECKey.fromPrivateAndPrecalculatedPublic(privateKeyBytes, publicKeyBytes)

        val inputScript = ScriptBuilder.createOutputScript(ecKey.toAddress(parameters))

        for (i in 0 until signTx.inputs.size) {
            val signature = signTx.calculateSignature(i, ecKey, inputScript, org.bitcoinj.core.Transaction.SigHash.ALL, false)
            val scriptSig = ScriptBuilder.createInputScript(signature, ecKey)
            signTx.getInput(i.toLong()).scriptSig = scriptSig
        }

        val signedTransactionBytes = signTx.bitcoinSerialize()
        val signedBitlibTransaction: Transaction
        try {
            signedBitlibTransaction = Transaction.fromBytes(signedTransactionBytes)
        } catch (e: Transaction.TransactionParsingException) {
//            Log.e(TAG, "signTx: Error parsing bitcoinj transaction ! msg: " + e.message)
            return null
        }

        return signedBitlibTransaction
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val coluTx = tx as ColuTransaction
        return if (coluTx.transaction != null && coluClient.broadcastTx(coluTx.transaction!!) != null) {
            BroadcastResult(BroadcastResultType.SUCCESS)
        } else {
            BroadcastResult(BroadcastResultType.REJECTED)
        }
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        // Colu transaction is a typical bitcoin transaction containing additional inputs and OP_RETURN data
        // Typical Colu transaction has 2 inputs and 4 outputs
        return FeeEstimatorBuilder().setLegacyInputs(2)
                .setLegacyOutputs(4)
                .createFeeEstimator()
                .estimateTransactionSize()
    }

    override fun getUnspentOutputViewModels(): List<GenericOutputViewModel> {
        val result = mutableListOf<GenericOutputViewModel>()
        accountBacking.unspentOutputs.forEach {
            result.add(GenericOutputViewModel(receiveAddress, Value.valueOf(coinType, it.value), false))
        }
        return result
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey? {
        return privateKey
    }
}