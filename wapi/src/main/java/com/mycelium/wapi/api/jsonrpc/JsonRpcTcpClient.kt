package com.mycelium.wapi.api.jsonrpc

import com.mycelium.WapiLogger
import com.mycelium.wapi.api.exception.RpcResponseException
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask


typealias Consumer<T> = (T) -> Unit

data class TcpEndpoint(val host: String, val port: Int)

/*
    JsonRpcTcpClient is intended for JSON RPC communication. Its key design features:
    - Has a separate thread for maintaining connectivity and processing messages
    - Able to handle network state change using setActive()
    - Allows making write() calls from any thread
    - Ping messages are sent continuously in a separate thread each 5 seconds. If there is no response
      to ping message received, we consider it as a server slowdown and close the connection
    - After a new connection is established, those requests who has not been processed by previous connection
      will be resent using 'awaitingRequestsMap'
    - If connection thread is no longer active, all write() calls are forced to stop by notifying them
      by using 'waitingLatches' map
 */
open class JsonRpcTcpClient(private var endpoints : Array<TcpEndpoint>,
                            private val logger: WapiLogger) {
    private var curEndpointIndex = (Math.random() * endpoints.size).toInt()
    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory
    val isConnected = AtomicBoolean(false)

    /* isConnectionThreadActive is used to pause main connection thread
       when the device sent a notification about no network connected and resume its execution
       when the connection is back again
    */
    @Volatile private var isConnectionThreadActive = true
    @Volatile private var socket: Socket? = null
    @Volatile private var incoming : BufferedReader? = null
    @Volatile private var outgoing : BufferedOutputStream? = null
    private val nextRequestId = AtomicInteger(0)
    // Timer responsible for periodically executing ping requests
    private var pingTimer: Timer? = null
    // Stores requests waiting to be processed
    private val awaitingRequestsMap = ConcurrentHashMap<String, String>()
    private val awaitingLatches = ConcurrentHashMap<String, CountDownLatch>()
    private val callbacks = ConcurrentHashMap<String, Consumer<AbstractResponse>>()
    private val subscriptions = ConcurrentHashMap<String, Subscription>()

    // Determines whether main connection thread execution should be paused or resumed
    fun setActive(isActive: Boolean) {
        isConnectionThreadActive = isActive

        // Force all waiting write methods to stop
        if (!isConnectionThreadActive) {
            for (latch in awaitingLatches.values) {
                latch.countDown()
            }
        }
    }

    fun endpointsChanged(newEndpoints: Array<TcpEndpoint>) {
        if (!this.endpoints.contentEquals(newEndpoints)) {
            this.endpoints = newEndpoints
            curEndpointIndex = 0
            // Close current connection
            closeConnection()
        }
    }

    // Starts the main connection thread
    @Throws(IllegalStateException::class)
    fun start() {
        thread(start = true) {
            while(true) {
                if (!isConnectionThreadActive) {
                    logger.logInfo("Waiting until the connection is active again")
                    while (!isConnectionThreadActive) {
                        sleep(300)
                    }
                    logger.logInfo("The connection is active again, continue main connection thread loop")
                }
                val currentEndpoint = endpoints[curEndpointIndex]
                try {
                    logger.logInfo("Connecting to ${currentEndpoint.host}:${currentEndpoint.port}")

                    socket = ssf.createSocket().apply {
                        soTimeout = MAX_READ_RESPONSE_TIMEOUT.toInt()
                        connect(InetSocketAddress(currentEndpoint.host, currentEndpoint.port))
                        keepAlive = true
                        incoming = BufferedReader(InputStreamReader(getInputStream()))
                        outgoing = BufferedOutputStream(getOutputStream())
                    }
                    isConnected.set(true)
                    logger.logInfo("Connected to ${currentEndpoint.host}:${currentEndpoint.port}")

                    resendRemainingRequests()
                    notify("server.version", RpcParams.mapParams(
                            "client_name" to "wapi",
                            "protocol_version" to "1.4"))

                    // Schedule periodic ping requests execution
                    pingTimer = Timer().apply {
                        scheduleAtFixedRate(timerTask {
                            sendPingMessage()
                        }, 0, INTERVAL_BETWEEN_PING_REQUESTS)
                    }
                    if (subscriptions.isNotEmpty()) {
                        renewSubscriptions()
                    }

                    // Inner loop for reading data from socket. If the connection breaks, we should
                    // exit this loop and try creating new socket in order to restore connection
                    while (isConnected.get() && isConnectionThreadActive) {
                        val msgStart = CharArray(200)
                        incoming!!.mark(200)
                        if(incoming!!.read(msgStart) > 0) {
                            // Handle the case of empty line received having 'endline' char only
                            if (msgStart[0] == '\n') {
                                continue
                            }
                            incoming!!.reset()
                            messageReceived(msgStart.joinToString(""))
                        }
                    }
                } catch (exception: Exception) {
                    // Facing with the exception here means that connection is closed for any reason
                    logger.logInfo("Connection thread loop interrupted. Reason: ${exception.message}")
                }
                logger.logInfo("Connection to ${currentEndpoint.host}:${currentEndpoint.port} closed")

                //Close connection if it is still opened
                closeConnection()

                // Sleep for some time before moving to the next endpoint
                if (isConnectionThreadActive) {
                    sleep(INTERVAL_BETWEEN_SOCKET_RECONNECTS)
                }

                curEndpointIndex = (curEndpointIndex + 1) % endpoints.size
                pingTimer?.cancel()
            }
        }
    }

    fun notify(methodName: String, params: RpcParams) {
        val requestId = nextRequestId.getAndIncrement().toString()
        internalWrite(RpcRequestOut(methodName, params).apply {
            id = requestId
        }.toJson())
    }


    /*
        Re-sends to the new server those requests that have not been processed by previous connection
        Callbacks for these requests are still valid for new connection as well
        until the responses for them are processed
    */
    private fun resendRemainingRequests() {
        for (msg in awaitingRequestsMap.values) {
            val bytes = (msg + "\n").toByteArray()
            outgoing!!.write(bytes)
            outgoing!!.flush()
        }
    }

    private fun renewSubscriptions() {
        logger.logInfo("Subscriptions been renewed")
        val toRenew = subscriptions.toMutableMap()
        toRenew.forEach { subscribe(it.value) }
    }

    fun addSubscription(subscription: Subscription) {
        subscriptions[subscription.methodName] = subscription
    }

    fun subscribe(subscription: Subscription) {
        var requestId = nextRequestId.getAndIncrement().toString()
        val request = RpcRequestOut(subscription.methodName, subscription.params).apply {
            id = requestId
            callbacks[id.toString()] = subscription.callback
            addSubscription(subscription)
        }.toJson()
        internalWrite(request)
    }

    /**
     * Computes a "short" compound ID based on an array of IDs.
     */
    private fun compoundId(ids: Array<String>): String = ids.sortedArray().joinToString("")

    @Throws(RpcResponseException::class)
    fun write(requests: List<RpcRequestOut>, timeout: Long): BatchedRpcResponse {
        if (!waitForConnected(timeout)) {
            throw RpcResponseException("Timeout")
        }
        var response: BatchedRpcResponse? = null
        val latch = CountDownLatch(1)

        requests.forEach {
            it.id = nextRequestId.getAndIncrement().toString()
        }

        val compoundId = compoundId(requests.map {it.id.toString()}.toTypedArray())

        callbacks[compoundId] = {
            response = it as BatchedRpcResponse
            latch.countDown()
        }

        val batchedRequest = '[' + requests.joinToString { it.toJson() } + ']'
        if (!internalWrite(batchedRequest)) {
            callbacks.remove(compoundId)
            throw RpcResponseException("Write error")
        }

        awaitingRequestsMap[compoundId] = batchedRequest
        awaitingLatches[compoundId] = latch

        if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
            logger.logInfo("Couldn't get reply for $timeout milliseconds.")
            // No need to keep request data anymore as we're done with it
            removeCurrentRequestData(compoundId)

            awaitingLatches.remove(compoundId)
            throw RpcResponseException("Timeout")
        }

        awaitingLatches.remove(compoundId)

        // The case with response as NULL typically happens when wait() method is forced to stop
        // by calling latch.await() manually inside setActive(), not using callback
        if (response == null) {
            throw RpcResponseException("Request was cancelled")
        }

        return response!!
    }

    fun waitForConnected(timeout: Long): Boolean {
        var timeWatingForConnectedState = 0L
        while (!isConnected.get()) {
            sleep(WAITING_FOR_CONNECTED_INTERVAL)
            timeWatingForConnectedState += WAITING_FOR_CONNECTED_INTERVAL
            if (timeWatingForConnectedState >= timeout) {
                return false
            }
        }
        return true
    }

    @Throws(RpcResponseException::class)
    fun write(methodName: String, params: RpcParams, timeout: Long): RpcResponse {
        if (!waitForConnected(timeout)) {
            throw RpcResponseException("Timeout")
        }
        var response: RpcResponse? = null
        val latch = CountDownLatch(1)
        val request = RpcRequestOut(methodName, params).apply {
            id = nextRequestId.getAndIncrement().toString()
            callbacks[id.toString()] = {
                response = it as RpcResponse
                latch.countDown()
            }
        }
        val requestJson = request.toJson()
        val requestId = request.id.toString()
        if (!internalWrite(requestJson)) {
            callbacks.remove(requestId)
            throw RpcResponseException("Write error")
        }

        awaitingRequestsMap[requestId] = requestJson
        awaitingLatches[requestId] = latch

        if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
            logger.logInfo("Couldn't get reply on $methodName for $timeout milliseconds.")
            // No need to keep request data anymore as we're done with it
            removeCurrentRequestData(requestId)
            awaitingLatches.remove(requestId)
            throw RpcResponseException("Timeout")
        }

        awaitingLatches.remove(requestId)

        // The case with response as NULL typically happens when wait() method is forced to stop
        // by calling latch.await() manually inside setActive(), not using callback
        if (response == null) {
            throw RpcResponseException("Request was cancelled")
        }

        return response!!
    }

    private fun removeCurrentRequestData(requestId: String) {
        callbacks.remove(requestId)
        awaitingRequestsMap.remove(requestId)
    }

    private fun closeConnection() {
        if (isConnected.get()) {
            isConnected.set(false)
            socket?.close()
        }
    }

    private fun messageReceived(message: String) {
        if (message.contains("error")) {
            logger.logError(message)
        }
        val isBatched = message[0] == '['
        if (isBatched) {
            val response = BatchedRpcResponse.fromJson(incoming!!)
            val compoundId = compoundId(response.responses.map {it.id.toString()}.toTypedArray())

            callbacks.remove(compoundId)?.invoke(response)
            awaitingRequestsMap.remove(compoundId)
        } else {
            val response = RpcResponse.fromJson(incoming!!)
            val id = response.id.toString()
            if (id != NO_ID.toString()) {
                callbacks[id]?.also { callback ->
                    callback.invoke(response)
                }
            } else {
                subscriptions[response.method]?.apply {
                    callback.invoke(response)
                }
            }
            removeCurrentRequestData(id)
        }
    }

    // Send ping message. It is expected to be executed in the dedicated timer thread
    @Synchronized
    private fun sendPingMessage() {
        if (!isConnected.get())
            return

        var pong: RpcResponse? = null
        val latch = CountDownLatch(1)
        val request = RpcRequestOut("server.ping", RpcMapParams(emptyMap<String, String>())).apply {
            id = nextRequestId.getAndIncrement().toString()
            callbacks[id.toString()] = {
                pong = it as RpcResponse
                latch.countDown()
            }
        }
        val requestJson = request.toJson()
        val requestId = request.id.toString()
        if (!internalWrite(requestJson)) {
            callbacks.remove(requestId)
            return
        }

        if (!latch.await(MAX_PING_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS)) {
            logger.logInfo("Couldn't get reply on server.ping with id=$requestId for $MAX_PING_RESPONSE_TIMEOUT milliseconds. Forcing connection to be closed")
            // Force socket close. It will cause reconnect to another server
            closeConnection()
            return
        }
        logger.logInfo("Pong! $pong")
    }


    @Synchronized
    private fun internalWrite(msg: String): Boolean {
        var result = true

        if (!isConnected.get())
            return false

        try {
            val bytes = (msg + "\n").toByteArray()
            outgoing!!.write(bytes)
            outgoing!!.flush()
        } catch (ex: Exception) {
            result = false
            closeConnection()
        }
        return result
    }

    companion object {
        private val INTERVAL_BETWEEN_SOCKET_RECONNECTS = TimeUnit.SECONDS.toMillis(1)
        private val INTERVAL_BETWEEN_PING_REQUESTS = TimeUnit.SECONDS.toMillis(10)
        private val MAX_PING_RESPONSE_TIMEOUT = TimeUnit.SECONDS.toMillis(10)
        private val MAX_READ_RESPONSE_TIMEOUT = TimeUnit.SECONDS.toMillis(20)
        private val WAITING_FOR_CONNECTED_INTERVAL = 300L
    }
}
