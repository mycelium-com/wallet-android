package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.tx.Transfer
import java.util.*

class FioAccountBacking(walletDB: WalletDB, private val uuid: UUID, private val currency: CryptoCurrency) {
    private val fioQueries = walletDB.fioAccountBackingQueries
    private val queries = walletDB.accountBackingQueries

    fun getTransactionSummaries(offset: Long, limit: Long, ownerAddress: String): List<TransactionSummary> =
            fioQueries.selectFioTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                     currency: CryptoCurrency,
                                                                                     blockNumber: Int,
                                                                                     timestamp: Long,
                                                                                     value: Value,
                                                                                     fee: Value,
                                                                                     confirmations: Int,
                                                                                     from: String,
                                                                                     to: String,
                                                                                     memo: String? ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, memo)
            }).executeAsList()

    fun getTransactionSummary(txidParameter: String, ownerAddress: String): TransactionSummary? =
            fioQueries.selectFioTransactionSummaryById(uuid, txidParameter, mapper = { txid: String,
                                                                                       currency: CryptoCurrency,
                                                                                       blockNumber: Int,
                                                                                       timestamp: Long,
                                                                                       value: Value,
                                                                                       fee: Value,
                                                                                       confirmations: Int,
                                                                                       from: String,
                                                                                       to: String,
                                                                                       memo: String? ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, memo)
            }).executeAsOneOrNull()

    fun putTransaction(blockNumber: Int, timestamp: Long, txid: String, raw: String, from: String, to: String, value: Value,
                       confirmations: Int, fee: Value, memo: String = "") {
        queries.insertTransaction(txid, uuid, currency, if (blockNumber == -1) Int.MAX_VALUE else blockNumber,
                timestamp, raw, value, fee, confirmations)
        fioQueries.insertTransaction(txid, uuid, from, to, memo)
    }

    private fun createTransactionSummary(ownerAddress: String,
                                         txid: String,
                                         currency: CryptoCurrency,
                                         blockNumber: Int,
                                         timestamp: Long,
                                         value: Value,
                                         fee: Value,
                                         confirmations: Int,
                                         from: String,
                                         to: String,
                                         memo: String?): TransactionSummary {
        val fromAddress = FioAddress(currency, FioAddressData(from))
        val toAddress = FioAddress(currency, FioAddressData(to))
        val inputs = listOf(InputViewModel(fromAddress, value, false))
        val outputs = listOf(OutputViewModel(toAddress, value, false))
        val destAddresses = listOf(toAddress)
        return FioTransactionSummary(fromAddress, toAddress, memo,
                currency, HexUtils.toBytes(txid), HexUtils.toBytes(txid), value, timestamp, if (blockNumber == Int.MAX_VALUE) -1 else blockNumber,
                confirmations, false, inputs, outputs,
                destAddresses, null, Transfer.GAS_LIMIT.toInt(), fee)
    }
}