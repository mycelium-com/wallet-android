package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import io.reactivex.Observable
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

class EthBalanceService(val address: String, val coinType: CryptoCurrency) {
    private val web3jService = HttpService("http://ropsten-index.mycelium.com:18545")
    private val web3j: Web3j = Web3j.build(web3jService)
    private val logger = Logger.getLogger(EthBalanceService::javaClass.name)
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    val balanceObservable: Observable<Balance> = Observable.create<Balance> { observer ->
        web3j.pendingTransactionFlowable().filter { tx -> tx.to == address || tx.from == address }
                .subscribe({
                    updateBalanceCache()
                    observer.onNext(balance)
                }, { observer.onError(it) })
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
                    .takeIf { it.isNotEmpty() }?.map { tx -> tx.value }?.reduce { acc, value -> acc + value }
                    ?: BigInteger.ZERO
            val outgoingSum: BigInteger = outgoingTx
                    .takeIf { it.isNotEmpty() }?.map { tx -> tx.value + tx.gasPrice * tx.gas }?.reduce { acc, value -> acc + value }
                    ?: BigInteger.ZERO
            balance = Balance(Value.valueOf(coinType, balanceResult.balance) - Value.valueOf(coinType, outgoingSum),
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
