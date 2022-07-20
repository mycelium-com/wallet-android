package com.mycelium.wallet.activity.modern.event


interface BackHandler {
    fun addBackListener(listener: BackListener)
    fun removeBackListener(listener: BackListener)
}