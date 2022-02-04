package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.ObtDataRecord
import fiofoundation.io.fiosdk.models.fionetworkprovider.SentFIORequestContent
import org.web3j.tx.Transfer
import java.math.BigInteger
import java.util.*

class FioAccountBacking(val walletDB: WalletDB, private val uuid: UUID, private val currency: CryptoCurrency) {
    private val fioSentRequestQueries = walletDB.fioRequestsSentBackingQueries
    private val fioReceivedRequestQueries = walletDB.fioRequestsReceivedBackingQueries
    private val fioAccountQueries = walletDB.fioAccountBackingQueries
    private val fioMappings = walletDB.fioNameAccountMappingsQueries
    private val queries = walletDB.accountBackingQueries
    private val fioOBTQueries = walletDB.fioOtherBlockchainTransactionsQueries

    fun putSentRequests(list: List<SentFIORequestContent>) {
        fioSentRequestQueries.transaction {
            list.forEach {
                fioSentRequestQueries.insertRequest(
                        it.fioRequestId,
                        uuid,
                        it.payerFioAddress,
                        it.payeeFioAddress,
                        it.payerFioAddress,
                        it.payeeFioAddress,
                        it.content,
                        it.deserializedContent,
                        it.timeStamp,
                        FioRequestStatus.getStatus(it.status))
            }
        }
    }

    fun putReceivedRequests(list: List<FIORequestContent>) {
        fioReceivedRequestQueries.transaction {
            list.forEach {
                fioReceivedRequestQueries.insertRequest(
                        it.fioRequestId,
                        uuid,
                        it.payerFioAddress,
                        it.payeeFioAddress,
                        it.payerFioAddress,
                        it.payeeFioAddress,
                        it.content,
                        it.deserializedContent,
                        it.timeStamp)
            }
        }
    }

    fun deleteSentRequests() {
        fioSentRequestQueries.deleteRequests(uuid)
    }

    fun deletePendingRequest(fioRequestId: BigInteger) {
        fioReceivedRequestQueries.deleteRequest(fioRequestId)
    }

    fun deletePendingRequests() {
        fioReceivedRequestQueries.deleteRequests(uuid)
    }

    fun insertOrUpdateMapping(fioName: String, publicAddress: String, chainCode: String, tokenCode: String,
                              accountUUID: UUID) {
        fioMappings.insertMapping(fioName, publicAddress, chainCode, tokenCode, accountUUID)
    }

    data class Mapping(val fioName: String,
                       val publicAddress: String,
                       val chainCode: String,
                       val tokenCode: String,
                       val accountUUID: UUID)

    fun insertOrUpdateMappings(mappings: List<Mapping>) {
        fioMappings.transaction {
            mappings.forEach {
                insertOrUpdateMapping(it.fioName, it.publicAddress, it.chainCode, it.tokenCode, it.accountUUID)
            }
        }
    }

    fun getRequestsGroups(): List<FioGroup> {
        val fioSentGroup = FioGroup(FioGroup.Type.SENT, mutableListOf())
        val fioPendingGroup = FioGroup(FioGroup.Type.PENDING, mutableListOf())
        fioSentRequestQueries.selectAccountFioRequests(uuid) { fio_request_id, uuid, payer_fio_address, payee_fio_address,
                                                               payer_fio_public_key, payee_fio_public_key, content,
                                                               deserialized_content, time_stamp, status ->
            fioSentGroup.children.add(
                    SentFIORequestContent().apply {
                        fioRequestId = fio_request_id
                        payerFioAddress = payer_fio_address
                        payeeFioAddress = payee_fio_address
                        payerFioPublicKey = payer_fio_public_key
                        payeeFioPublicKey = payee_fio_public_key
                        this.content = content
                        deserializedContent = deserialized_content
                        timeStamp = time_stamp
                        this.status = status?.status!!
                    })
        }.executeAsList()
        fioReceivedRequestQueries.selectAccountFioRequests(uuid) { fio_request_id, uuid, payer_fio_address, payee_fio_address,
                                                                   payer_fio_public_key, payee_fio_public_key, content,
                                                                   deserialized_content, time_stamp ->
            fioPendingGroup.children.add(
                    FIORequestContent().apply {
                        fioRequestId = fio_request_id
                        payerFioAddress = payer_fio_address
                        payeeFioAddress = payee_fio_address
                        payerFioPublicKey = payer_fio_public_key
                        payeeFioPublicKey = payee_fio_public_key
                        this.content = content
                        deserializedContent = deserialized_content
                        timeStamp = time_stamp
                    })
        }.executeAsList()
        return listOf(fioPendingGroup, fioSentGroup)
    }

    fun getTransactionSummaries(offset: Long, limit: Long): List<TransactionSummary> =
            fioAccountQueries.selectFioTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
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
            fioAccountQueries.selectFioTransactionSummaryById(uuid, txidParameter, mapper = { txid: String,
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
        fioAccountQueries.insertTransaction(txid, uuid, from, to, transferred, memo)
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

    fun putOBT(list: List<ObtDataRecord>) {
        fioOBTQueries.transaction {
            list.forEach {
                fioOBTQueries.insertTx(
                        it.deserializedContent?.obtId ?: "",
                        it.fioRequestId,
                        it.payerFioAddress,
                        it.payeeFioAddress,
                        it.payerFioAddress,
                        it.payeeFioAddress,
                        it.content,
                        it.status,
                        it.timeStamp,
                        it.deserializedContent)
            }
        }
    }

    fun putTransactions(blockHeight: Int, txs: List<Tx>) {
        walletDB.transaction {
            txs.forEach {
                putTransaction(it.blockNumber.toInt(), it.timestamp, it.txid, "",
                        it.fromAddress, it.toAddress, it.sum,
                        kotlin.math.max(blockHeight - it.blockNumber.toInt(), 0),
                        it.fee, it.transferred, it.memo)
            }
        }
    }
}
