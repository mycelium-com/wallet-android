package com.mycelium.wallet.external.partner.model

import com.fasterxml.jackson.annotation.JsonProperty


data class Partner(@JsonProperty("title") val title: String,
                   @JsonProperty("description") val description: String,
                   @JsonProperty("imageUrl") val imageUrl: String,
                   @JsonProperty("info") val info: String,
                   @JsonProperty("link") val link: String)