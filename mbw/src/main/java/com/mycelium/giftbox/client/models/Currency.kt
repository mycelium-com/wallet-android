package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Currency(
    val extraIdName: Any,
    val fullName: String?,
    val name: String?
) : Parcelable