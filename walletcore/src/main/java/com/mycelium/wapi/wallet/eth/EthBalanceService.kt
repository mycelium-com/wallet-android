package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import io.reactivex.Single
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.Transaction
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger

class EthBalanceService(private val address: String,
                        private val coinType: CryptoCurrency,
                        private val web3jWrapper: Web3jWrapper) {
    private val logger = Logger.getLogger(EthBalanceService::class.simpleName)
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    private val allTxsFlowable get() = web3jWrapper.pendingTransactionFlowable()
                .onErrorReturn { Transaction() }
    val incomingTxsFlowable get() = allTxsFlowable.filter { tx -> tx.to == address }
    val outgoingTxsFlowable get() = allTxsFlowable.filter { tx -> tx.from == address }

    val balanceFlowable
        get() =
            incomingTxsFlowable.mergeWith(outgoingTxsFlowable)
                    .flatMapSingle {
                        updateBalanceCache()
                        Single.just(balance)
                    }

    fun updateBalanceCache(): Boolean {
        return try {
            val balanceRequest = web3jWrapper.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            val balanceResult = balanceRequest.send()
            val txs = try {
                web3jWrapper.getPendingTransactions()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed while requesting pending transactions $e")
                emptyList<Transaction>()
            }
            val incomingTx = txs.filter { it.from != address && it.to == address }
            val outgoingTx = txs.filter { it.from == address && it.to != address }
            val toSelfTx = txs.filter { it.from == address && it.to == address }

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
        } catch (e: Exception) {
            false
        }
    }
}