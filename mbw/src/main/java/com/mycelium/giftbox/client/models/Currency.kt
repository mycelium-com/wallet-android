package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Currency(
    val extraIdName: String?,
    val fullName: String?,
    val name: String?
) : Parcelable