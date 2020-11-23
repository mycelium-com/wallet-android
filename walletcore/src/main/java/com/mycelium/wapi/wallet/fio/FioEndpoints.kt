package com.mycelium.wapi.wallet.fio

import com.mycelium.net.HttpEndpoint
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.timerTask

object FioEndpoints : ServerFioApiListChangedListener, ServerFioHistoryListChangedListener {
    private var apiEndpoints: FioApiEndpoints = FioApiEndpoints(emptyList())
    private var historyEndpoints: FioHistoryEndpoints = FioHistoryEndpoints(emptyList())
    private var curApiEndpointIndex = 0
    private var curHistoryEndpointIndex = 0
    private val endpointsLock: ReentrantLock = ReentrantLock()

    private val ROTATE_ENDPOINT_TIME = TimeUnit.MINUTES.toMillis(5)
//    private val ROTATE_ENDPOINT_TIME = TimeUnit.SECONDS.toMillis(20)

    fun init(apiEndpoints: FioApiEndpoints, historyEndpoints: FioHistoryEndpoints) {
        this.apiEndpoints = apiEndpoints
        this.historyEndpoints = historyEndpoints
        curApiEndpointIndex = (Math.random() * this.apiEndpoints.endpoints.size).toInt()
        curHistoryEndpointIndex = (Math.random() * this.historyEndpoints.endpoints.size).toInt()

        Timer().apply {
            scheduleAtFixedRate(timerTask {
                rotateEndpoints()
            }, ROTATE_ENDPOINT_TIME, ROTATE_ENDPOINT_TIME)
        }
    }

    fun getCurrentApiEndpoint(): HttpEndpoint {
        return performEndpointsWork {
            apiEndpoints.endpoints[curApiEndpointIndex]
        }
    }

    fun getCurrentHistoryEndpoint(): HttpEndpoint {
        return performEndpointsWork {
            historyEndpoints.endpoints[curHistoryEndpointIndex]
        }
    }

    private fun moveToNextApiEndpoint() {
        curApiEndpointIndex = (curApiEndpointIndex + 1) % apiEndpoints.endpoints.size
    }

    private fun moveToNextHistoryEndpoint() {
        curHistoryEndpointIndex = (curHistoryEndpointIndex + 1) % historyEndpoints.endpoints.size
    }

    private fun rotateEndpoints() {
        performEndpointsWork {
            moveToNextApiEndpoint()
            moveToNextHistoryEndpoint()
        }
    }

    private inline fun <T> performEndpointsWork(work: () -> T): T {
        endpointsLock.lock()
        try {
            return work()
        } finally {
            endpointsLock.unlock()
        }
    }

    override fun apiServerListChanged(newEndpoints: Array<HttpEndpoint>) {
        performEndpointsWork {
            apiEndpoints.endpoints = newEndpoints.toList()
            curApiEndpointIndex = (Math.random() * this.apiEndpoints.endpoints.size).toInt()
        }
    }

    override fun historyServerListChanged(newEndpoints: Array<HttpEndpoint>) {
        performEndpointsWork {
            historyEndpoints.endpoints = newEndpoints.toList()
            curHistoryEndpointIndex = (Math.random() * this.historyEndpoints.endpoints.size).toInt()
        }
    }
}

interface ServerFioApiListChangedListener {
    fun apiServerListChanged(newEndpoints: Array<HttpEndpoint>)
}

interface ServerFioHistoryListChangedListener {
    fun historyServerListChanged(newEndpoints: Array<HttpEndpoint>)
}