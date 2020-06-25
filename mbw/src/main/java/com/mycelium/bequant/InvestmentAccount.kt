package com.mycelium.bequant

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


class InvestmentAccount : WalletAccount<BtcAddress> {
    private val id = UUID.randomUUID()

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?, data: GenericTransactionData?): GenericTransaction {
        TODO("Not yet implemented")
    }

    override fun canSign(): Boolean {
        TODO("Not yet implemented")
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        TODO("Not yet implemented")
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        TODO("Not yet implemented")
    }

    override fun getReceiveAddress(): GenericAddress = Utils.getBtcCoinType().parseAddress("2MvQpPibRGyNeV3jraAQnTZGHN23P5vRbhL")

    override fun getCoinType(): CryptoCurrency = Utils.getBtcCoinType()

    override fun getBasedOnCoinType(): CryptoCurrency = Utils.getBtcCoinType()

    override fun getAccountBalance(): Balance = Balance.getZeroBalance(Utils.getBtcCoinType())

    override fun isMineAddress(address: GenericAddress?): Boolean = false

    override fun isExchangeable(): Boolean = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("Not yet implemented")
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<GenericTransactionSummary> {
        TODO("Not yet implemented")
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> {
        TODO("Not yet implemented")
    }

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("Not yet implemented")
    }

    override fun getLabel(): String = "Investment Account"

    override fun setLabel(label: String?) {
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("Not yet implemented")
    }

    override fun synchronize(mode: SyncMode?): Boolean = true

    override fun getBlockChainHeight(): Int  = 0

    override fun canSpend(): Boolean = true

    override fun isSyncing(): Boolean = false

    override fun isArchived(): Boolean = false

    override fun isActive(): Boolean = true

    override fun archiveAccount() {
    }

    override fun activateAccount() {
    }

    override fun dropCachedData() {
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = false

    override fun getId(): UUID = id

    override fun broadcastOutgoingTransactions(): Boolean = false

    override fun removeAllQueuedTransactions() {
        TODO("Not yet implemented")
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: BtcAddress?): Value {
        TODO("Not yet implemented")
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun getTypicalEstimatedTransactionSize(): Int {
        TODO("Not yet implemented")
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): BtcAddress {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(subType: String?): BtcAddress {
        TODO("Not yet implemented")
    }

    override fun getDependentAccounts(): MutableList<WalletAccount<GenericAddress>> {
        TODO("Not yet implemented")
    }

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("Not yet implemented")
    }
}