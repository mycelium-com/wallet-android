package com.mycelium.giftbox

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.wallet.WalletApplication


object GiftboxPreference {
    private val preference: SharedPreferences by lazy {
        WalletApplication.getInstance().getSharedPreferences("giftbox_main", Context.MODE_PRIVATE)
    }
    const val COUNTRIES_KEY = "country_set"
    const val LAST_PRODUCT_FETCH = "last_product_fetch"
    const val ALL_COUNTRIES_KEY = "all_countries"
    const val ALL_CATEGORIES_KEY = "all_categories"

    fun setGroupOpen(group: String, flag: Boolean) {
        preference.edit().putBoolean(group, flag).apply()
    }

    fun isGroupOpen(group: String): Boolean =
        preference.getBoolean(group, true)

    fun selectedCountries(): List<CountryModel> =
        preference.getStringSet(COUNTRIES_KEY, emptySet())?.mapNotNull {
            CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
        } ?: emptyList()

    fun setSelectedCountries(countries: List<CountryModel>) {
        preference.edit().putStringSet(COUNTRIES_KEY, countries.map { it.acronym }.toSet()).apply()
    }

    fun setLastProductFetch(date: Long) {
        preference.edit().putLong(LAST_PRODUCT_FETCH, date).apply()
    }

    fun productFetched() {
        setLastProductFetch(System.currentTimeMillis())
    }

    fun lastProductFetch(): Long = preference.getLong(LAST_PRODUCT_FETCH, 0)

    fun needFetchProducts(): Boolean =
        System.currentTimeMillis() - lastProductFetch() > 1000 * 60 * 60 * 24 // 24 hours

    fun setCountries(countries: List<String>) {
        preference.edit().putStringSet(ALL_COUNTRIES_KEY, countries.toSet()).apply()
    }

    fun getCountries(): List<String> =
        preference.getStringSet(ALL_COUNTRIES_KEY, emptySet())?.toList().orEmpty()

    fun setCategories(categories: List<String>) {
        preference.edit().putStringSet(ALL_CATEGORIES_KEY, categories.toSet()).apply()
    }

    fun getCategories(): List<String> =
        preference.getStringSet(ALL_CATEGORIES_KEY, emptySet())?.toList().orEmpty()
}