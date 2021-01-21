package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btc.BtcAccountBacking
import com.mycelium.wapi.wallet.btcvault.BTCVTransactionSummary
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


class BitcoinVaultHDAccountBacking(val walletDB: WalletDB,
                                   private val uuid: UUID,
                                   private val currency: CryptoCurrency) : BtcAccountBacking {

    private val contextQueries = walletDB.accountContextQueries
    private val btcvContextQueries = walletDB.bTCVContextQueries
    private val accountBackingQueries = walletDB.bTCVAccountBackingQueries
    private val txQueries = walletDB.bTCVTransactionQueries
    private val utxoQueries = walletDB.bTCVUtxoQueries
    private val ptxoQueries = walletDB.bTCVPtxoQueries

    fun getTransactionSummaries(offset: Long, limit: Long): List<TransactionSummary> =
            accountBackingQueries.selectBTCVTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                                 currency: CryptoCurrency,
                                                                                                 blockNumber: Int,
                                                                                                 timestamp: Long,
                                                                                                 value: Value,
                                                                                                 fee: Value,
                                                                                                 confirmations: Int,
                                                                                                 sender: String,
                                                                                                 receiver: String ->
                val fromAddress = BtcvAddress(currency, BitcoinAddress.fromString(sender).allAddressBytes)
                val toAddress = BtcvAddress(currency, BitcoinAddress.fromString(receiver).allAddressBytes)
                BTCVTransactionSummary(
                        currency, HexUtils.toBytes(txid), HexUtils.toBytes(txid), null, timestamp, blockNumber,
                        confirmations, false,
                        listOf(InputViewModel(fromAddress, value, false)),
                        listOf(OutputViewModel(toAddress, value, false)), null,
                        null, 0, fee)
            }).executeAsList()

    fun getTransactionSummary(txidParameter: String): TransactionSummary? {
        return null
    }

    fun updateAccountContext(context: BitcoinVaultHDAccountContext) {
        contextQueries.update(context.accountName, context.balance, context.isArchived(), context.blockHeight, context.id)
//        _updateBip44Account.bindString(3, gson.toJson(context.getIndexesMap()));
//        _updateBip44Account.bindLong(4, context.getLastDiscovery());
//        _updateBip44Account.bindLong(5, context.getAccountType());
//        _updateBip44Account.bindLong(6, context.getAccountSubId());
//        _updateBip44Account.bindString(7, gson.toJson(context.getDefaultAddressType()));
    }

    override fun beginTransaction() {
    }

    override fun setTransactionSuccessful() {

    }

    override fun endTransaction() {
    }

    override fun clear() {
        TODO("Not yet implemented")
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
            utxoQueries.selectUtxoById(outPoint, uuid, mapper = { outpoint: OutPoint?,
                                                                  accountId: UUID?,
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

    override fun putTransactions(transactions: MutableCollection<out TransactionEx>?) {
        txQueries.transaction {
            transactions?.forEach {
                putTransaction(it)
            }
        }
    }

    override fun getTransaction(hash: Sha256Hash): TransactionEx? =
            txQueries.selectBTCVTransactionById(hash, uuid, mapper = { id: Sha256Hash?,
                                                                       hash: Sha256Hash,
                                                                       blockNumber: Int,
                                                                       timestamp: Int,
                                                                       binary: ByteArray ->
                TransactionEx(id, hash, blockNumber, timestamp, binary)
            }).executeAsOneOrNull()

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
            txQueries.selectBTCVTransactions(uuid, limit.toLong(), offset.toLong(), mapper = { id: Sha256Hash?,
                                                                                               hash: Sha256Hash,
                                                                                               blockNumber: Int,
                                                                                               timestamp: Int,
                                                                                               binary: ByteArray ->
                TransactionEx(id, hash, blockNumber, timestamp, binary)
            }).executeAsList()

    override fun getTransactionsSince(since: Long): MutableList<TransactionEx> {
        TODO("Not yet implemented")
//        txQueries.selectBTCVTransactionsSince()
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

    override fun getOutgoingTransactions(): Map<Sha256Hash, ByteArray> =
            mapOf()

    override fun isOutgoingTransaction(txid: Sha256Hash?): Boolean = false

    override fun removeOutgoingTransaction(txid: Sha256Hash?) {
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