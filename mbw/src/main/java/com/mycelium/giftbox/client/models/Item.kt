package com.mycelium.giftbox.client.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Item(
    val amount: String?,
    val client_order_id: String?,
    val items: List<SubItem>,
    val product_code: String?,
    val product_img: String?,
    val product_name: String?,
    val quantity: String?,
    val status: String?,
    val timestamp: Date?
) : Parcelable

@Parcelize
data class SubItem(
    val amount: String?,
    val code: String?,
    val delivery_url: String?,
    val expiry_date: Date?,
    val pin : String?
) : Parcelable