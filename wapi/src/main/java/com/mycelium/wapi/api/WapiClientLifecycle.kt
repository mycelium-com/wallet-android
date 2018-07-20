package com.mycelium.wapi.api

internal interface WapiClientLifecycle {
    fun setAppInForegroung(isInForeground: Boolean)
    fun setNetworkConnected(isNetworkConnected: Boolean)
}
