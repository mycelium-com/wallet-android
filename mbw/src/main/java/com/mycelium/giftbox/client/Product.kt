package com.mycelium.giftbox.client

data class Product(
        val available_denominations: List<String>?,
        val card_image_url: String,
        val code: String,
        val name: String,
        val categories: List<String>?,
        val currency_code: String?,
        val domination_type: String?,
        val description: String?,
        val expiry_date_policy: String?,
        val maximum_value: String?,
        val minimum_value: String?,
        val redeem_instructions_html: String?
)
