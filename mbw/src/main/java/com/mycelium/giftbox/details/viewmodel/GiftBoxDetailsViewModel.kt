package com.mycelium.giftbox.details.viewmodel

import android.app.Application
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mycelium.giftbox.client.models.Ecode
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.common.AmountViewModel
import com.mycelium.giftbox.common.DescriptionViewModel
import com.mycelium.giftbox.getDateTimeString


class GiftBoxDetailsViewModel(application: Application) : AndroidViewModel(application), AmountViewModel, DescriptionViewModel {
    val cardAmount = MutableLiveData<String>()
    val expireDate = MutableLiveData<String>()

    val redeemCode = MutableLiveData<String>("")
    val pinCode = MutableLiveData<String>("")

    override val amount = MutableLiveData<String>()
    override val amountFiat = MutableLiveData<String>()
    override val minerFee = MutableLiveData<String>()
    override val date = MutableLiveData<String>()

    override val description = MutableLiveData<String>()
    override val more = MutableLiveData<Boolean>(false)
    val expiry = MutableLiveData<String>()
    var productInfo: ProductInfo? = null

    fun setOrder(order: OrderResponse) {
        cardAmount.value = "${order.amount} ${order.currencyCode}"
        amount.value = "${order.amount} ${order.currencyCode}"
        date.value = order.timestamp?.getDateTimeString(getApplication<Application>().resources)
    }

    fun setProduct(product: ProductResponse) {
        productInfo = product.product
        description.value = product.product?.description
        expiry.value = if (product.product?.expiryInMonths != null) "${product.product?.expiryDatePolicy} (${product.product?.expiryInMonths} months)" else "Does not expire"
        expireDate.value = expiry.value
    }

    fun setCodes(code: Ecode) {
        when {
            code.deliveryUrl?.isNotEmpty() == true -> {
                redeemCode.value = code.deliveryUrl
            }
            URLUtil.isValidUrl(code.code) -> {
                redeemCode.value = code.code
            }
            code.code?.isNotEmpty() == true -> {
                redeemCode.value = code.code
            }
            else -> {
                redeemCode.value = ""
            }
        }
        pinCode.value = if (code.pin?.isNullOrEmpty() == true) {
            code.pin
        } else {
            ""
        }
    }
}