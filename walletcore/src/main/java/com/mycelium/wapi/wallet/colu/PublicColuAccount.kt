package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import java.util.*


open class PublicColuAccount(val context: ColuAccountContext
                             , private val type: CryptoCurrency
                             , val networkParameters: NetworkParameters
                             , val coluClient: ColuApi
                             , val accountBacking: ColuAccountBacking
                             , val backing: WalletBacking<ColuAccountContext>
                             , val listener: AccountListener? = null) : WalletAccount<BtcAddress> {

    override fun getTransactions(offset: Int, limit: Int): MutableList<GenericTransaction> {
        return ArrayList<GenericTransaction>()
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        return false
    }

    override fun getTx(transactionId: Sha256Hash?): GenericTransaction {
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
        if (context.publicKey != null) {
            addressList = convert(context.publicKey, type as ColuMain)
        } else {
            addressList = context.address ?: mapOf()
        }
        uuid = ColuUtils.getGuidForAsset(type, addressList[AddressType.P2PKH]?.getBytes())
        cachedBalance = calculateBalance(emptyList(), accountBacking.getTransactionSummaries(0, 2000))
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummaryGeneric> {
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

    override fun getTxSummary(transactionId: Sha256Hash): TransactionSummaryGeneric? {
        checkNotArchived()
        return accountBacking.getTxSummary(transactionId)
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): List<TransactionSummaryGeneric> {
        return accountBacking.getTransactionSummaries(offset, limit)
    }

    override fun getBlockChainHeight(): Int = context.blockHeight

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long, destinationAddres: BtcAddress): Value {
        return Value.zeroValue(if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        return defaultFeeEstimation
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

    private fun calculateBalance(unspent: List<TransactionOutputEx>, transactions: List<TransactionSummaryGeneric>): Balance {
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
                    s.add(it.value)
                }
            }
            tx.outputs.forEach {
                if (tx.confirmations == 0 && isMineAddress(it.address)) {
                    receiving = receiving.add(it.value)
                    r.add(it.value)
                }
            }
            val c = s.add(r.negate())
            if(!c.isZero) {
                change.add(c)
                receiving = receiving.add(c.negate())
                sending = sending.add(c.negate())
            }
        }
        unspent.forEach {
            confirmed = confirmed.add(it.value)
        }
        return Balance(confirmed, receiving, sending, Value.zeroValue(coinType))
    }


    override fun canSpend(): Boolean = false

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
        return 0;
    }

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction? {
        return null
    }

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        // This implementation is empty since this account is read only and cannot create,
        // sign and broadcast transactions
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        // This implementation is empty since this account is read only and cannot create,
        // sign and broadcast transactions
        return BroadcastResult(BroadcastResultType.REJECTED)
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
        // This is not spendable account so it does not contain private key
        return null
    }
}