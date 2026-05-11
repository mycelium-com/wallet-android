package com.mycelium.common

import kotlinx.serialization.Serializable

@Serializable
data class CountryModel(
    val name: String, val acronym: String,
    val acronym3: String,
    val code: Int,
    val nationality: String? = null,
    val description: String? = null,
    val enabled: Boolean = true
)