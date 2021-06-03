package com.mycelium.giftbox.details.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.common.AmountViewModel
import com.mycelium.giftbox.common.DetailsViewModel
import com.mycelium.giftbox.getDateTimeString


class GiftBoxDetailsViewModel(application: Application) : AndroidViewModel(application), AmountViewModel, DetailsViewModel {
    val cardAmount = MutableLiveData<String>()
    val expireDate = MutableLiveData<String>()

    override val amount = MutableLiveData<String>()
    override val amountFiat = MutableLiveData<String>()
    override val minerFee = MutableLiveData<String>()
    override val date = MutableLiveData<String>()

    override val description = MutableLiveData<String>()
    override val expiry = MutableLiveData<String>()

    fun setOrder(order: OrderResponse) {
        cardAmount.value = "${order.amount} ${order.currencyCode}"
        expireDate.value = "Does not expire" //TODO find where we can get expire date
        amount.value = "${order.amount} ${order.currencyCode}"
        date.value = order.timestamp?.getDateTimeString(getApplication<Application>().resources)
    }

    fun setProduct(product: ProductResponse) {
        description.value = product.product?.description
        expiry.value = if (product.product?.expiryInMonths != null) "${product.product?.expiryDatePolicy} (${product.product?.expiryInMonths} months)" else "Does not expire"
    }
}