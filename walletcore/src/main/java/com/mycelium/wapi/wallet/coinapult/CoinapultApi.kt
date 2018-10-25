package com.mycelium.wapi.wallet.coinapult

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.coins.Balance
import java.math.BigDecimal


interface CoinapultApi {
    fun getTransactions(currency: Currency): List<CoinapultTransaction>?
    fun getBalance(currency: Currency): Balance?
    fun getAddress(currency: Currency, currentAddress: GenericAddress?): GenericAddress?
    fun broadcast(amount: BigDecimal,currency:Currency, address: BtcLegacyAddress)
}