package com.mycelium.giftbox.client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MCProductResponse(
    @JsonProperty("total_count")
    val count: Int,
    /* List of all categories */
    @JsonProperty("all_categories")
    var categories: List<String> = listOf(),
    /* List of all countries that have products in the catalog */
    @JsonProperty("all_countries")
    var countries: List<String> = emptyList(),
    @JsonProperty("items")
    val items: List<MCProductInfo>
)

