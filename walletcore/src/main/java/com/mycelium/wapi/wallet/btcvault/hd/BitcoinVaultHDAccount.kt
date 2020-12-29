package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


class BitcoinVaultHDAccount(protected var accountContext: BitcoinVaultHDAccountContext,
                            network: NetworkParameters,
                            private val accountListener: AccountListener?) : WalletAccount<BtcvAddress> {
    var accountName = ""

    @Volatile
    protected var syncing = false

    val accountIndex: Int
        get() = accountContext.accountIndex

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: Address?, amount: Value?, fee: Fee?, data: TransactionData?): Transaction {
        TODO("Not yet implemented")
    }

    override fun signTx(request: Transaction?, keyCipher: KeyCipher?) {
        TODO("Not yet implemented")
    }

    override fun broadcastTx(tx: Transaction?): BroadcastResult {
        TODO("Not yet implemented")
    }

    override fun getReceiveAddress(): Address = BtcvAddress(coinType, "RNpmnbN2ystNC9y4H5v1Gk5yi7VVWLHccR")


    override fun getCoinType(): CryptoCurrency = accountContext.currency

    override fun getBasedOnCoinType(): CryptoCurrency = accountContext.currency

    override fun getAccountBalance(): Balance = Balance.getZeroBalance(accountContext.currency)

    override fun isMineAddress(address: Address?): Boolean = false

    override fun isExchangeable(): Boolean = true

    override fun getTx(transactionId: ByteArray?): Transaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): TransactionSummary {
        TODO("Not yet implemented")
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<TransactionSummary> = mutableListOf()

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> = mutableListOf()

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> = mutableListOf()

    override fun getLabel(): String = accountName

    override fun setLabel(label: String) {
        accountName = label
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean {
        TODO("Not yet implemented")
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        syncing = true
        //TODO  need implementation
        syncing = false
        return true
    }

    override fun getBlockChainHeight(): Int = 100

    override fun canSpend(): Boolean = true

    override fun canSign(): Boolean = true

    override fun isSyncing(): Boolean = syncing

    override fun isArchived(): Boolean = accountContext.isArchived()


    override fun isActive(): Boolean = !accountContext.isArchived()

    override fun archiveAccount() {
        accountContext.setArchived(true)
    }

    override fun activateAccount() {
        accountContext.setArchived(false)
    }

    override fun dropCachedData() {
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = true

    override fun getId(): UUID = accountContext.id

    override fun broadcastOutgoingTransactions(): Boolean = false

    override fun removeAllQueuedTransactions() {
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: BtcvAddress?): Value {
        TODO("Not yet implemented")
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun getTypicalEstimatedTransactionSize(): Int = 0

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): BtcvAddress {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(subType: String?): BtcvAddress {
        TODO("Not yet implemented")
    }

    override fun getDependentAccounts(): MutableList<WalletAccount<Address>> = mutableListOf()

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }
}