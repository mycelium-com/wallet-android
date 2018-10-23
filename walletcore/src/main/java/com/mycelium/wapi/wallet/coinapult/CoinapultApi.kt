package com.mycelium.wapi.wallet.coinapult

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.Balance


interface CoinapultApi {
    fun getTransactions(currency: Currency): List<CoinapultTransaction>?
    fun getBalance(currency: Currency): Balance?
    fun getAddress(currency: Currency, currenctAddress: GenericAddress?): GenericAddress?
}