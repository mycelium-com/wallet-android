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
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.GetFIONamesResponse
import org.web3j.crypto.Credentials
import java.math.BigInteger
import java.util.*

class FioAccount(val fioKeyManager: FioKeyManager, val fiosdk: FIOSDK, val credentials: Credentials) : WalletAccount<FioAddress> {
    private var balance: Balance = Balance(Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain))


    //TODO
    val maxFee = BigInteger.ZERO

    fun registerFIOAddress(fioAddress: String) {
        fiosdk.registerFioAddress(fioAddress, maxFee)
    }

    fun registerFioDomain(fioDomain: String) {
        fiosdk.registerFioDomain(fioDomain, maxFee)
    }

    fun getFioNames(): GetFIONamesResponse {
        return fiosdk.getFioNames()
    }



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
        return balance
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
        return mutableListOf()
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> {
        return mutableListOf()
    }

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        return mutableListOf()
    }

    override fun getLabel(): String {
        return "FIO"
    }

    override fun setLabel(label: String?) {
        "FIO label"
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean {
        return true
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val fioBalance = fiosdk.getFioBalance()
        balance = Balance(Value.valueOf(FIOMain, fioBalance.balance), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain))
        return true
    }

    override fun getBlockChainHeight(): Int {
        TODO("Not yet implemented")
    }

    override fun canSpend(): Boolean {
        return true
    }

    override fun canSign(): Boolean {
        return false
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

    }

    override fun dropCachedData() {

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
        return true
    }

    override fun removeAllQueuedTransactions() {

    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: FioAddress?): Value {
        TODO("Not yet implemented")
    }

    override fun getSyncTotalRetrievedTransactions(): Int {
        return 0
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        return 0
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
        return mutableListOf()
    }

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

}