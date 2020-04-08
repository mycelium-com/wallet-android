package com.mycelium.bequant.market.model

const val MARKET_ITEM = 1
const val MARKET_TITLE_ITEM = 2

open class AdapterItem(val viewType: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdapterItem

        if (viewType != other.viewType) return false

        return true
    }

    override fun hashCode(): Int {
        return viewType
    }
}

//TODO correct variables class
class MarketItem(val currencies: String,
                 val volume: String,
                 val price: String,
                 val fiatPrice: String,
                 val change: String) : AdapterItem(MARKET_ITEM)

class MarketTitleItem(val sortBy: Int) : AdapterItem(MARKET_TITLE_ITEM)