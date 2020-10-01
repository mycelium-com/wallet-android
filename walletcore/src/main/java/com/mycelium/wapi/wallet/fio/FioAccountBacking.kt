package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import org.web3j.tx.Transfer
import java.util.*

class FioAccountBacking(walletDB: WalletDB, private val uuid: UUID, private val currency: CryptoCurrency) {
    private val fioQueries = walletDB.fioAccountBackingQueries
    private val queries = walletDB.accountBackingQueries

    fun putRequestsSummaries(list: List<FIORequestContent>) {
        list.forEach {
            fioQueries.insertRequest(
                    it.fioRequestId.longValueExact(),
                    it.payerFioAddress,
                    it.payeeFioAddress,
                    it.payerFioAddress,
                    it.payeeFioAddress,
                    it.content,
                    it.timeStamp)
        }
    }

    fun getRequestsContent(): List<FIORequestContent> {
        return fioQueries.selectFioRequests { fio_request_id, payer_fio_address, payee_fio_address, payer_fio_public_key, payee_fio_public_key, content, time_stamp ->
            val fioRequestContent = FIORequestContent()
            fioRequestContent.fioRequestId = fio_request_id.toBigInteger()
            fioRequestContent.payerFioAddress = payer_fio_address
            fioRequestContent.payeeFioAddress = payee_fio_address
            fioRequestContent.payerFioPublicKey = payer_fio_public_key
            fioRequestContent.payeeFioPublicKey = payee_fio_public_key
            fioRequestContent.content = content
            fioRequestContent.timeStamp = time_stamp
            fioRequestContent
        }.executeAsList()
    }

    fun getTransactionSummaries(offset: Long, limit: Long): List<TransactionSummary> =
            fioQueries.selectFioTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                     currency: CryptoCurrency,
                                                                                     blockNumber: Int,
                                                                                     timestamp: Long,
                                                                                     value: Value,
                                                                                     fee: Value,
                                                                                     confirmations: Int,
                                                                                     from: String,
                                                                                     to: String,
                                                                                     transferred: Value,
                                                                                     memo: String? ->
                createTransactionSummary(txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, transferred, memo)
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
                                                                                       transferred: Value,
                                                                                       memo: String? ->
                createTransactionSummary(txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, transferred, memo)
            }).executeAsOneOrNull()

    fun putTransaction(blockNumber: Int, timestamp: Long, txid: String, raw: String, from: String, to: String, value: Value,
                       confirmations: Int, fee: Value, transferred: Value, memo: String = "") {
        queries.insertTransaction(txid, uuid, currency, if (blockNumber == -1) Int.MAX_VALUE else blockNumber,
                timestamp, raw, value, fee, confirmations)
        fioQueries.insertTransaction(txid, uuid, from, to, transferred, memo)
    }

    private fun createTransactionSummary(txid: String,
                                         currency: CryptoCurrency,
                                         blockNumber: Int,
                                         timestamp: Long,
                                         value: Value,
                                         fee: Value,
                                         confirmations: Int,
                                         from: String,
                                         to: String,
                                         transferred: Value,
                                         memo: String?): TransactionSummary {
        val fromAddress = FioAddress(currency, FioAddressData(from))
        val toAddress = FioAddress(currency, FioAddressData(to))
        val inputs = listOf(InputViewModel(fromAddress, value, false))
        val outputs = listOf(OutputViewModel(toAddress, value, false))
        val destAddresses = listOf(toAddress)
        return FioTransactionSummary(fromAddress, toAddress, memo, value,
                currency, HexUtils.toBytes(txid), HexUtils.toBytes(txid), transferred,
                timestamp, if (blockNumber == Int.MAX_VALUE) -1 else blockNumber,
                confirmations, false, inputs, outputs,
                destAddresses, null, Transfer.GAS_LIMIT.toInt(), fee)
    }
}