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
                             , val accountBacking: AccountBacking<ColuTransaction>
                             , val backing: WalletBacking<ColuAccountContext, ColuTransaction>
                             , val listener: AccountListener? = null) : WalletAccount<ColuTransaction, BtcAddress> {
    override fun getDefaultFeeEstimation(): FeeEstimationsGeneric {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        cachedBalance = calculateBalance(accountBacking.allUnspentOutputs.toList(), accountBacking.getTransactions(0, 2000))
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<ColuTransaction> {
        val history = ArrayList<ColuTransaction>()
        checkNotArchived()
        val list = accountBacking.getTransactionsSince(receivingSince)
        for (tex in list) {
            val tx = getTx(tex.txid)
            if (tx != null) {
                history.add(tx)
            }
        }
        return history
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

    override fun getTx(transactionId: Sha256Hash): ColuTransaction? {
        checkNotArchived()
        return accountBacking.getTx(transactionId)
    }

    override fun getTransactions(offset: Int, limit: Int): List<ColuTransaction> {
        return accountBacking.getTransactions(offset, limit)
    }

    override fun getBlockChainHeight(): Int = context.blockHeight

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long, destinationAddres: BtcAddress): Value {
        return Value.zeroValue(if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    override fun getSendToRequest(destination: BtcAddress, amount: Value, feePerKb: Value): SendRequest<ColuTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        return FeeEstimationsGeneric(Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType), System.currentTimeMillis())
    }


    @Synchronized
    override fun synchronize(mode: SyncMode?): Boolean {
        // retrieve history from colu server
        val txsInfo = coluClient.getAddressTransactions(receiveAddress)
        txsInfo?.let {
            accountBacking.clear()
            accountBacking.putTransactions(it.transactions)
            it.unspent.forEach { txOut ->
                accountBacking.putUnspentOutput(txOut)
            }
            cachedBalance = calculateBalance(it.unspent, it.transactions)
            listener?.balanceUpdated(this)
        }
        return true
    }

    private fun calculateBalance(unspent: List<TransactionOutputEx>, transactions: List<ColuTransaction>): Balance {
        var confirmed = Value.zeroValue(coinType)
        var receiving = Value.zeroValue(coinType)
        var sending = Value.zeroValue(coinType)

        for (tx in transactions) {
            tx.inputs.forEach {
                if (tx.confirmations < 6 && isMineAddress(it.address)) {
                    sending = sending.add(it.value)
                }
            }
            tx.outputs.forEach {
                if (tx.confirmations < 6 && isMineAddress(it.address)) {
                    receiving = receiving.add(it.value)
                }
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

    override fun completeTransaction(request: SendRequest<ColuTransaction>) {
        // This implementation is empty since this account is read only and cannot create,
        // sign and broadcast transactions
    }

    override fun signTransaction(request: SendRequest<ColuTransaction>, keyCipher: KeyCipher) {
        // This implementation is empty since this account is read only and cannot create,
        // sign and broadcast transactions
    }

    override fun broadcastTx(tx: ColuTransaction): BroadcastResult {
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

    override fun getUnspentOutputs(): List<GenericTransaction.GenericOutput> {
        val result = mutableListOf<GenericTransaction.GenericOutput>()
        accountBacking.allUnspentOutputs.forEach {
            result.add(GenericTransaction.GenericOutput(receiveAddress, Value.valueOf(coinType, it.value)))
        }
        return result
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey? {
        // This is not spendable account so it does not contain private key
        return null
    }
}