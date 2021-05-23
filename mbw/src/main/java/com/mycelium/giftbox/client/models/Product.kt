package com.mycelium.giftbox.client.models

data class Product(
        val available_denominations: List<String>?,
        val card_image_url: String?,
        val categories: List<String>?,
        val code: String?,
        val countries: List<String>?,
        val currency_code: String?,
        val denomination_type: String?,
        val description: String?,
        val expiry_date_policy: String?,
        val expiry_in_months: Int?,
        val maximum_value: String?,
        val minimum_value: String?,
        val name: String?,
        val redeem_instructions_html: String?,
        val terms_and_conditions_pdf_url: String?
)
