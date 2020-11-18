package com.mycelium.wapi.wallet.fio

import com.mycelium.net.HttpEndpoint
import com.mycelium.wapi.wallet.eth.ServerEthListChangedListener

object FioEndpoints : ServerEthListChangedListener{
    private var apiEndpoints: FioApiEndpoints = FioApiEndpoints(emptyList())
    private var historyEndpoints: FioHistoryEndpoints = FioHistoryEndpoints(emptyList())
    private var curApiEndpointIndex = 0
    private var curHistoryEndpointIndex = 0

    fun init(apiEndpoints: FioApiEndpoints, historyEndpoints: FioHistoryEndpoints) {
        this.apiEndpoints = apiEndpoints
        this.historyEndpoints = historyEndpoints
    }

    fun currentApiEndpoint() = apiEndpoints.endpoints[curApiEndpointIndex]
    fun currentHistoryEndpoint() = historyEndpoints.endpoints[curHistoryEndpointIndex]

    fun moveToNextApiEndpoint() {
        curApiEndpointIndex = (curApiEndpointIndex + 1) % apiEndpoints.endpoints.size
    }

    fun moveToNextHistoryEndpoint() {
        curHistoryEndpointIndex = (curHistoryEndpointIndex + 1) % historyEndpoints.endpoints.size
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        TODO("Not yet implemented")
    }
}