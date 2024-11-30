package com.mycelium.giftbox.purchase.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.model.MCOrderStatusResponse
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.common.OrderHeaderViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wapi.wallet.coins.toAssetInfo
import java.math.BigDecimal


class GiftboxBuyResultViewModel : ViewModel(), OrderHeaderViewModel {
    val totalAmountFiatString = MutableLiveData("")
    val totalAmountCryptoString = MutableLiveData("")
    val minerFeeFiat = MutableLiveData("")
    val minerFeeCrypto = MutableLiveData("")
    val more = MutableLiveData(true)
    val moreText = more.map {
        WalletApplication.getInstance().getString(
            if (it) {
                R.string.show_transaction_details
            } else {
                R.string.show_transaction_details_hide
            }
        )
    }


    override val productName = MutableLiveData("")
    override val expire = MutableLiveData("")
    override val country = MutableLiveData("")

    fun setProduct(product: MCProductInfo) {
        productName.value = product.name
//        expire.value = if (product.expiryInMonths != null) "${product.expiryDatePolicy} (${product.expiryInMonths} months)" else "Does not expire"
        country.value = product.countries?.mapNotNull {
            CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
        }?.joinToString { it.name }
    }

    override val cardValue = MutableLiveData("")
    override val quantity = MutableLiveData(0)

    fun setOrder(orderResponse: MCOrderResponse) {
        cardValue.value = "${orderResponse.faceValue?.stripTrailingZeros()?.toPlainString()} ${orderResponse.product?.currency}"
//        quantity.value = orderResponse.quantity?.toInt() ?: 0
        if (orderResponse.paymentAmount != null) {
            totalAmountFiatString.value =
                "${orderResponse.paymentAmount?.times(orderResponse.quantity)} ${orderResponse.paymentCurrency}"
        } else {
            totalAmountFiatString.value = cardValue.value
        }

//        var asset = orderResponse.pacurrencyFromInfo?.name?.toAssetInfo()
//        if (asset == null) {
//            asset = MbwManager.getInstance(WalletApplication.getInstance()).getWalletManager(false).getAssetTypes()
//                    .find { it.getCurrencyId().equals(orderResponse.currencyFromInfo?.name, true) }
//        }
//        totalAmountCryptoString.value =
//                asset?.value(orderResponse.amountExpectedFrom ?: "")?.toStringFriendlyWithUnit()
    }
}