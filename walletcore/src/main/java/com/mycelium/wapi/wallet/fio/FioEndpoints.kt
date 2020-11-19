package com.mycelium.wapi.wallet.fio

import com.mycelium.net.HttpEndpoint
import java.util.logging.Level
import java.util.logging.Logger

object FioEndpoints : ServerFioApiListChangedListener, ServerFioHistoryListChangedListener{
    private var apiEndpoints: FioApiEndpoints = FioApiEndpoints(emptyList())
    private var historyEndpoints: FioHistoryEndpoints = FioHistoryEndpoints(emptyList())
    private var curApiEndpointIndex = 0
    private var curHistoryEndpointIndex = 0

    fun init(apiEndpoints: FioApiEndpoints, historyEndpoints: FioHistoryEndpoints) {
        this.apiEndpoints = apiEndpoints
        this.historyEndpoints = historyEndpoints
        Logger.getLogger("asdaf").log(Level.WARNING, "inited endpoints. api: ${this.apiEndpoints.endpoints} \n " +
                "history: ${this.historyEndpoints.endpoints}")
    }

    fun currentApiEndpoint() = apiEndpoints.endpoints[curApiEndpointIndex]
    fun currentHistoryEndpoint() = historyEndpoints.endpoints[curHistoryEndpointIndex]

    fun moveToNextApiEndpoint() {
//        curApiEndpointIndex = (curApiEndpointIndex + 1) % apiEndpoints.endpoints.size
    }

    fun moveToNextHistoryEndpoint() {
        curHistoryEndpointIndex = (curHistoryEndpointIndex + 1) % historyEndpoints.endpoints.size
    }

    override fun apiServerListChanged(newEndpoints: Array<HttpEndpoint>) {
        apiEndpoints.endpoints = newEndpoints.toList()
        Logger.getLogger("asdaf").log(Level.WARNING, "got api servers changed event. api now: ${this.apiEndpoints.endpoints}");
    }

    override fun historyServerListChanged(newEndpoints: Array<HttpEndpoint>) {
        historyEndpoints.endpoints = newEndpoints.toList()
        Logger.getLogger("asdaf").log(Level.WARNING, "got history servers changed event. history now: ${this.historyEndpoints.endpoints}");
    }
}

interface ServerFioApiListChangedListener {
    fun apiServerListChanged(newEndpoints: Array<HttpEndpoint>)
}

interface ServerFioHistoryListChangedListener {
    fun historyServerListChanged(newEndpoints: Array<HttpEndpoint>)
}