package com.mycelium.view


enum class Denomination(val scale: Int, val asciiString: String, val unicodeString: String) {
    UNIT(0, "", ""),
    MILLI(3, "m", "m"),
    MICRO(6, "u", "\u00B5"),
    BITS(6, "bits", "bits");

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

    fun getAmount(value: Long): Long {
        return value / Math.pow(10.0, scale.toDouble()).toLong()
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
                "bits" -> BITS
                else -> null
            }
        }
    }
}