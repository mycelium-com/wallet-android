package com.mycelium.wapi.api

internal interface WapiClientLifecycle {
    fun setAppInForeground(isInForeground: Boolean)
    fun setNetworkConnected(isNetworkConnected: Boolean)
}
