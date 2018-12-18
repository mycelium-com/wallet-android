package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


class CoinapultAccount(val context: CoinapultAccountContext, val accountKey: InMemoryPrivateKey
                       , val api: CoinapultApi
                       , val backing: AccountBacking<CoinapultTransaction>
                       , val _network: NetworkParameters
                       , val currency: Currency
                       , val listener: AccountListener?)
    : WalletAccount<CoinapultTransaction, BtcAddress> {
    override fun isExchangeable(): Boolean {
        return true
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        return accountKey
    }

    var accountLabel: String = ""

    override fun getTransactionsSince(receivingSince: Long): MutableList<CoinapultTransaction> {
        val history = ArrayList<CoinapultTransaction>()
        checkNotArchived()
        val list = backing.getTransactionsSince(receivingSince)
        for (tex in list) {
            val tx = getTx(tex.txid)
            history.add(tx)
        }
        return history
    }

    override fun getLabel(): String {
        return accountLabel
    }

    override fun setLabel(label: String?) {
        accountLabel = label.toString()
    }

    val uuid: UUID = CoinapultUtils.getGuidForAsset(currency, accountKey.publicKey.publicKeyBytes)
    protected var cachedBalance = Balance(Value.zeroValue(coinType), Value.zeroValue(coinType)
            , Value.zeroValue(coinType), Value.zeroValue(coinType))
    @Volatile
    protected var _isSynchronizing: Boolean = false

    var address: GenericAddress? = context.address

    override fun getId() = uuid

    override fun getReceiveAddress(): GenericAddress = address!!

    override fun isMineAddress(address: GenericAddress?): Boolean = receiveAddress == address

    override fun getTx(transactionId: Sha256Hash?): CoinapultTransaction {
        return backing.getTx(transactionId)
    }

    override fun getTransactions(offset: Int, limit: Int): List<CoinapultTransaction> {
        return backing.getTransactions(offset, limit)
    }

    override fun isActive() = !context.isArchived()

    override fun archiveAccount() {
        context.setArchived(true)
    }

    override fun broadcastOutgoingTransactions(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isArchived() = context.isArchived()

    protected fun checkNotArchived() {
        val usingArchivedAccount = "Using archived account"
        if (isArchived) {
            throw RuntimeException(usingArchivedAccount)
        }
    }

    override fun activateAccount() {
        context.setArchived(false)
    }

    override fun isDerivedFromInternalMasterseed(): Boolean = true

    override fun dropCachedData() {
    }

    override fun isSynchronizing(): Boolean = _isSynchronizing

    override fun getCoinType(): CryptoCurrency = currency

    override fun getBlockChainHeight(): Int = 0

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long): Value {
        return Value.zeroValue(if (_network.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSendToRequest(destination: BtcAddress, amount: Value): SendRequest<CoinapultTransaction> {
        return CoinapultSendRequest(currency, destination, amount)
    }

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        return FeeEstimationsGeneric(Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType))
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun canSpend(): Boolean = true

    override fun isVisible(): Boolean = true

    override fun completeAndSignTx(request: SendRequest<CoinapultTransaction>, keyCipher: KeyCipher) {
        completeTransaction(request)
        signTransaction(request, keyCipher)
    }

    override fun completeTransaction(request: SendRequest<CoinapultTransaction>) {
        if (request is CoinapultSendRequest) {
            request.tx = CoinapultTransaction(Sha256Hash.ZERO_HASH, request.amount, false
                    , 0, "", 0, request.destination)
            request.isCompleted = true
        } else {
            TODO("completeTransaction not implemented for ${request.javaClass.simpleName}")
        }
    }

    override fun signTransaction(request: SendRequest<CoinapultTransaction>, keyCipher: KeyCipher) {
        if (!request.isCompleted) {
            return
        }
        if (request is CoinapultSendRequest) {

        } else {
            TODO("signTransaction not implemented for ${request.javaClass.simpleName}")
        }
    }

    override fun broadcastTx(tx: CoinapultTransaction): BroadcastResult {
        return try {
            api.broadcast(tx.value.valueAsBigDecimal, tx.value.getType() as Currency, tx.address!!)
            BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: Exception) {
            BroadcastResult(BroadcastResultType.REJECTED)
        }
    }

    override fun getAccountBalance(): Balance = cachedBalance

    override fun checkAmount(receiver: WalletAccount.Receiver, kbMinerFee: Long, enteredAmount: Value?) {
        if (cachedBalance.confirmed.isLessThan(enteredAmount)) {
            throw StandardTransactionBuilder.InsufficientFundsException(receiver.amount, kbMinerFee)
        }
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        _isSynchronizing = true
        try {
            val newAddress = api.getAddress(currency, address)
            newAddress?.let {
                address = it
                context.address = it
            }
        } catch (e: Exception) {
        }
        val balance = api.getBalance(currency)
        if (balance != null && balance != cachedBalance) {
            cachedBalance = balance
            listener?.balanceUpdated(this)
        }
        val transactions = api.getTransactions(currency)
        backing.putTransactions(transactions)
//        transactions?.forEach {
//            if (it.state == "processing" || it.completeTime * 1000 > oneMinuteAgo) {
//                if (!it.incoming) {
//                    sendingFiatNotIncludedInBalance = sendingFiatNotIncludedInBalance.add(json.`in`.expected)
//                } else {
//                    receivingFiat = receivingFiat.add(json.out.amount)
//                }
//            } else if (it.state == "confirming") {
//                if (!it.incoming) {
//                    sendingFiatNotIncludedInBalance = sendingFiatNotIncludedInBalance.add(json.`in`.expected)
//                } else {
//                    receivingFiatNotIncludedInBalance = receivingFiatNotIncludedInBalance.add(json.out.expected)
//                }
//            }
//        }
        _isSynchronizing = false
        return true
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        return 0
    }

    override fun getUnspentOutputs(): MutableList<GenericTransaction.GenericOutput> {
        return mutableListOf()
    }
}