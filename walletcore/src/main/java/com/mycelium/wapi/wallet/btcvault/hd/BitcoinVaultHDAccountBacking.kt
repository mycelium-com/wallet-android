package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.btc.BtcAccountBacking
import java.util.*


class BitcoinVaultHDAccountBacking(private val walletDB: WalletDB,
                                   private val uuid: UUID) : BtcAccountBacking {

    private val contextQueries = walletDB.accountContextQueries
    private val btcvContextQueries = walletDB.bTCVContextQueries
    private val txQueries = walletDB.bTCVTransactionQueries
    private val utxoQueries = walletDB.bTCVUtxoQueries
    private val ptxoQueries = walletDB.bTCVPtxoQueries
    private val refersPtxoQueries = walletDB.bTCVRefersPtxoQueries
    private val outTxoQueries = walletDB.bTCVOutgoingTxQueries

    fun updateAccountContext(context: BitcoinVaultHDAccountContext) {
        walletDB.transaction {
            contextQueries.update(context.accountName, context.balance, context.archived, context.blockHeight, context.id)
            btcvContextQueries.update(context.indexesMap, context.getLastDiscovery(),
                    context.accountType, context.accountSubId, context.defaultAddressType,
                    context.id)
        }
    }

    override fun beginTransaction() {
    }

    override fun setTransactionSuccessful() {

    }

    override fun endTransaction() {
    }

    override fun clear() {
        utxoQueries.deleteUtxos(uuid)
        ptxoQueries.deletePtxos(uuid)
        txQueries.deleteTransactions(uuid)
        outTxoQueries.deleteAll(uuid)
        refersPtxoQueries.deleteAll(uuid)
    }

    override fun getAllUnspentOutputs(): Collection<TransactionOutputEx> =
            utxoQueries.selectUtxos(uuid, mapper = { outPoint: OutPoint?,
                                                     accountId: UUID?,
                                                     height: Int,
                                                     value: Long,
                                                     isCoinbase: Boolean,
                                                     script: ByteArray? ->
                TransactionOutputEx(outPoint, height, value, script, isCoinbase)
            }).executeAsList()


    override fun getUnspentOutput(outPoint: OutPoint?): TransactionOutputEx? =
            utxoQueries.selectUtxoById(outPoint, uuid, mapper = { _: OutPoint?,
                                                                  _: UUID?,
                                                                  height: Int,
                                                                  value: Long,
                                                                  isCoinbase: Boolean,
                                                                  script: ByteArray? ->
                TransactionOutputEx(outPoint, height, value, script, isCoinbase)
            }).executeAsOneOrNull()

    override fun deleteUnspentOutput(outPoint: OutPoint?) {
        utxoQueries.deleteUtxo(outPoint, uuid)
    }

    override fun putUnspentOutput(output: TransactionOutputEx) {
        utxoQueries.insertUtxo(output.outPoint, uuid, output.height, output.value, output.isCoinBase, output.script)
    }

    override fun putParentTransactionOuputs(outputsList: MutableList<TransactionOutputEx>?) {
        ptxoQueries.transaction {
            outputsList?.forEach {
                putParentTransactionOutput(it)
            }
        }
    }

    override fun putParentTransactionOutput(output: TransactionOutputEx) {
        ptxoQueries.insertPtxo(output.outPoint, uuid, output.height, output.value, output.isCoinBase, output.script)
    }

    override fun getParentTransactionOutput(outPoint: OutPoint?): TransactionOutputEx? =
            ptxoQueries.selectPtxoById(outPoint, uuid, mapper = { outpoint: OutPoint?,
                                                                  accountId: UUID?,
                                                                  height: Int,
                                                                  value: Long,
                                                                  isCoinbase: Boolean,
                                                                  script: ByteArray? ->
                TransactionOutputEx(outPoint, height, value, script, isCoinbase)
            }).executeAsOneOrNull()

    override fun hasParentTransactionOutput(outPoint: OutPoint?): Boolean =
            ptxoQueries.selectPtxoById(outPoint, uuid).executeAsOneOrNull() != null


    override fun putTransaction(transaction: TransactionEx) {
        txQueries.insertTransaction(transaction.txid, transaction.hash, uuid, transaction.height, transaction.time, transaction.binary)
    }

    override fun putTransactions(transactions: Collection<out TransactionEx>?) {
        txQueries.transaction {
            transactions?.forEach {
                putTransaction(it)
            }
        }
    }

    override fun getTransaction(hash: Sha256Hash): TransactionEx? =
            txQueries.selectBTCVTransactionById(hash, uuid, mapper = ::TransactionEx).executeAsOneOrNull()

    override fun deleteTransaction(hash: Sha256Hash) {
        val tex = getTransaction(hash)
        val tx = TransactionEx.toTransaction(tex)
        walletDB.transaction {
            // See if any of the outputs are stored locally and remove them
            for (i in tx.outputs.indices) {
                val outPoint = OutPoint(tx.id, i)
                getUnspentOutput(outPoint)?.let {
                    deleteUnspentOutput(outPoint)
                }
            }
            // remove it from the accountBacking
            txQueries.deleteTransaction(hash, uuid)
        }
    }

    override fun getTransactionHistory(offset: Int, limit: Int): List<TransactionEx> =
            txQueries.selectBTCVTransactions(uuid, limit.toLong(), offset.toLong(), mapper = ::TransactionEx).executeAsList()

    override fun getTransactionsSince(since: Long): List<TransactionEx> =
            txQueries.selectBTCVTransactionsSince(uuid, since.toInt(), mapper = ::TransactionEx).executeAsList()

    override fun getUnconfirmedTransactions(): Collection<TransactionEx> =
            txQueries.selectBTCVUnconfirmedTransactions(uuid, mapper = ::TransactionEx).executeAsList()

    override fun getYoungTransactions(maxConfirmations: Int, blockChainHeight: Int): Collection<TransactionEx> {
        val minHeight = blockChainHeight - maxConfirmations + 1
        return txQueries.selectBTCVYoungTransactions(uuid, minHeight, mapper = ::TransactionEx).executeAsList()
    }

    override fun hasTransaction(txid: Sha256Hash?): Boolean =
            txQueries.selectBTCVTransactionById(txid, uuid).executeAsOneOrNull() != null

    override fun putOutgoingTransaction(txid: Sha256Hash, rawTransaction: ByteArray?) {
        outTxoQueries.insertTransaction(txid, uuid, rawTransaction)
    }

    override fun getOutgoingTransactions(): Map<Sha256Hash, ByteArray?> =
            outTxoQueries.selectBTCVOutgoingTxAll(uuid, mapper = { id: Sha256Hash, raw: ByteArray? ->
                id to raw
            }).executeAsList().toMap()

    override fun isOutgoingTransaction(txid: Sha256Hash): Boolean =
            outTxoQueries.selectBTCVOutgoingTxById(uuid, txid).executeAsOneOrNull() != null

    override fun removeOutgoingTransaction(txid: Sha256Hash) {
        outTxoQueries.delete(txid, uuid)
    }

    override fun deleteTxRefersParentTransaction(txId: Sha256Hash) {
        refersPtxoQueries.delete(txId, uuid)
    }

    override fun getTransactionsReferencingOutPoint(outPoint: OutPoint?): Collection<Sha256Hash> =
            refersPtxoQueries.selectRefersPtxo(outPoint, uuid, mapper = { txid: Sha256Hash?,
                                                                          accountId: UUID?,
                                                                          input: OutPoint? ->
                txid!!
            }).executeAsList()

    override fun putTxRefersParentTransaction(txId: Sha256Hash?, refersOutputs: List<OutPoint>) {
        refersOutputs.forEach {
            refersPtxoQueries.insert(txId, uuid, it)
        }
    }
}
