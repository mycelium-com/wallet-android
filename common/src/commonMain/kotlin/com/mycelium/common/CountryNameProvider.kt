package com.mycelium.common

/**
 * Platform-specific provider for country display names.
 * Uses native locale APIs on each platform to get localized country names.
 */
expect fun getCountryDisplayName(countryCode: String): String
