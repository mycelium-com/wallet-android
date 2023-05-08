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
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import java.util.ArrayList
import java.util.Collections


class CurrencySwitcher(private val exchangeRateManager: ExchangeRateManager,
                       fiatCurrencies: Set<AssetInfo>,
                       currentTotalCurrency: AssetInfo,
                       currentCurrencyMap: MutableMap<AssetInfo, AssetInfo?>,
                       currentFiatMap: MutableMap<AssetInfo, AssetInfo?>,
                       denominationMap: MutableMap<AssetInfo, Denomination>) {

    var denominationMap: MutableMap<AssetInfo, Denomination> = mutableMapOf()
    private var fiatCurrencies: List<AssetInfo>? = null
    var walletCurrencies: List<AssetInfo>? = null
        set(walletAssetTypes) {
            field = walletAssetTypes
            // sync maps with wallet current asset types
            // if new asset type has been added then add it to maps and init with default values
            walletAssetTypes?.forEach { asset ->
                if (!currentCurrencyMap.containsKey(asset)) {
                    currentCurrencyMap[asset] = asset
                }
                if (!currentFiatCurrencyMap.containsKey(asset)) {
                    currentFiatCurrencyMap[asset] = FiatType(Constants.DEFAULT_CURRENCY)
                }
                if (!denominationMap.containsKey(asset)) {
                    denominationMap[asset] = Denomination.UNIT
                }
            }

            // if walletAssetTypes doesn't contain an asset anymore, remove the asset from the maps too
            walletAssetTypes?.let {
                currentCurrencyMap.keys.filterNot { asset -> it.contains(asset) }
                        .forEach { asset ->
                            currentCurrencyMap.remove(asset)
                        }
                currentFiatCurrencyMap.keys.filterNot { asset -> it.contains(asset) }
                        .forEach { asset ->
                            currentFiatCurrencyMap.remove(asset)
                        }
                denominationMap.keys.filterNot { asset -> it.contains(asset) }
                        .forEach { asset ->
                            denominationMap.remove(asset)
                        }
            }
        }

    // general fiat currency equals the last selected currency in total balance for all currency groups
    var currentTotalCurrency: AssetInfo? = null

    // the last selected currency for each currency group that user has
    var currentCurrencyMap = mutableMapOf<AssetInfo, AssetInfo?>()
    var currentFiatCurrencyMap = mutableMapOf<AssetInfo, AssetInfo?>()

    var defaultCurrency: AssetInfo = Utils.getBtcCoinType()

    init {
        val currencies = Lists.newArrayList(fiatCurrencies)
        currencies.sortWith(Comparator { cryptoCurrency, t1 -> cryptoCurrency.symbol.compareTo(t1.symbol) })
        this.fiatCurrencies = currencies
        this.currentCurrencyMap = currentCurrencyMap
        this.currentFiatCurrencyMap = currentFiatMap
        this.currentTotalCurrency = currentTotalCurrency
        this.denominationMap = denominationMap
    }

    fun getCurrentCurrency(coinType: AssetInfo): AssetInfo {
        return currentCurrencyMap[coinType] ?: FiatType(Constants.DEFAULT_CURRENCY)
    }

    fun getCurrentFiatCurrency(coinType: AssetInfo): AssetInfo {
        return currentFiatCurrencyMap[coinType] ?: FiatType(Constants.DEFAULT_CURRENCY)
    }

    fun getDenomination(coinType: AssetInfo): Denomination = denominationMap[coinType] ?: Denomination.UNIT

    fun getCurrentCurrencyIncludingDenomination(coinType: AssetInfo): String {
        val currentCurrency = getCurrentCurrency(coinType)
        return if (currentCurrency is FiatType) {
            currentCurrency.symbol
        } else {
            getDenomination(coinType).getUnicodeString(currentCurrency.symbol)
        }
    }

    fun setCurrency(coinType: AssetInfo, setToCurrency: AssetInfo?) {
        if (isFiatCurrency(setToCurrency)) {
            currentFiatCurrencyMap[coinType] = setToCurrency
        }
        currentCurrencyMap[coinType] = setToCurrency
    }

    fun setDenomination(coinType: AssetInfo, denomination: Denomination) {
        denominationMap[coinType] = denomination
    }

    fun isFiatCurrency(currency: AssetInfo?): Boolean {
        return currency is FiatType
    }

    fun getCurrencyList(vararg additions: AssetInfo): List<AssetInfo> {
        //make a copy to prevent others from changing our internal list
        val result = ArrayList(fiatCurrencies!!)
        Collections.addAll(result, *additions)
        return result
    }

    fun setCurrencyList(fiatCurrencies: Set<AssetInfo>) {
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

    fun getNextCurrency(includeBitcoin: Boolean): AssetInfo? {
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

    fun isFiatExchangeRateAvailable(fromCurrency: AssetInfo): Boolean {
        if (currentFiatCurrencyMap[fromCurrency] == null) {
            // we dont even have a fiat currency...
            return false
        }

        // check if there is a rate available
        val rate = exchangeRateManager.getExchangeRate(fromCurrency.symbol, currentFiatCurrencyMap[fromCurrency]!!.symbol)
        return rate?.price != null
    }

    fun getAsFiatValue(value: Value?): Value? = when {
        value == null -> null
        isFiatCurrency(value.type) -> value
        currentFiatCurrencyMap[value.type] == null -> null
        else -> exchangeRateManager.get(value, currentFiatCurrencyMap[value.type]!!)
    }

    /**
     * Get the exchange rate price for the currently selected currency.
     *
     *
     * Returns null if the current rate is too old or for a different currency.
     * In that case the caller could choose to call refreshRates() and supply a handler to get a callback.
     */
    @Synchronized
    fun getExchangeRatePrice(fromCurrency: AssetInfo): Double? {
        val rate = exchangeRateManager.getExchangeRate(fromCurrency.symbol, getCurrentFiatCurrency(fromCurrency).symbol)
        return rate?.price
    }

    /**
     * Converts set of Values (generally consisting of different coin types)
     * represented by "sum" to the "toCurrency" and returns the sum of converted values.
     */
    fun getValue(sum: ValueSum, toCurrency: AssetInfo): Value {
        val distinctTypes = sum.values.distinctBy { it.type }

        // if all of the values are of the same type then just add up
        if (distinctTypes.size == 1 && distinctTypes[0].type == toCurrency) {
            return sum.values.reduce { acc, value -> acc + value }
        }
        return sum.values.mapNotNull {
            exchangeRateManager.get(it, toCurrency)
        }.takeIf { it.isNotEmpty() }?.reduce { acc, value -> acc + value }
                ?: Value.zeroValue(toCurrency)
    }
}
