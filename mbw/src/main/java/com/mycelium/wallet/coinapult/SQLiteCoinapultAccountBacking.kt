package com.mycelium.wallet.coinapult

import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.AccountBacking
import com.mycelium.wapi.wallet.colu.ColuTransaction


class SQLiteCoinapultAccountBacking : AccountBacking<ColuTransaction> {
    override fun beginTransaction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTransactionSuccessful() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun endTransaction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllUnspentOutputs(): MutableCollection<TransactionOutputEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnspentOutput(outPoint: OutPoint?): TransactionOutputEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteUnspentOutput(outPoint: OutPoint?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putUnspentOutput(output: TransactionOutputEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putParentTransactionOuputs(outputsList: MutableList<TransactionOutputEx>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putParentTransactionOutput(output: TransactionOutputEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParentTransactionOutput(outPoint: OutPoint?): TransactionOutputEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasParentTransactionOutput(outPoint: OutPoint?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTransaction(transaction: TransactionEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTransactions(transactions: MutableCollection<out TransactionEx>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransaction(hash: Sha256Hash?): TransactionEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteTransaction(hash: Sha256Hash?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionHistory(offset: Int, limit: Int): MutableList<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionsSince(since: Long): MutableList<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnconfirmedTransactions(): MutableCollection<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getYoungTransactions(maxConfirmations: Int, blockChainHeight: Int): MutableCollection<TransactionEx> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasTransaction(txid: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putOutgoingTransaction(txid: Sha256Hash?, rawTransaction: ByteArray?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutgoingTransactions(): MutableMap<Sha256Hash, ByteArray> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOutgoingTransaction(txid: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeOutgoingTransaction(txid: Sha256Hash?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteTxRefersParentTransaction(txId: Sha256Hash?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionsReferencingOutPoint(outPoint: OutPoint?): MutableCollection<Sha256Hash> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTxRefersParentTransaction(txId: Sha256Hash?, refersOutputs: MutableList<OutPoint>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTx(hash: Sha256Hash?): ColuTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactions(offset: Int, limit: Int): MutableList<ColuTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putTransactions(txList: MutableList<ColuTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}