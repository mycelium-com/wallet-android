package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductResponse(
    val priceCurrency: String?,
    val priceOffer: String?,
    val product: Product?,
    val similarProducts: List<Product>?
) : Parcelable
