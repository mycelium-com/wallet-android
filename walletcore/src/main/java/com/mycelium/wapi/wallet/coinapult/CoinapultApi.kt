package com.mycelium.wapi.wallet.coinapult


interface CoinapultApi {
    fun getTransactions(): List<CoinapultTransaction>
}