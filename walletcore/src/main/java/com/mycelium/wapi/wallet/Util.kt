package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.math.BigDecimal
import java.math.BigInteger

object Util {
    /**
     * the method is used to remove additional characters indicating testnet coins from currencies' symbols
     * before making request to the server with these symbols as parameters, as server provides
     * exchange rates only by pure symbols, i.e. BTC and not tBTC
     */
    @JvmStatic
    fun trimTestnetSymbolDecoration(symbol: String): String {
        if (symbol == "tBTC") {
            return symbol.substring(1)
        }
        return if (symbol == "MTt") {
            symbol.substring(0, symbol.length - 1)
        } else symbol
    }

    @JvmStatic
    fun addTestnetSymbolDecoration(symbol: String): String {
        if (symbol == "BTC") {
            return "t$symbol"
        }
        return if (symbol == "MT") {
            symbol + "t"
        } else symbol
    }

    @JvmStatic
    fun getCoinsByChain(networkParameters: NetworkParameters): List<CryptoCurrency> {
        return COINS.values.filter {
            if (networkParameters.isProdnet) it.id.contains("main")
            else it.id.contains("test")
        }
    }

    @JvmStatic
    fun strToBigInteger(coinType: CryptoCurrency, amountStr: String): BigInteger =
            BigDecimal(amountStr).movePointRight(coinType.unitExponent).toBigIntegerExact()
}