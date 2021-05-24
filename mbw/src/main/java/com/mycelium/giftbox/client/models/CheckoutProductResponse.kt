package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CheckoutProductResponse(
    val currencies: List<Currency>?,
    val fromCurrencyId: String?,
    val priceOffer: String?,
    val product: Product?
) : Parcelable