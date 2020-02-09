package com.mycelium.wapi.api

internal interface WapiClientLifecycle {
    fun setNetworkConnected(isNetworkConnected: Boolean)
}
