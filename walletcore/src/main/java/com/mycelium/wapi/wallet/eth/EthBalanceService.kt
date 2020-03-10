package com.mycelium.wapi.wallet.eth

import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
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

class EthBalanceService(val address: String,
                        val coinType: CryptoCurrency,
                        var client: Web3j,
                        var endpoints: ServerEndpoints) {
    private val logger = Logger.getLogger(EthBalanceService::class.simpleName)
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    private val allTxsFlowable get() = client.pendingTransactionFlowable()
            .onErrorReturn { Transaction() }
    val incomingTxsFlowable get() = allTxsFlowable.filter { tx -> tx.to.equals(address, true) }
    val outgoingTxsFlowable get() = allTxsFlowable.filter { tx -> tx.from.equals(address, true) }

    val balanceFlowable get() =
            incomingTxsFlowable.mergeWith(outgoingTxsFlowable)
                .flatMapSingle {
                    updateBalanceCache()
                    Single.just(balance)
                }

    fun updateBalanceCache(): Boolean {
        return try {
            val balanceRequest = client.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            val balanceResult = balanceRequest.send()
            val txs: List<Transaction> = try {
                getPendingTransactions()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed while requesting pending transactions $e")
                emptyList()
            }
            val incomingTx = txs.filter { !it.from.equals(address, true) && it.to.equals(address, true) }
            val outgoingTx = txs.filter { it.from.equals(address, true) && !it.to.equals(address, true) }
            val toSelfTx = txs.filter { it.from.equals(address, true) && it.to.equals(address, true) }

            val incomingSum: BigInteger = incomingTx
                    .map(Transaction::getValue)
                    .fold(BigInteger.ZERO, BigInteger::add)
            val outgoingSum: BigInteger = outgoingTx
                    .map { tx -> tx.value + tx.gasPrice * tx.gas }
                    .fold(BigInteger.ZERO, BigInteger::add)
            val toSelfTxSum: BigInteger = toSelfTx
                    .map { tx -> tx.gasPrice * tx.gas }
                    .fold(BigInteger.ZERO, BigInteger::add)

            balance = Balance(Value.valueOf(coinType, balanceResult.balance - outgoingSum - toSelfTxSum),
                    Value.valueOf(coinType, incomingSum), Value.valueOf(coinType, outgoingSum + toSelfTxSum), Value.zeroValue(coinType))
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
                HttpService(endpoints.currentEndpoint.baseUrl),
                ParityAllTransactionsResponse::class.java)
        val response = request.send()
        if (response.hasError()) {
            throw Exception("${response.error.code}: ${response.error.message}")
        } else {
            return response.transactions
        }
    }
}

class ParityAllTransactionsResponse : Response<ArrayList<Transaction>>() {
    val transactions: ArrayList<Transaction>
        get() = result
}
