package com.mycelium.giftbox.details.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mycelium.giftbox.client.models.GetOrderResponse
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

    fun setOrder(order: GetOrderResponse) {
        cardAmount.value = "${order.amount} ${order.currency_code}"
        expireDate.value = "Does not expire" //TODO find where we can get expire date
        amount.value = "${order.amount} ${order.currency_code}"
        date.value = order.timestamp?.getDateTimeString(getApplication<Application>().resources)
    }

    fun setProduct(product: ProductResponse) {
        description.value = product.product?.description
    }
}