package com.mycelium.view

import java.math.BigInteger
import kotlin.math.pow


enum class Denomination(val scale: Int, private val asciiString: String, private val unicodeString: String,
                        private vararg val supportedBy: String) {
    UNIT(0, "", "", "BTC", "ETH", "FIO", "BTCV"),
    MILLI(3, "m", "m", "BTC", "ETH", "BTCV"),
    MICRO(6, "u", "\u00B5", "BTC", "ETH", "BTCV"),
    NANO(9, "SUF", "SUF", "FIO"),
    SATOSHI(8, "sats", "sats", "BTC", "BTCV"),
    FINNEY(3, "finney", "finney", "ETH"),
    SZABO(6, "szabo", "szabo", "ETH"),
    GWEI(9, "gwei", "gwei", "ETH"),
    MWEI(12, "mwei", "mwei", "ETH"),
    KWEI(15, "kwei", "kwei", "ETH"),
    WEI(18, "wei", "wei", "ETH"),
    KETHER(-3, "kether", "kether", "ETH"),
    METHER(-6, "mether", "mether", "ETH"),
    GETHER(-9, "gether", "gether", "ETH"),
    TETHER(-12, "tether", "tether", "ETH");

    fun getUnicodeString(symbol: String): String =
            when (this) {
                UNIT -> symbol
                MILLI, MICRO -> unicodeString + symbol
                else -> unicodeString
            }

    fun getAsciiString(symbol: String): String =
            when (this) {
                UNIT -> symbol
                MILLI, MICRO -> asciiString + symbol
                else -> asciiString
            }

    fun getAmount(value: BigInteger): BigInteger {
        return value / 10.0.pow(scale.toDouble()).toBigDecimal().toBigInteger()
    }

    fun supportedBy(coinType: String): Boolean {
        return this.supportedBy.contains(coinType)
    }

    companion object {
        @JvmStatic
        fun fromString(string: String): Denomination? {
            return when (string.toLowerCase()) {
                "btc"//back compatibility
                    , "unit" -> UNIT
                "mbtc"//back compatibility
                    , "milli" -> MILLI
                "ubtc" //back compatibility
                    , "micro" -> MICRO
                "satoshi" -> SATOSHI
                "nano" -> NANO
                "finney" -> FINNEY
                "szabo" -> SZABO
                "gwei" -> GWEI
                "mwei" -> MWEI
                "kwei" -> KWEI
                "wei" -> WEI
                "kether" -> KETHER
                "mether" -> METHER
                "gether" -> GETHER
                "tether" -> TETHER
                else -> null
            }
        }
    }
}