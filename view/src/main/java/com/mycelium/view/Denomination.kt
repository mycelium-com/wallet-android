package com.mycelium.view


enum class Denomination(val base10: Int, val asciiString: String, val unicodeString: String) {
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

    companion object {
        fun fromString(string: String): Denomination {
            return when (string) {
                "BTC"//back compatibility
                    , "unit" -> UNIT
                "mBTC"//back compatibility
                    , "milli" -> MILLI
                "uBTC" //back compatibility
                    , "micro" -> MICRO
                "bits" -> BITS
                else -> UNIT
            }
        }
    }
}