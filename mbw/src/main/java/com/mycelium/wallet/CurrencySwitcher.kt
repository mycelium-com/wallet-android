/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet

import com.google.api.client.util.Lists
import com.mycelium.view.Denomination
import com.mycelium.wallet.exchange.ValueSum
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fiat.coins.FiatType

import java.util.ArrayList
import java.util.Collections


class CurrencySwitcher(private val exchangeRateManager: ExchangeRateManager,
                       fiatCurrencies: Set<GenericAssetInfo>,
                       currentTotalCurrency: GenericAssetInfo,
                       currentCurrencyMap: MutableMap<GenericAssetInfo, GenericAssetInfo?>,
                       currentFiatMap: MutableMap<GenericAssetInfo, GenericAssetInfo?>,
                       var denomination: Denomination) {

    private var fiatCurrencies: List<GenericAssetInfo>? = null
    var walletCurrencies: List<GenericAssetInfo>? = null
        set(walletAssetTypes) {
            field = walletAssetTypes
            // fills current currencies maps with default values
            walletAssetTypes?.filterNot { currentCurrencyMap.containsKey(it) }.orEmpty()
                    .forEach { asset ->
                        currentCurrencyMap[asset] = asset
                    }
            walletAssetTypes?.filterNot { currentFiatCurrencyMap.containsKey(it) }.orEmpty()
                    .forEach { asset ->
                        currentFiatCurrencyMap[asset] = FiatType(Constants.DEFAULT_CURRENCY)
                    }
        }

    // general fiat currency equals the last selected currency in total balance for all currency groups
    var currentTotalCurrency: GenericAssetInfo? = null

    // the last selected currency for each currency group that user has
    var currentCurrencyMap = mutableMapOf<GenericAssetInfo, GenericAssetInfo?>()
    var currentFiatCurrencyMap = mutableMapOf<GenericAssetInfo, GenericAssetInfo?>()

    var defaultCurrency: GenericAssetInfo = Utils.getBtcCoinType()

    init {
        val currencies = Lists.newArrayList(fiatCurrencies)
        currencies.sortWith(Comparator { cryptoCurrency, t1 -> cryptoCurrency.symbol.compareTo(t1.symbol) })
        this.fiatCurrencies = currencies
        this.currentCurrencyMap = currentCurrencyMap
        this.currentFiatCurrencyMap = currentFiatMap
        this.currentTotalCurrency = currentTotalCurrency
    }

    fun getCurrentCurrency(coinType: GenericAssetInfo): GenericAssetInfo? {
        return currentCurrencyMap[coinType]
    }

    fun getCurrentFiatCurrency(coinType: GenericAssetInfo): GenericAssetInfo? {
        return currentFiatCurrencyMap[coinType]
    }

    fun getCurrentCurrencyIncludingDenomination(coinType: GenericAssetInfo): String {
        return if (currentCurrencyMap[coinType] is FiatType) {
            currentCurrencyMap[coinType]!!.symbol
        } else {
            denomination.getUnicodeString(currentCurrencyMap[coinType]!!.symbol)
        }
    }

    fun setCurrency(coinType: GenericAssetInfo, setToCurrency: GenericAssetInfo?) {
        if (isFiatCurrency(setToCurrency)) {
            currentFiatCurrencyMap[coinType] = setToCurrency
        }
        currentCurrencyMap[coinType] = setToCurrency
    }

    fun isFiatCurrency(currency: GenericAssetInfo?): Boolean {
        return currency is FiatType
    }

    fun getCurrencyList(vararg additions: GenericAssetInfo): List<GenericAssetInfo> {
        //make a copy to prevent others from changing our internal list
        val result = ArrayList(fiatCurrencies!!)
        Collections.addAll(result, *additions)
        return result
    }

    fun setCurrencyList(fiatCurrencies: Set<GenericAssetInfo>) {
        // convert the set to a list and sort it
        val currencies = Lists.newArrayList(fiatCurrencies)
        currencies.sortWith(Comparator { abstractAsset, t1 -> abstractAsset.symbol.compareTo(t1.symbol) })

        //if we de-selected our current active currency, we switch it
        if (!currencies.contains(currentTotalCurrency)) {
            currentTotalCurrency = currencies.firstOrNull()
            currentCurrencyMap.filterValues { currCurrency -> !currencies.contains(currCurrency) }
                    .keys.forEach { coinType ->
                setCurrency(coinType, currencies.firstOrNull())
            }
        }

        //copy to prevent changes by caller
        this.fiatCurrencies = ArrayList(currencies)
    }

    fun getNextCurrency(includeBitcoin: Boolean): GenericAssetInfo? {
        val currencies = getCurrencyList()

        //just to be sure we dont cycle through a single one
        if (!includeBitcoin && currencies.size <= 1) {
            return currentFiatCurrencyMap[Utils.getBtcCoinType()]
        }

        var index = currencies.indexOf(currentCurrencyMap[Utils.getBtcCoinType()])
        index++ //hop one forward

        if (index >= currencies.size) {
            // we are at the end of the fiat-list. return BTC if we should include Bitcoin, otherwise wrap around
            if (includeBitcoin) {
                // only set currentCurrency, but leave currentFiat currency as it was
                currentCurrencyMap[Utils.getBtcCoinType()] = defaultCurrency
            } else {
                index -= currencies.size //wrap around
                currentCurrencyMap[Utils.getBtcCoinType()] = currencies[index]
                currentFiatCurrencyMap[Utils.getBtcCoinType()] = currentCurrencyMap[Utils.getBtcCoinType()]
            }
        } else {
            currentCurrencyMap[Utils.getBtcCoinType()] = currencies[index]
            currentFiatCurrencyMap[Utils.getBtcCoinType()] = currentCurrencyMap[Utils.getBtcCoinType()]
        }

        exchangeRateManager.requestOptionalRefresh()

        return currentCurrencyMap[Utils.getBtcCoinType()]
    }

    fun isFiatExchangeRateAvailable(fromCurrency: GenericAssetInfo): Boolean {
        if (currentFiatCurrencyMap[fromCurrency] == null) {
            // we dont even have a fiat currency...
            return false
        }

        // check if there is a rate available
        val rate = exchangeRateManager.getExchangeRate(fromCurrency.symbol, currentFiatCurrencyMap[fromCurrency]!!.symbol)
        return rate?.price != null
    }


    fun getAsFiatValue(value: Value?): Value? {
        if (isFiatCurrency(value?.type)) {
            return value
        }
        if (value == null) {
            return null
        }
        return if (currentFiatCurrencyMap[value.type] == null) {
            null
        } else exchangeRateManager.get(value, currentFiatCurrencyMap[value.type]!!)
    }

    /**
     * Get the exchange rate price for the currently selected currency.
     *
     *
     * Returns null if the current rate is too old or for a different currency.
     * In that case the caller could choose to call refreshRates() and supply a handler to get a callback.
     */
    @Synchronized
    fun getExchangeRatePrice(fromCurrency: GenericAssetInfo): Double? {
        val rate = exchangeRateManager.getExchangeRate(fromCurrency.symbol, currentFiatCurrencyMap[fromCurrency]?.symbol)
        return rate?.price
    }

    /**
     * Converts set of Values (generally consisting of different coin types)
     * represented by sum to the toCurrency and returns sum of converted values.
     */
    fun getValue(sum: ValueSum, toCurrency: GenericAssetInfo): Value {
        val distinctTypes = sum.values.distinctBy { it.type }

        if (distinctTypes.size == 1 && distinctTypes[0] == toCurrency) {
            return sum.values.reduce { acc, value -> acc + value }
        }
        return sum.values.mapNotNull {
            exchangeRateManager.get(it, toCurrency)
        }.takeIf { it.isNotEmpty() }?.reduce { acc, value -> acc + value }
                ?: Value.zeroValue(toCurrency)
    }
}
