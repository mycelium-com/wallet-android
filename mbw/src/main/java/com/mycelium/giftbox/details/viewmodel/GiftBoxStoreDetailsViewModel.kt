package com.mycelium.giftbox.details.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.client.models.CurrencyInfo
import com.mycelium.giftbox.client.models.ProductInfo


class GiftBoxStoreDetailsViewModel : ViewModel() {
    val description = MutableLiveData<String>()
    val amount = MutableLiveData<String>()
    val country = MutableLiveData<String>()
    val currency = MutableLiveData<String>()
    val expire = MutableLiveData<String>()
    val more = MutableLiveData<Boolean>(false)

    var productInfo: ProductInfo? = null
    var currencies: Array<CurrencyInfo>? = null

    fun setProduct(product: ProductInfo?) {
        productInfo = product
        description.value = product?.description
        currency.value = product?.currencyCode
        country.value = product?.countries?.mapNotNull {
            CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
        }?.joinToString { it.name }
        amount.value = "from ${product?.minimumValue} ${product?.currencyCode} to ${product?.maximumValue} ${product?.currencyCode}"
        expire.value = if (product?.expiryInMonths != null) "${product.expiryDatePolicy} (${product.expiryInMonths} months)" else "Does not expire"
    }
}