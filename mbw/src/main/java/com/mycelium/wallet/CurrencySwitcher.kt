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
import com.mycelium.wapi.wallet.coinapult.Currency
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fiat.coins.FiatType

import java.util.ArrayList
import java.util.Collections

class CurrencySwitcher(private val exchangeRateManager: ExchangeRateManager, fiatCurrencies: Set<GenericAssetInfo>, currentCurrency: GenericAssetInfo, var denomination: Denomination) {

    private var fiatCurrencies: List<GenericAssetInfo>? = null
    var walletCurrencies: List<GenericAssetInfo>? = null

    // the last selected/shown fiat currency
    var currentFiatCurrency: GenericAssetInfo? = null
        private set

    // the last shown currency (usually same as fiat currency, but in some spots we cycle through all currencies including Bitcoin)
    var currentCurrency: GenericAssetInfo? = null
        private set

    var defaultCurrency: GenericAssetInfo = Utils.getBtcCoinType()

    val currentCurrencyIncludingDenomination: String
        get() = if (currentCurrency is FiatType || currentCurrency is Currency) {
            currentCurrency!!.symbol
        } else {
            denomination.getUnicodeString(currentCurrency!!.symbol)
        }

    init {
        val currencies = Lists.newArrayList(fiatCurrencies)
        currencies.sortWith(Comparator { cryptoCurrency, t1 -> cryptoCurrency.symbol.compareTo(t1.symbol) })
        this.fiatCurrencies = currencies
        this.currentCurrency = currentCurrency

        if (isFiatCurrency(currentCurrency)) {
            this.currentFiatCurrency = currentCurrency
        } else {
            this.currentFiatCurrency = currencies.firstOrNull()
        }
    }

    fun setCurrency(setToCurrency: GenericAssetInfo?) {
        if (isFiatCurrency(setToCurrency)) {
            currentFiatCurrency = setToCurrency
        }
        currentCurrency = setToCurrency
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
        if (!currencies.contains(currentFiatCurrency)) {
            if (currencies.isEmpty()) {
                //no fiat
                setCurrency(null)
            } else {
                setCurrency(currencies[0])
            }
        }
        //copy to prevent changes by caller
        this.fiatCurrencies = ArrayList(currencies)
    }

    fun getNextCurrency(includeBitcoin: Boolean): GenericAssetInfo? {
        val currencies = getCurrencyList()

        //just to be sure we dont cycle through a single one
        if (!includeBitcoin && currencies.size <= 1) {
            return currentFiatCurrency
        }

        var index = currencies.indexOf(currentCurrency)
        index++ //hop one forward

        if (index >= currencies.size) {
            // we are at the end of the fiat-list. return BTC if we should include Bitcoin, otherwise wrap around
            if (includeBitcoin) {
                // only set currentCurrency, but leave currentFiat currency as it was
                currentCurrency = defaultCurrency
            } else {
                index -= currencies.size //wrap around
                currentCurrency = currencies[index]
                currentFiatCurrency = currentCurrency
            }
        } else {
            currentCurrency = currencies[index]
            currentFiatCurrency = currentCurrency
        }

        exchangeRateManager.requestOptionalRefresh()

        return currentCurrency
    }

    fun isFiatExchangeRateAvailable(fromCurrency: String): Boolean {
        if (currentFiatCurrency == null) {
            // we dont even have a fiat currency...
            return false
        }

        // check if there is a rate available
        val rate = exchangeRateManager.getExchangeRate(fromCurrency, currentFiatCurrency!!.symbol)
        return rate?.price != null
    }


    fun getAsFiatValue(value: Value?): Value? {
        if (value == null) {
            return null
        }
        return if (currentFiatCurrency == null) {
            null
        } else exchangeRateManager.get(value, currentFiatCurrency!!)
    }

    /**
     * Get the exchange rate price for the currently selected currency.
     *
     *
     * Returns null if the current rate is too old or for a different currency.
     * In that the case the caller could choose to call refreshRates() and supply a handler to get a callback.
     */
    @Synchronized
    fun getExchangeRatePrice(fromCurrency: String): Double? {
        val rate = exchangeRateManager.getExchangeRate(fromCurrency, currentFiatCurrency!!.symbol)
        return rate?.price
    }

    fun getValue(sum: ValueSum): Value {
        // currentCurrency could be a cryptocurrency itself
        // don't bother then to get exchange rate, just sum
        if (currentCurrency!! == sum.values[0].type) {
            return sum.values.reduce { acc, value -> acc + value }
        }
        return sum.values.mapNotNull {
            exchangeRateManager.get(it, currentCurrency!!)
        }.takeIf { it.isNotEmpty() }?.reduce { acc, value -> acc + value }
                ?: Value.zeroValue(currentCurrency!!)
    }
}
