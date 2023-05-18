package com.mycelium.giftbox.purchase.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
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
    val moreText = Transformations.map(more) {
        WalletApplication.getInstance().getString(
                if (it) {
                    R.string.show_transaction_details
                } else {
                    R.string.show_transaction_details_hide
                })
    }


    override val productName = MutableLiveData("")
    override val expire = MutableLiveData("")
    override val country = MutableLiveData("")

    fun setProduct(product: ProductInfo) {
        productName.value = product.name
        expire.value = if (product.expiryInMonths != null) "${product.expiryDatePolicy} (${product.expiryInMonths} months)" else "Does not expire"
        country.value = product.countries?.mapNotNull {
            CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
        }?.joinToString { it.name }
    }

    override val cardValue = MutableLiveData("")
    override val quantity = MutableLiveData(0)

    fun setOrder(orderResponse: OrderResponse) {
        val cardAmount = (orderResponse.amount?.toBigDecimal() ?: BigDecimal.ZERO)
        cardValue.value = "${cardAmount.stripTrailingZeros().toPlainString()} ${orderResponse.currencyCode}"
        quantity.value = orderResponse.quantity?.toInt() ?: 0
        totalAmountFiatString.value = "${orderResponse.amount?.toBigDecimal()?.times(orderResponse.quantity!!)} ${orderResponse.currencyCode}"
        var asset = orderResponse.currencyFromInfo?.name?.toAssetInfo()
        if (asset == null) {
            asset = MbwManager.getInstance(WalletApplication.getInstance()).getWalletManager(false).getAssetTypes()
                    .find { it.symbol.equals(orderResponse.currencyFromInfo?.name, true) }
        }
        totalAmountCryptoString.value =
                asset?.value(orderResponse.amountExpectedFrom ?: "")?.toStringFriendlyWithUnit()
    }
}