package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import io.reactivex.Observable
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class EthBalanceService(val address: String, val coinType: CryptoCurrency) {
    private val web3j: Web3j = Web3j.build(HttpService("http://ropsten-index.mycelium.com:18545"))
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    val pendingTxObservable: Observable<Balance> = Observable.create<Balance> { observer ->
        try {
            // check whether we have missed any pending transactions events while app was inactive
            pollAndUpdateBalance()

            // start listening for new pending transactions
            @Suppress("CheckResult")
            web3j.pendingTransactionFlowable().filter { tx -> tx.to == address || tx.from == address }
                    .subscribe({
                        pollAndUpdateBalance()
                        observer.onNext(balance)
                    }, { observer.onError(it) })
            web3j.transactionFlowable().filter { tx -> tx.to == address || tx.from == address }
                    .subscribe({
                        pollAndUpdateBalance()
                        observer.onNext(balance)
                    }, { observer.onError(it) })
        } catch (e: Exception) {
            observer.onError(e)
            // todo decide what to do with this observable
        }
    }

    @Throws(Exception::class)
    private fun pollAndUpdateBalance() {
        val response = web3j.ethGetBlockByNumber(DefaultBlockParameterName.PENDING, true).send()

        if (response.hasError()) {
            throw Exception("${response.error.code}: ${response.error.message}")
        }

        val txs = response.block.transactions.map { txResult -> txResult as Transaction }
        val incomingTx = txs.filter { it.to == address }
        val outgoingTx = txs.filter { it.from == address }
        val incomingSum: BigInteger = incomingTx
                .takeIf { it.isNotEmpty() }?.map { tx -> tx.value }?.reduce { acc, value -> acc + value } ?: BigInteger.ZERO
        val outgoingSum: BigInteger = outgoingTx
                .takeIf { it.isNotEmpty() }?.map { tx -> tx.value + tx.gasPrice * tx.gas }?.reduce { acc, value -> acc + value } ?: BigInteger.ZERO
        updateBalance(incomingSum.toLong(), outgoingSum.toLong())
    }

    private fun updateBalance(incomingSum: Long, outgoingSumWithGas: Long) {
        balance = Balance(balance.confirmed, Value.valueOf(coinType, incomingSum),
                Value.valueOf(coinType, outgoingSumWithGas), balance.pendingChange)
        updateBalanceCache()
    }

    fun updateBalanceCache(): Boolean {
        return try {
            val balanceRequest = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            val balanceResult = balanceRequest.send()
            balance = Balance(Value.valueOf(coinType, balanceResult.balance) - balance.pendingSending,
                    balance.pendingReceiving, balance.pendingSending, balance.pendingChange)
            true
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: UnknownHostException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
