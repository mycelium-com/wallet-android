package com.mycelium.common

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode

/**
 * iOS implementation using NSLocale to get country display names.
 */
actual fun getCountryDisplayName(countryCode: String): String {
    return try {
        NSLocale.currentLocale.localizedStringForCountryCode(countryCode.uppercase())
            ?: countryCode
    } catch (e: Exception) {
        countryCode
    }
}
