package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.toUUID
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import fiofoundation.io.fiosdk.FIOSDK
import org.web3j.crypto.Credentials
import java.util.*

class FioAccount(val fioKeyManager: FioKeyManager, val fiosdk: FIOSDK, val credentials: Credentials) : WalletAccount<FioAddress> {
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

    override fun getReceiveAddress(): Address {
        val formatPubKey = fioKeyManager.formatPubKey(PublicKey(fiosdk.publicKey.toByteArray()))
        return FioAddress(FIOMain, FioAddressData(formatPubKey))
    }

    override fun getCoinType(): CryptoCurrency {
        return FIOMain
    }

    override fun getBasedOnCoinType(): CryptoCurrency {
        return FIOMain
    }

    override fun getAccountBalance(): Balance {
        val fioBalance = fiosdk.getFioBalance()
        return Balance(Value.valueOf(FIOMain, fioBalance.balance), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain))
    }

    override fun isMineAddress(address: Address?): Boolean = address == receiveAddress


    override fun isExchangeable(): Boolean {
        return true
    }

    override fun getTx(transactionId: ByteArray?): Transaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): TransactionSummary {
        TODO("Not yet implemented")
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<TransactionSummary> {
        TODO("Not yet implemented")
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> {
        TODO("Not yet implemented")
    }

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        TODO("Not yet implemented")
    }

    override fun getLabel(): String {
        TODO("Not yet implemented")
    }

    override fun setLabel(label: String?) {
        TODO("Not yet implemented")
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean {
        TODO("Not yet implemented")
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBlockChainHeight(): Int {
        TODO("Not yet implemented")
    }

    override fun canSpend(): Boolean {
        TODO("Not yet implemented")
    }

    override fun canSign(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSyncing(): Boolean {
        return false
    }

    override fun isArchived(): Boolean {
        return false
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun archiveAccount() {
        TODO("Not yet implemented")
    }

    override fun activateAccount() {
        TODO("Not yet implemented")
    }

    override fun dropCachedData() {
        TODO("Not yet implemented")
    }

    override fun isVisible(): Boolean {
        return true
    }

    override fun isDerivedFromInternalMasterseed(): Boolean {
        return true
    }

    override fun getId(): UUID = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receiveAddress.getBytes())


    override fun broadcastOutgoingTransactions(): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAllQueuedTransactions() {
        TODO("Not yet implemented")
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: FioAddress?): Value {
        TODO("Not yet implemented")
    }

    override fun getSyncTotalRetrievedTransactions(): Int {
        TODO("Not yet implemented")
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        TODO("Not yet implemented")
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): FioAddress {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(subType: String?): FioAddress {
        TODO("Not yet implemented")
    }

    override fun getDependentAccounts(): MutableList<WalletAccount<Address>> {
        TODO("Not yet implemented")
    }

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

}