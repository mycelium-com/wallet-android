package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SYMBOL_COIN_MAP
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

object Util {
    /**
     * the method is used to remove additional characters indicating testnet coins from currencies' symbols
     * before making request to the server with these symbols as parameters, as server provides
     * exchange rates only by pure symbols, i.e. BTC and not tBTC
     */
    @JvmStatic
    fun trimTestnetSymbolDecoration(symbol: String): String =
            when (symbol) {
                "tBTC", "tBTCV", "tETH" -> symbol.substring(1)
                else -> symbol
            }

    @JvmStatic
    fun addTestnetSymbolDecoration(symbol: String, isTestnet: Boolean): String =
            if (isTestnet) {
                when (symbol) {
                    "BTC", "BTCV", "ETH" -> "t$symbol"
                    else -> symbol
                }
            } else {
                symbol
            }

    @JvmStatic
    fun getCoinByChain(networkParameters: NetworkParameters, symbol: String) =
            SYMBOL_COIN_MAP.filter {
                if (networkParameters.isProdnet) it.value.id.contains("main")
                else it.value.id.contains("test")
            }[symbol.toUpperCase(Locale.US)]

    @JvmStatic
    fun strToBigInteger(coinType: AssetInfo, amountStr: String): BigInteger =
                BigDecimal(amountStr).movePointRight(coinType.unitExponent).toBigIntegerExact()

    @JvmStatic
    fun valueToDouble(value: Value): Double = value.toPlainString().toDouble()

    @JvmStatic
    fun transformExpirationDate(dateStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val date = sdf.parse(dateStr)

        // val requiredSdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US) - old format
        // new format - September 20, 2021 at 6:23pm
        val requiredSdf = SimpleDateFormat("LLLL dd, yyyy 'at' hh:mm a", Locale.US)
        return requiredSdf.format(date)
    }

    @JvmStatic
    fun convertToDate(fioDateStr: String): Date {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.parse(fioDateStr)
    }

    /**
     * adds 365 to the given Date
     */
    @JvmStatic
    fun getRenewTill(expirationDate: Date): Date = Calendar.getInstance().apply {
        time = expirationDate
        add(Calendar.DAY_OF_MONTH, 365)
    }.time
}