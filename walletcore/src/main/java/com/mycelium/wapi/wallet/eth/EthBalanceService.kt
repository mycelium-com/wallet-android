package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class EthBalanceService(val address: String, val coinType: CryptoCurrency) {
    private val web3j: Web3j = Web3j.build(HttpService("http://ropsten-index.mycelium.com:18545"))
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    val pendingTxObservable: Observable<Balance> = Observable.create<Balance> { observer ->
        // check whether we have missed any pending transactions events while app was inactive
        try {
            val response = web3j.ethGetBlockByNumber(DefaultBlockParameterName.PENDING, true).send()

            if (response.hasError()) {
                observer.onError(Throwable("${response.error.code}: ${response.error.message}"))
                // todo decide what to do with this observable
            }

            val txs = response.block.transactions.map { txResult -> txResult as Transaction }

            // process incoming pending txs
            txs.filter { it.to == address }.forEach { pendingTx ->
                processIncomingTx(pendingTx, observer)
            }

            // process outgoing pending txs
            txs.filter { it.from == address }.forEach { pendingTx ->
                processOutgoingTx(pendingTx, observer)
            }
        } catch (e: Exception) {
            observer.onError(e)
            // todo decide what to do with this observable
        }

        // start listening for new pending transactions
        // incoming
        val disposable = web3j.pendingTransactionFlowable().filter { tx -> tx.to == address }
                .subscribe({ pendingTx ->
                    processIncomingTx(pendingTx, observer)
                }, { observer.onError(it) })

        // outgoing
        if (!disposable.isDisposed) {
            web3j.pendingTransactionFlowable().filter { tx -> tx.from == address }
                    .subscribe({ pendingTx ->
                        processOutgoingTx(pendingTx, observer)
                    }, { observer.onError(it) })
        }
    }

    private fun processIncomingTx(pendingTx: Transaction, observer: ObservableEmitter<Balance>) {
        val receiveAmount = Value.valueOf(coinType, pendingTx.value)
        balance = Balance(balance.confirmed, balance.pendingReceiving + receiveAmount, balance.pendingSending,
                balance.pendingChange)
        observer.onNext(balance)

        // wait for the tx confirmation
        @Suppress("CheckResult")
        web3j.transactionFlowable().filter { tx -> tx.hash == pendingTx.hash }.firstOrError().subscribe({
            balance = Balance(balance.confirmed + receiveAmount,balance.pendingReceiving - receiveAmount, balance.pendingSending,
                    balance.pendingChange)
            observer.onNext(balance)
        }, { observer.onError(it) })
    }

    private fun processOutgoingTx(pendingTx: Transaction, observer: ObservableEmitter<Balance>) {
        val spentAmount = Value.valueOf(coinType, pendingTx.value + pendingTx.gasPrice * pendingTx.gas)
        balance = Balance(balance.confirmed - spentAmount, balance.pendingReceiving,
                balance.pendingSending + spentAmount, balance.pendingChange)
        observer.onNext(balance)

        // wait for the tx confirmation
        @Suppress("CheckResult")
        web3j.transactionFlowable().filter { tx -> tx.hash == pendingTx.hash }.firstOrError().subscribe({
            balance = Balance(balance.confirmed, balance.pendingReceiving, balance.pendingSending - spentAmount,
                    balance.pendingChange)
            observer.onNext(balance)
        }, { observer.onError(it) })
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
