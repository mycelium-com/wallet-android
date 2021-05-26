package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductsResponse(
    val categories: List<String>?,
    val countries: List<String>?,
    val products: List<Product>?,
    val size: Int?
) : Parcelable
