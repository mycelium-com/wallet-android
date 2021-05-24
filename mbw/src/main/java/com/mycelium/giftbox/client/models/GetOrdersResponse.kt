package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GetOrdersResponse(
    val items: List<Item>?,
    val size: Int?,
    val status: String?
) : Parcelable