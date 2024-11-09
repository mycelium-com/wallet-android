package com.mycelium.giftbox.details.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.models.CurrencyInfo
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.getCardValue
import com.mycelium.giftbox.common.DescriptionViewModel


class GiftBoxStoreDetailsViewModel : ViewModel(), DescriptionViewModel {
    override val description = MutableLiveData<String>()
    override val more = MutableLiveData(false)
    override val moreVisible = MutableLiveData(false)
    override val termsLink = MutableLiveData<String?>()
    override val redeemInstruction = MutableLiveData<String?>()

    val amount = MutableLiveData<String>()
    val country = MutableLiveData<String>()
    val currency = MutableLiveData<String?>()
    val expire = MutableLiveData<String>()
    val productInfo = MutableLiveData<MCProductInfo?>()
    var currencies: Array<CurrencyInfo>? = null

    fun setProduct(product: MCProductInfo?) {
        productInfo.value = product
//        description.value = product?.description
        currency.value = product?.currency
//        termsLink.value = product?.termsAndConditionsPdfUrl
//        redeemInstruction.value = product?.redeemInstructionsHtml
        country.value = product?.countries?.mapNotNull {
            CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
        }?.joinToString { it.name }
        amount.value = product?.minFaceValue.toString()
//        expire.value = if (product?.expiryInMonths != null) "${product.expiryDatePolicy} (${product.expiryInMonths} months)" else "Does not expire"
    }
}