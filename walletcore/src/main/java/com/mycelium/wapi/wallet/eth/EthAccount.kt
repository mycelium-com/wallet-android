package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthTest
import org.web3j.crypto.Credentials
import java.util.*

class EthAccount(val credentials: Credentials) : WalletAccount<EthAddress> {
    private val coinType = EthTest

    override fun getDefaultFeeEstimation(): FeeEstimationsGeneric {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createTx(addres: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress() = EthAddress(coinType, credentials.address)


    override fun getCoinType() = coinType

    override fun getBasedOnCoinType(): CryptoCurrency {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val ethBalanceService = EthBalanceService(credentials.address)

    override fun getAccountBalance(): Balance {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return Balance(Value(EthTest, ethBalanceService.balance.toLong()), Value(EthTest, 0), Value(EthTest, 0), Value(EthTest, 0))
    }

    override fun isMineAddress(address: GenericAddress) =
            address == EthAddress(coinType, "0x60c2A43Cc69658eC4b02a65A07623D7192166F4e")

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummaries(offset: Int, limit: Int) = emptyList<GenericTransactionSummary>()

    override fun getTransactionsSince(receivingSince: Long) = emptyList<GenericTransactionSummary>()

    override fun getTransactions(offset: Int, limit: Int) = emptyList<GenericTransaction>()

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel() = "Some account"

    override fun setLabel(label: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        ethBalanceService.updateBalanceCache()
        return true
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = true

    override fun isSyncing() = false

    override fun isArchived() = false

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun activateAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dropCachedData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override fun getId() = UUID(BitUtils.uint64ToLong(credentials.ecKeyPair.publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
            credentials.ecKeyPair.publicKey.toByteArray(), 16))

    override fun isSynchronizing(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastOutgoingTransactions() = false

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Long, destinationAddress: EthAddress?): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSyncTotalRetrievedTransactions() = 0

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(): EthAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(subType: String?): EthAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}