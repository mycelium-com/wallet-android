package com.mycelium.common

import java.util.Locale

/**
 * Android implementation using java.util.Locale to get country display names.
 */
actual fun getCountryDisplayName(countryCode: String): String {
    return try {
        Locale("", countryCode.uppercase()).displayCountry.takeIf { it.isNotEmpty() }
            ?: countryCode
    } catch (e: Exception) {
        countryCode
    }
}
