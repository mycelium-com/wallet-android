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
import org.web3j.tx.Transfer
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
                                                                                  gasUsed: BigInteger,
                                                                                  success: Boolean ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, success)
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
                                                                                            gasUsed: BigInteger,
                                                                                            success: Boolean ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, success)
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
                                                                                    gasUsed: BigInteger,
                                                                                    success: Boolean ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, success)
            }).executeAsOneOrNull()

    fun getUnconfirmedTransactions(ownerAddress: String): List<EthTransactionSummary> =
            ethQueries.selectUnconfirmedTransactions(uuid, mapper = { txid: String,
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
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed)
            }).executeAsList()


    fun putTransaction(blockNumber: Int, timestamp: Long, txid: String, raw: String, from: String, to: String?, value: Value,
                       gasPrice: Value, confirmations: Int, nonce: BigInteger, success: Boolean = true,
                       gasLimit: BigInteger = Transfer.GAS_LIMIT, gasUsed: BigInteger? = null) {
        queries.insertTransaction(txid, uuid, currency, if (blockNumber == -1) Int.MAX_VALUE else blockNumber, timestamp, raw, value, gasPrice, confirmations)
        ethQueries.insertTransaction(txid, uuid, from, to ?: contractCreationAddress.addressString, nonce, gasLimit, success)
        if (gasUsed != null) {
            updateGasUsed(txid, gasUsed, gasPrice)
        }
    }

    fun deleteAllAccountTransactions() {
        queries.deleteAllAccountTransactions(uuid)
    }

    fun deleteTransaction(txid: String) {
        queries.deleteTransaction(uuid, txid)
    }

    private fun updateGasUsed(txid: String, gasUsed: BigInteger, fee: Value) {
        ethQueries.updateGasUsed(gasUsed, uuid, txid)
        queries.updateFee(fee, uuid, txid)
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
                                         gasUsed: BigInteger,
                                         success: Boolean = true): EthTransactionSummary {
        val convertedValue = if (token != null) transformValueFromDb(token, value) else value
        val inputs = listOf(GenericInputViewModel(EthAddress(currency, from), value, false))
        // "to" address may be empty if we have a contract funding transaction
        val outputs = if (to.isEmpty()) {
            listOf()
        } else {
            listOf(GenericOutputViewModel(EthAddress(currency, to), convertedValue, false))
        }

        val destAddresses = listOf(if (to.isEmpty()) contractCreationAddress else EthAddress(currency, to))
        val transferred = if (token != null) getTokenTransferred(ownerAddress, from, to, convertedValue)
                          else getEthTransferred(ownerAddress, from, to, convertedValue, fee, success)
        return EthTransactionSummary(EthAddress(currency, from), EthAddress(currency, to), nonce,
                convertedValue, gasLimit, gasUsed, currency, HexUtils.toBytes(txid.substring(2)),
                HexUtils.toBytes(txid.substring(2)), transferred, timestamp, if (blockNumber == Int.MAX_VALUE) -1 else blockNumber,
                confirmations, false, inputs, outputs,
                destAddresses, null, Transfer.GAS_LIMIT.toInt(), fee)
    }

    private fun transformValueFromDb(token: ERC20Token, value: Value): Value = Value.valueOf(token, value.value)

    private fun getTokenTransferred(ownerAddress: String, from: String, to: String, value: Value): Value {
        return if (from.equals(ownerAddress, true) && !to.equals(ownerAddress, true)) {
            // outgoing
            -value
        } else if (!from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // incoming
            value
        } else if (from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // self
            Value.zeroValue(token!!)
        } else {
            // transaction doesn't relate to us in any way. should not happen
            throw IllegalStateException("Transaction that wasn't sent to us or from us detected.")
        }
    }

    private fun getEthTransferred(ownerAddress: String, from: String, to: String, value: Value,
                                  fee: Value, success: Boolean): Value {
        return if (from.equals(ownerAddress, true) && !to.equals(ownerAddress, true)) {
            // outgoing
            if (success) -value - fee else -fee
        } else if (!from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // incoming
            if (success) value else Value.zeroValue(currency)
        } else if (from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // self
            -fee
        } else {
            // transaction doesn't relate to us in any way. should not happen
            throw IllegalStateException("Transaction that wasn't sent to us or from us detected.")
        }
    }
}