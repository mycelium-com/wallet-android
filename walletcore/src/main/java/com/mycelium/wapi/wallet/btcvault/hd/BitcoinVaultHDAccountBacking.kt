package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btc.BtcAccountBacking
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*


class BitcoinVaultHDAccountBacking(walletDB: WalletDB,
                                   private val uuid: UUID,
                                   private val currency: CryptoCurrency) : BtcAccountBacking {

    fun getTransactionSummaries(offset: Long, limit: Long): List<TransactionSummary> {
        return emptyList()
    }

    fun getTransactionSummary(txidParameter: String): TransactionSummary? {
        return null
    }

    fun updateAccountContext(context: BitcoinVaultHDAccountContext) {

    }

    override fun beginTransaction() {
        TODO("Not yet implemented")
    }

    override fun setTransactionSuccessful() {
        TODO("Not yet implemented")
    }

    override fun endTransaction() {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun getAllUnspentOutputs(): Collection<TransactionOutputEx> =
            listOf()


    override fun getUnspentOutput(outPoint: OutPoint?): TransactionOutputEx {
        TODO("Not yet implemented")
    }

    override fun deleteUnspentOutput(outPoint: OutPoint?) {
        TODO("Not yet implemented")
    }

    override fun putUnspentOutput(output: TransactionOutputEx?) {
        TODO("Not yet implemented")
    }

    override fun putParentTransactionOuputs(outputsList: MutableList<TransactionOutputEx>?) {
        TODO("Not yet implemented")
    }

    override fun putParentTransactionOutput(output: TransactionOutputEx?) {
        TODO("Not yet implemented")
    }

    override fun getParentTransactionOutput(outPoint: OutPoint?): TransactionOutputEx {
        TODO("Not yet implemented")
    }

    override fun hasParentTransactionOutput(outPoint: OutPoint?): Boolean {
        TODO("Not yet implemented")
    }

    override fun putTransaction(transaction: TransactionEx?) {
        TODO("Not yet implemented")
    }

    override fun putTransactions(transactions: MutableCollection<out TransactionEx>?) {
        TODO("Not yet implemented")
    }

    override fun getTransaction(hash: Sha256Hash?): TransactionEx {
        TODO("Not yet implemented")
    }

    override fun deleteTransaction(hash: Sha256Hash?) {
        TODO("Not yet implemented")
    }

    override fun getTransactionHistory(offset: Int, limit: Int): MutableList<TransactionEx> {
        TODO("Not yet implemented")
    }

    override fun getTransactionsSince(since: Long): MutableList<TransactionEx> {
        TODO("Not yet implemented")
    }

    override fun getUnconfirmedTransactions(): Collection<TransactionEx> =
            listOf()


    override fun getYoungTransactions(maxConfirmations: Int, blockChainHeight: Int): Collection<TransactionEx> =
            listOf()

    override fun hasTransaction(txid: Sha256Hash?): Boolean {
        TODO("Not yet implemented")
    }

    override fun putOutgoingTransaction(txid: Sha256Hash?, rawTransaction: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun getOutgoingTransactions(): MutableMap<Sha256Hash, ByteArray> {
        TODO("Not yet implemented")
    }

    override fun isOutgoingTransaction(txid: Sha256Hash?): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeOutgoingTransaction(txid: Sha256Hash?) {
        TODO("Not yet implemented")
    }

    override fun deleteTxRefersParentTransaction(txId: Sha256Hash?) {
    }

    override fun getTransactionsReferencingOutPoint(outPoint: OutPoint?): MutableCollection<Sha256Hash> {
        TODO("Not yet implemented")
    }

    override fun putTxRefersParentTransaction(txId: Sha256Hash?, refersOutputs: MutableList<OutPoint>?) {
        TODO("Not yet implemented")
    }
}