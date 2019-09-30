package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import io.reactivex.Observable
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class EthBalanceService(val address: String, val coinType: CryptoCurrency) {
    private val web3j: Web3j = Web3j.build(HttpService("http://ropsten-index.mycelium.com:18545"))
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    val pendingTxObservable: Observable<Balance> = Observable.create<Balance> { e ->
        // incoming tx filter
        val disposable = web3j.pendingTransactionFlowable().filter { tx -> tx.to == address }.subscribe({ pendingTx ->
            balance = Balance(balance.confirmed, balance.pendingReceiving
                    + Value.valueOf(coinType, pendingTx.value), balance.pendingSending,
                    balance.pendingChange)
            e.onNext(balance)

            // wait for the tx confirmation
            web3j.transactionFlowable().filter { tx -> tx.hash == pendingTx.hash }.subscribe({ confirmedTx ->
                balance = Balance(balance.confirmed + Value.valueOf(coinType, confirmedTx.value),
                        balance.pendingReceiving - Value.valueOf(coinType, confirmedTx.value), balance.pendingSending,
                        balance.pendingChange)
                e.onNext(balance)
            }, { e.onError(it) })
        }, {
            e.onError(it)
        })

        // outgoing tx filter
        if (!disposable.isDisposed) {
            web3j.pendingTransactionFlowable().filter { tx -> tx.from == address }.subscribe({ pendingTx ->
                balance = Balance(balance.confirmed, balance.pendingReceiving,
                        balance.pendingSending + Value.valueOf(coinType, pendingTx.value),
                        balance.pendingChange)
                e.onNext(balance)

                // wait for the tx confirmation
                web3j.transactionFlowable().filter { tx -> tx.hash == pendingTx.hash }.subscribe({ confirmedTx ->
                    balance = Balance(balance.confirmed + Value.valueOf(coinType, confirmedTx.value),
                            balance.pendingReceiving, balance.pendingSending - Value.valueOf(coinType, confirmedTx.value),
                            balance.pendingChange)
                    e.onNext(balance)
                }, { e.onError(it) })
            }, {
                e.onError(it)
            })
        }
    }

    fun updateBalanceCache(): Boolean {
        return try {
            val balanceRequest = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            val balanceResult = balanceRequest.send()
            balance = Balance(Value.valueOf(coinType, balanceResult.balance),
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
