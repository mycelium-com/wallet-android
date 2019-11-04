package com.mycelium.wapi.wallet.genericdb

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.GenericInputViewModel
import com.mycelium.wapi.wallet.GenericOutputViewModel
import com.mycelium.wapi.wallet.GenericTransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAddress
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class EthAccountBacking(walletDB: WalletDB, private val uuid: UUID, private val currency: CryptoCurrency) {
    private val ethQueries = walletDB.ethAccountBackingQueries
    private val queries = walletDB.accountBackingQueries
    private val logger = Logger.getLogger(this.javaClass.name)

    fun getTransactionSummaries(offset: Long, limit: Long, ownerAddress: String): List<GenericTransactionSummary>? =
            ethQueries.selectTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                  currency: CryptoCurrency,
                                                                                  blockNumber: Int,
                                                                                  timestamp: Long,
                                                                                  value: Value,
                                                                                  fee: Value,
                                                                                  confirmations: Int,
                                                                                  from: String,
                                                                                  to: String ->
                val input = GenericInputViewModel(EthAddress(currency, from), value, false)
                logger.log(Level.INFO, "from: $from")
                val output = GenericOutputViewModel(EthAddress(currency, to), value, false)
                logger.log(Level.INFO, "to: $to")
                logger.log(Level.INFO, "txid: $txid")
                val transferred = if (to == ownerAddress) value else -value - fee
                GenericTransactionSummary(currency, HexUtils.toBytes(txid.substring(2)),
                        HexUtils.toBytes(txid.substring(2)), transferred, timestamp, blockNumber,
                        confirmations, false, listOf(input), listOf(output),
                        listOf(EthAddress(currency, to)), null, 21000, fee)
            }).executeAsList()

    fun getTransactionSummary(txidParameter: String, ownerAddress: String): GenericTransactionSummary =
            ethQueries.selectTransactionSummaryById(uuid, txidParameter, mapper = { txid: String,
                                                                                    currency: CryptoCurrency,
                                                                                    blockNumber: Int,
                                                                                    timestamp: Long,
                                                                                    value: Value,
                                                                                    fee: Value,
                                                                                    confirmations: Int,
                                                                                    from: String,
                                                                                    to: String ->
                val input = GenericInputViewModel(EthAddress(currency, from), value, false)
                val output = GenericOutputViewModel(EthAddress(currency, to), value, false)
                val transferred = if (to == ownerAddress) value else -value - fee
                GenericTransactionSummary(currency, HexUtils.toBytes(txid.substring(2)),
                        HexUtils.toBytes(txid.substring(2)), transferred, timestamp, blockNumber,
                        confirmations, false, listOf(input), listOf(output),
                        listOf(EthAddress(currency, to)), null, 21000, fee)
            }).executeAsOne()

    fun putTransaction(blockNumber: Int, timestamp: Long, txid: String,
                       raw: String, from: String, to: String, value: Value,
                       gasPrice: Value, confirmations: Int) {
        ethQueries.insertTransaction(txid, uuid, from, to)
        queries.insertTransaction(txid, uuid, currency, blockNumber, timestamp, raw, value, gasPrice, confirmations)
    }
}