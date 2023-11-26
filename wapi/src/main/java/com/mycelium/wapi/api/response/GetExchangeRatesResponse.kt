package com.mycelium.wapi.api.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.mycelium.wapi.model.ExchangeRate

import java.io.Serializable

class GetExchangeRatesResponse(
        @param:JsonProperty @field:JsonProperty val fromCurrency: String,
        @param:JsonProperty @field:JsonProperty val toCurrency: String,
        @param:JsonProperty @field:JsonProperty val exchangeRates: List<ExchangeRate>) : Serializable {
    override fun toString() = "$fromCurrency-$toCurrency(${exchangeRates.size} rates)"

    // For Jackson
    @Suppress("unused")
    constructor() : this("", "", emptyList())

    companion object {
        private const val serialVersionUID = 1L
    }
}
