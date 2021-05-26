package com.mycelium.giftbox.client.models

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Item(
        val amount: String?,
        val client_order_id: String?,
        val items: List<SubItem>,
        val product_code: String?,
        val product_img: String?,
        @JsonProperty("product_name")
        val name: String?,
        val quantity: String?,
        val status: String?,
        val timestamp: Date?
) : Parcelable

@Parcelize
data class SubItem(
        val amount: String?,
        val code: String?,
        val delivery_url: String?,
        @JsonProperty("expiry_date")
        val expiryDate: Date?,
        val pin: String?
) : Parcelable