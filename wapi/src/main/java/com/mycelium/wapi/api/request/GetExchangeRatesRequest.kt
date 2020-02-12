package com.mycelium.wapi.api.request

import com.fasterxml.jackson.annotation.JsonProperty

import java.io.Serializable

class GetExchangeRatesRequest(
        @param:JsonProperty @field:JsonProperty var version: Int,
        @param:JsonProperty @field:JsonProperty var fromCurrency: String,
        @param:JsonProperty @field:JsonProperty var toCurrency: String) : Serializable {
    override fun toString() = "$fromCurrency-$toCurrency"

    // For Jackson
    @Suppress("unused")
    constructor() : this(0, "", "")

    companion object {
        private const val serialVersionUID = 1L
    }
}
