package com.mycelium.wapi.wallet.genericdb

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.GenericInputViewModel
import com.mycelium.wapi.wallet.GenericOutputViewModel
import com.mycelium.wapi.wallet.GenericTransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAddress
import java.lang.IllegalStateException
import java.math.BigInteger
import java.util.*

class EthAccountBacking(walletDB: WalletDB, private val uuid: UUID, private val currency: CryptoCurrency, private val token: ERC20Token? = null) {
    private val ethQueries = walletDB.ethAccountBackingQueries
    private val queries = walletDB.accountBackingQueries
    private val contractCreationAddress = EthAddress(currency, "0x0000000000000000000000000000000000000000")

    fun getTransactionSummaries(offset: Long, limit: Long, ownerAddress: String): List<GenericTransactionSummary> =
            ethQueries.selectTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                  currency: CryptoCurrency,
                                                                                  blockNumber: Int,
                                                                                  timestamp: Long,
                                                                                  value: Value,
                                                                                  fee: Value,
                                                                                  confirmations: Int,
                                                                                  from: String,
                                                                                  to: String,
                                                                                  nonce: BigInteger?,
                                                                                  gasLimit: BigInteger,
                                                                                  gasUsed: BigInteger ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber,
                        timestamp, value, fee, confirmations, from, to, nonce, gasLimit, gasUsed)
            }).executeAsList()

    /**
     * @param timestampParameter time in seconds
     */
    fun getTransactionSummariesSince(timestampParameter: Long, ownerAddress: String): List<GenericTransactionSummary> =
            ethQueries.selectTransactionSummariesSince(uuid, timestampParameter, mapper = { txid: String,
                                                                                            currency: CryptoCurrency,
                                                                                            blockNumber: Int,
                                                                                            timestamp: Long,
                                                                                            value: Value,
                                                                                            fee: Value,
                                                                                            confirmations: Int,
                                                                                            from: String,
                                                                                            to: String,
                                                                                            nonce: BigInteger?,
                                                                                            gasLimit: BigInteger,
                                                                                            gasUsed: BigInteger ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber,
                        timestamp, value, fee, confirmations, from, to, nonce, gasLimit, gasUsed)
            }).executeAsList()

    fun getTransactionSummary(txidParameter: String, ownerAddress: String): GenericTransactionSummary? =
            ethQueries.selectTransactionSummaryById(uuid, txidParameter, mapper = { txid: String,
                                                                                    currency: CryptoCurrency,
                                                                                    blockNumber: Int,
                                                                                    timestamp: Long,
                                                                                    value: Value,
                                                                                    fee: Value,
                                                                                    confirmations: Int,
                                                                                    from: String,
                                                                                    to: String,
                                                                                    nonce: BigInteger?,
                                                                                    gasLimit: BigInteger,
                                                                                    gasUsed: BigInteger ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber,
                        timestamp, value, fee, confirmations, from, to, nonce, gasLimit, gasUsed)
            }).executeAsOneOrNull()

    fun getUnconfirmedTransactions(): List<UnconfirmedTransaction> =
            ethQueries.selectUnconfirmedTransactions(uuid, mapper = { fromAddress, toAddress, value, fee ->
                        UnconfirmedTransaction(fromAddress, toAddress, value, fee) }).executeAsList()

    fun putTransaction(blockNumber: Int, timestamp: Long, txid: String, raw: String, from: String, to: String, value: Value,
                       gasPrice: Value, confirmations: Int, nonce: BigInteger, gasLimit: BigInteger = BigInteger.valueOf(21000), gasUsed: BigInteger = BigInteger.valueOf(21000)) {
        queries.insertTransaction(txid, uuid, currency, if (blockNumber == -1) Int.MAX_VALUE else blockNumber, timestamp, raw, value, gasPrice, confirmations)
        ethQueries.insertTransaction(txid, uuid, from, to, nonce, gasLimit, gasUsed)
    }

    fun updateTransaction(txid: String, blockNumber: Int, confirmations: Int) {
        queries.updateTransaction(blockNumber, confirmations, uuid, txid)
    }

    fun updateGasUsed(txid: String, gasUsed: BigInteger, fee: Value) {
        ethQueries.updateGasUsed(gasUsed, uuid, txid)
        queries.updateFee(fee, uuid, txid)
    }

    fun updateNonce(txid: String, nonce: BigInteger) {
        ethQueries.updateNonce(nonce, uuid, txid)
    }

    fun deleteTransaction(txid: String) {
        queries.deleteTransaction(uuid, txid)
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
                                         nonce: BigInteger?,
                                         gasLimit: BigInteger,
                                         gasUsed: BigInteger): GenericTransactionSummary {
        val convertedValue = if (token != null) transformValueFromDb(token, value) else value
        val inputs = listOf(GenericInputViewModel(EthAddress(currency, from), value, false))
        // "to" address may be empty if we have a contract funding transaction
        val outputs = if (to.isEmpty()) listOf()
        else listOf(GenericOutputViewModel(EthAddress(currency, to), convertedValue, false))
        val destAddresses = if (to.isEmpty()) listOf(contractCreationAddress) else listOf(EthAddress(currency, to))
        val transferred: Value = if ((from == ownerAddress) && (to != ownerAddress)) {
            if (token != null) -convertedValue else -convertedValue - fee
        } else if ((from != ownerAddress) && (to == ownerAddress)) {
            convertedValue
        } else if ((from == ownerAddress) && (to == ownerAddress)) {
            -fee // sent to ourselves
        } else {
            // transaction doesn't relate to us in any way. should not happen
            throw IllegalStateException("Transaction that wasn't sent to us or from us detected.")
        }
        return EthTransactionSummary(EthAddress(currency, from), EthAddress(currency, to), nonce,
                convertedValue, gasLimit, gasUsed, currency, HexUtils.toBytes(txid.substring(2)),
                HexUtils.toBytes(txid.substring(2)), transferred, timestamp, if (blockNumber == Int.MAX_VALUE) -1 else blockNumber,
                confirmations, false, inputs, outputs,
                destAddresses, null, 21000, fee)
    }

    private fun transformValueFromDb(token: ERC20Token, value: Value): Value {
        return Value.valueOf(token, value.value)
    }
}

class UnconfirmedTransaction(val from: String, val to: String, val value: Value, val fee: Value)