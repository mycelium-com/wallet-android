package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


class CoinapultAccount(val context: CoinapultAccountContext, val accountKey: InMemoryPrivateKey
                       , val coinapultApi: CoinapultApi
                       , val backing: AccountBacking<CoinapultTransaction>
                       , val _network: NetworkParameters
                       , val coinapultCurrency: Currency)
    : WalletAccount<CoinapultTransaction, BtcAddress> {


    val uuid: UUID = CoinapultUtils.getGuidForAsset(coinapultCurrency, accountKey.publicKey.publicKeyBytes)
    protected var cachedBalance = Balance(Value.zeroValue(coinType), Value.zeroValue(coinType)
            , Value.zeroValue(coinType), Value.zeroValue(coinType))
    @Volatile
    protected var _isSynchronizing: Boolean = false

    val address: GenericAddress? = null

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

    override fun activateAccount() {
        context.setArchived(false)
    }

    override fun isDerivedFromInternalMasterseed(): Boolean = true

    override fun dropCachedData() {
    }

    override fun isSynchronizing(): Boolean = _isSynchronizing

    override fun getCoinType(): CryptoCurrency {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBlockChainHeight(): Int = 0

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSendToRequest(destination: GenericAddress?, amount: Value?): SendRequest<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun canSpend(): Boolean = true

    override fun isVisible(): Boolean = true

    override fun completeAndSignTx(request: SendRequest<CoinapultTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeTransaction(request: SendRequest<CoinapultTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTransaction(request: SendRequest<CoinapultTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: CoinapultTransaction?): BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAccountBalance(): Balance = cachedBalance

    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val transactions = coinapultApi.getTransactions()
        transactions.forEach {
        }
        return true
    }
}