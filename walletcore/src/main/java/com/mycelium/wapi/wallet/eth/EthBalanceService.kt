package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import io.reactivex.Flowable
import io.reactivex.Single
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.logging.Level
import java.util.logging.Logger

class EthBalanceService(val address: String, val coinType: CryptoCurrency, private val web3jService: HttpService) {
    private val web3j: Web3j = Web3j.build(web3jService)
    private val logger = Logger.getLogger(EthBalanceService::javaClass.name)
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    val incomingTxFlowable: Flowable<Transaction> = web3j.pendingTransactionFlowable().filter { tx -> tx.to == address }
    val outgoingTxFlowable: Flowable<Transaction> = web3j.pendingTransactionFlowable().filter { tx -> tx.from == address }

    val balanceFlowable: Flowable<Balance> =
            incomingTxFlowable.mergeWith(outgoingTxFlowable)
                .flatMapSingle {
                    updateBalanceCache()
                    Single.just(balance)
                }

    fun updateBalanceCache(): Boolean {
        return try {
            val balanceRequest = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            val balanceResult = balanceRequest.send()
            val txs = try {
                getPendingTransactions()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed while requesting pending transactions $e")
                emptyList<Transaction>()
            }
            val incomingTx = txs.filter { it.to == address }
            val outgoingTx = txs.filter { it.from == address }

            val incomingSum: BigInteger = incomingTx
                    .map(Transaction::getValue)
                    .fold(BigInteger.ZERO, BigInteger::add)
            val outgoingSum: BigInteger = outgoingTx
                    .map { tx -> tx.value + tx.gasPrice * tx.gas }
                    .fold(BigInteger.ZERO, BigInteger::add)

            balance = Balance(Value.valueOf(coinType, balanceResult.balance - outgoingSum),
                    Value.valueOf(coinType, incomingSum), Value.valueOf(coinType, outgoingSum), balance.pendingChange)
            true
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: UnknownHostException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun getPendingTransactions(): List<Transaction> {
        val request = Request<Any, ParityAllTransactionsResponse>(
                "parity_allTransactions",
                emptyList(),
                web3jService,
                ParityAllTransactionsResponse::class.java).send()
        if (request.hasError()) {
            throw Exception("${request.error.code}: ${request.error.message}")
        } else {
            return request.transactions
        }
    }
}

class ParityAllTransactionsResponse : Response<ArrayList<Transaction>>() {
    val transactions: ArrayList<Transaction>
        get() = result
}
