package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CurrencyFromInfo(
    val extraIdName: Any,
    val fullName: String?,
    val name: String?
) : Parcelable