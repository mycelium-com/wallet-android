package com.mycelium.giftbox.client

object GitboxAPI {
    val giftRepository by lazy { GiftboxApiRepository() }
    val mcGiftRepository by lazy { MCGiftboxApiRepository() }
}