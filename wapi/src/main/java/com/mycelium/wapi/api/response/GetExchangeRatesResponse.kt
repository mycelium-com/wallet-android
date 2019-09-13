package com.mycelium.wapi.api.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.mycelium.wapi.model.ExchangeRate

import java.io.Serializable

class GetExchangeRatesResponse(
        @param:JsonProperty
        @field:JsonProperty
        val fromCurrency: String,
        @param:JsonProperty
        @field:JsonProperty
        val toCurrency: String,
        @param:JsonProperty @field:JsonProperty
        val exchangeRates: Array<ExchangeRate>) : Serializable {

    constructor() : this("", "", emptyArray())

    override fun toString(): String {
        return "$fromCurrency-$toCurrency"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
