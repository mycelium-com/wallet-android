package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SYMBOL_COIN_MAP
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

object Util {
    /**
     * the method is used to remove additional characters indicating testnet coins from currencies' symbols
     * before making request to the server with these symbols as parameters, as server provides
     * exchange rates only by pure symbols, i.e. BTC and not tBTC
     */

    private var symbolList = arrayOf(
        "ZRX", "USDT", "USDC", "HT", "BUSD", "LEO", "TUSD", "LINK", "PAX", "ZB", "OKB", "OMG",
        "BAT", "BIX", "MCO", "STORJ", "GUSD", "KNC", "KNC", "SNT", "LAMB", "CHZ", "REP", "IOST",
        "ICX", "HUSD", "DATA", "LOOM", "MATIC", "MANA", "ELF", "DAI", "UQC", "HOT", "OGN", "TRB",
        "CHR", "CRO", "ENJ", "HEDG", "MKR", "HYN", "SNX", "KCS", "NMR", "CRPT", "LEND", "DGD",
        "QNT", "GNT", "MTL", "NEXO", "THETA", "BTM", "ANT", "SXP", "GNO", "CHSB", "FX", "SAI",
        "XIN", "REN", "CENNZ", "ABYSS", "ARN", "ATL", "ATLS", "CL", "DENT", "DCN", "POLY", "ENG",
        "RCN", "FXC", "RPL", "LA", "BRD", "XAUT", "MT", "FTM", "SHIB", "PAXG", "WBTC", "LRC", "CRV",
        "HEX", "SAND", "FTT", "AAVE", "UNI", "AXS", "RLC", "YFI", "GRT", "CVC", "GALA", "ILV",
        "SUSHI", "1INCH", "POWR", "BNT", "COMP", "RNDR", "DYDX", "ANKR", "XYO",
        "BTC", "ETH"
    )
    @JvmStatic
    fun trimTestnetSymbolDecoration(symbol: String): String =
        if (symbolList.contains(symbol.substring(1))) symbol.substring(1)
        else symbol

    @JvmStatic
    fun addTestnetSymbolDecoration(symbol: String, isTestnet: Boolean): String =
            if (isTestnet) {
                if (symbolList.contains(symbol)) "t$symbol"
                else symbol
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
}
