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

package com.mycelium.wallet.activity.util

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout

import com.google.common.base.Preconditions
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.SelectedCurrencyChanged
import com.mycelium.wapi.wallet.currency.CurrencySum
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.toggleable_currency_display.view.*


open class ToggleableCurrencyDisplay : LinearLayout {
    protected val eventBus = MbwManager.getEventBus()
    protected val currencySwitcher by lazy { MbwManager.getInstance(context).currencySwitcher!! }

    private var currentValue: CurrencyValue? = null
    var fiatOnly = false
    protected var hideOnNoExchangeRate = false
    private var precision = -1

    private val valueToShow: CurrencyValue?
        get() = currencySwitcher.getAsFiatValue(currentValue)

    private var isAddedToBus = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
        parseXML(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
        parseXML(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun parseXML(context: Context, attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ToggleableCurrencyButton)

        for (i in 0 until a.indexCount) {
            val attr = a.getIndex(i)
            when (attr) {
                R.styleable.ToggleableCurrencyButton_fiatOnly -> fiatOnly = a.getBoolean(attr, false)
                R.styleable.ToggleableCurrencyButton_textSize -> setTextSize(a.getDimensionPixelSize(attr, 12))
                R.styleable.ToggleableCurrencyButton_textColor -> setTextColor(a.getColor(attr, resources.getColor(R.color.lightgrey)))
                R.styleable.ToggleableCurrencyButton_hideOnNoExchangeRate -> {
                    hideOnNoExchangeRate = a.getBoolean(attr, false)
                    precision = a.getInteger(attr, -1)
                }
                R.styleable.ToggleableCurrencyButton_precision -> precision = a.getInteger(attr, -1)
            }
        }
        a.recycle()
    }

    protected fun init(context: Context) {
        val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mInflater.inflate(R.layout.toggleable_currency_display, this, true)
    }

    private fun setTextSize(size: Int) {
        tvCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        tvDisplayValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
    }

    private fun setTextColor(color: Int) {
        tvCurrency.setTextColor(color)
        tvDisplayValue.setTextColor(color)
    }

    protected open fun updateUi() {
        Preconditions.checkNotNull(currencySwitcher)

        if (fiatOnly) {
            showFiat()
        } else {
            // Switch to BTC if no fiat fx rate is available
            if (!currencySwitcher.isFiatExchangeRateAvailable
                    && currencySwitcher.isFiatCurrency(currencySwitcher.currentCurrency)
                    && !currencySwitcher.isFiatCurrency(currencySwitcher.defaultCurrency)) {
                currencySwitcher.setCurrency(CurrencyValue.BTC)
            }

            visibility = View.VISIBLE
            val formattedValue = if (precision >= 0) {
                currencySwitcher.getFormattedValue(currentValue, false, precision)
            } else {
                currencySwitcher.getFormattedValue(currentValue, false)
            }

            tvDisplayValue.text = formattedValue
            val currentCurrency = currencySwitcher.currentCurrencyIncludingDenomination
            tvCurrency.text = currentCurrency
        }
    }

    private fun showFiat() {
        if (hideOnNoExchangeRate && !currencySwitcher.isFiatExchangeRateAvailable) {
            // hide everything
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            val formattedFiatValue: String

            // convert to the target fiat currency, if needed
            val valueToShow = valueToShow

            formattedFiatValue = if (precision >= 0) {
                currencySwitcher.getFormattedFiatValue(valueToShow, false, precision)
            } else {
                currencySwitcher.getFormattedFiatValue(valueToShow, false)
            }

            tvCurrency.text = currencySwitcher.currentFiatCurrency
            tvDisplayValue.text = formattedFiatValue
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAddedToBus = true
        eventBus.register(this)
        updateUi()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // unregister from the event bus
        if (isAddedToBus) {
            eventBus.unregister(this)
            isAddedToBus = false
        }
    }

    fun setValue(value: CurrencyValue) {
        this.currentValue = value
        updateUi()
    }

    fun setValue(sum: CurrencySum) {
        this.currentValue = currencySwitcher.getValueFromSum(sum)
        updateUi()
    }

    @Subscribe
    open fun onExchangeRateChange(event: ExchangeRatesRefreshed) = updateUi()

    @Subscribe
    open fun onSelectedCurrencyChange(event: SelectedCurrencyChanged) = updateUi()
}
