package com.mycelium.giftbox.client.model

import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel

data class Products(
    val products: List<MCProductInfo>?,
    val categories: List<String>?,
    val countries: List<CountryModel>?
)