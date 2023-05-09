package com.mycelium.wapi.api.jsonrpc

import com.mrd.bitlib.util.SslUtils
import com.mycelium.wapi.api.exception.RpcResponseException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask
import kotlin.coroutines.resume


typealias Consumer<T> = (T) -> Unit

data class TcpEndpoint(val host: String, val port: Int, var useSsl: Boolean = true)

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
open class JsonRpcTcpClient(private var endpoints : Array<TcpEndpoint>, androidApiVersion: Int) {
    private val logger = Logger.getLogger(JsonRpcTcpClient::class.java.simpleName)
    private var curEndpointIndex = (Math.random() * endpoints.size).toInt()
    private val ssf = if (androidApiVersion < 22) SslUtils.getSsLSocketFactory(ELECTRUMX_THUMBPRINT)
                        else SSLSocketFactory.getDefault() as SSLSocketFactory
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
    private var previousRequestsMap = mutableMapOf<String, String>()
    // Stores requests waiting to be processed
    private val awaitingRequestsMap = ConcurrentHashMap<String, String>()
    private val callbacks = ConcurrentHashMap<String, Consumer<AbstractResponse>>()
    private val continuations = ConcurrentHashMap<String, CancellableContinuation<AbstractResponse>>()
    private val subscriptions = ConcurrentHashMap<String, Subscription>()

    // Determines whether main connection thread execution should be paused or resumed
    fun setActive(isActive: Boolean) {
        logger.log(Level.INFO, "Tcp client's connected state is updated: ${isActive}")
        isConnectionThreadActive = isActive

        // Force all waiting write methods to stop
        if (!isConnectionThreadActive) {
            continuations.values.forEach { it.takeIf { !it.isCompleted && !it.isCancelled }?.cancel() }
        }
    }

    fun endpointsChanged(newEndpoints: Array<TcpEndpoint>) {
        if (!this.endpoints.contentEquals(newEndpoints)) {
            this.endpoints = newEndpoints
            curEndpointIndex = 0
            thread {
                // Close current connection
                closeConnection()
            }
        }
    }

    // Starts the main connection thread
    @Throws(IllegalStateException::class)
    fun start() {
        thread(start = true) {
            while(true) {
                if (!isConnectionThreadActive) {
                    logger.log(Level.INFO, "Waiting until the connection is active again")
                    while (!isConnectionThreadActive) {
                        sleep(300)
                    }
                    logger.log(Level.INFO, "The connection is active again, continue main connection thread loop")
                }
                val currentEndpoint = endpoints[curEndpointIndex]
                try {
                    logger.log(Level.INFO, "Connecting to ${currentEndpoint.host}:${currentEndpoint.port}")
                    socket = when {
                        currentEndpoint.host.endsWith(".onion") ->
                            Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050)))
                        currentEndpoint.useSsl -> ssf.createSocket()
                        else -> Socket()
                    }.apply {
                        soTimeout = MAX_READ_RESPONSE_TIMEOUT.toInt()
                        connect(InetSocketAddress(currentEndpoint.host, currentEndpoint.port))
                        keepAlive = true
                        incoming = BufferedReader(InputStreamReader(getInputStream()))
                        outgoing = BufferedOutputStream(getOutputStream())
                    }
                    isConnected.set(true)
                    logger.log(Level.INFO, "Connected to ${currentEndpoint.host}:${currentEndpoint.port}")

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
                        val line = incoming!!.readLine()
                        if(line?.isEmpty() != false) {
                            continue
                        }
                        messageReceived(line)
                    }
                } catch (exception: Exception) {
                    // Facing with the exception here means that connection is closed for any reason
                    logger.log(Level.INFO, "Connection thread loop interrupted. Reason: ${exception.message}")
                }
                logger.log(Level.INFO, "Connection to ${currentEndpoint.host}:${currentEndpoint.port} closed")

                //Close connection if it is still opened
                closeConnection()

                //Finish ping timer thread execution
                pingTimer?.cancel()

                // Sleep for some time before moving to the next endpoint
                if (isConnectionThreadActive) {
                    sleep(INTERVAL_BETWEEN_SOCKET_RECONNECTS)
                }

                curEndpointIndex = (curEndpointIndex + 1) % endpoints.size

                previousRequestsMap.clear()
                previousRequestsMap.putAll(awaitingRequestsMap)
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
        for (msg in previousRequestsMap.values) {
            internalWrite(msg)
        }
    }

    private fun renewSubscriptions() {
        logger.log(Level.INFO, "Subscriptions been renewed")
        val toRenew = subscriptions.toMutableMap()
        toRenew.forEach { subscribe(it.value) }
    }

    // Adds new a subscription to subscriptions map
    fun addSubscription(subscription: Subscription) {
        subscriptions[subscription.methodName] = subscription
    }

    private fun subscribe(subscription: Subscription) {
        val requestId = nextRequestId.getAndIncrement().toString()
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
    private fun compoundId(ids: List<String>): String = ids.min() ?: ""

    fun cancel(requests: List<RpcRequestOut>) {
        val compoundId = compoundId(requests.map { it.id.toString() })
        removeCurrentRequestData(compoundId)
    }

    @Throws(RpcResponseException::class)
    suspend fun write(requests: List<RpcRequestOut>, timeout: Long): BatchedRpcResponse {
        if (!waitForConnected(timeout)) {
            throw RpcResponseException("Timeout")
        }
        requests.forEach {
            it.id = nextRequestId.getAndIncrement().toString()
        }
        val compoundId = compoundId(requests.map { it.id.toString() })
        val batchedRequest = '[' + requests.joinToString { it.toJson() } + ']'
        val response = writeAndWait(timeout, compoundId, batchedRequest)
        return response as BatchedRpcResponse
    }

    private suspend fun writeAndWait(timeout: Long, requestId: String, request: String): AbstractResponse? {
        var response: AbstractResponse? = null
        withTimeoutOrNull(timeout) {
            response = suspendCancellableCoroutine { continuation: CancellableContinuation<AbstractResponse?> ->
                continuations[requestId] = continuation
                if (!internalWrite(request)) {
                    continuations.remove(requestId)
                    throw RpcResponseException("Write error")
                }
                awaitingRequestsMap[requestId] = request
            } // The case with response as NULL typically happens when wait() method is forced to stop
                    // by calling latch.await() manually inside setActive(), not using callback
                    ?: throw RpcResponseException("Request was cancelled")
        } ?: kotlin.run {
            logger.log(Level.INFO, "Couldn't get reply for $timeout milliseconds.")
            // No need to keep request data anymore as we're done with it
            removeCurrentRequestData(requestId)
            throw RpcResponseException("Timeout")
        }
        return response
    }

    private suspend fun waitForConnected(timeout: Long): Boolean =
        withTimeoutOrNull(timeout) {
            while (!isConnected.get()) {
                delay(WAITING_FOR_CONNECTED_INTERVAL)
            }
            true
        } ?: false

    @Throws(RpcResponseException::class)
    suspend fun write(methodName: String, params: RpcParams, timeout: Long): RpcResponse {
        if (!waitForConnected(timeout)) {
            throw RpcResponseException("Timeout")
        }
        val request = RpcRequestOut(methodName, params).apply {
            id = nextRequestId.getAndIncrement().toString()
        }
        val requestJson = request.toJson()
        val requestId = request.id.toString()
        val response = writeAndWait(timeout, requestId, requestJson)
        return response as RpcResponse
    }

    private fun removeCurrentRequestData(requestId: String) {
        awaitingRequestsMap.remove(requestId)
        continuations.remove(requestId)?.takeIf { !it.isCompleted && !it.isCancelled }?.cancel()
    }

    private fun closeConnection() {
        if (isConnected.get()) {
            isConnected.set(false)
            socket?.close()
        }
    }

    private fun messageReceived(message: String) {
        if (message.contains("error")) {
            logger.log(Level.SEVERE, message)
        }
        val isBatched = message[0] == '['
        if (isBatched) {
            val response = BatchedRpcResponse.fromJson(message)
            val compoundId = compoundId(response.responses.map {it.id.toString()})
            continuations.remove(compoundId)?.resume(response)
            awaitingRequestsMap.remove(compoundId)
        } else {
            val response = RpcResponse.fromJson(message)
            val id = response.id.toString()
            continuations.remove(id)?.resume(response)
            awaitingRequestsMap.remove(id)
            if (id != NO_ID.toString()) {
                callbacks[id]?.also { callback ->
                    callback.invoke(response)
                }
            } else {
                subscriptions[response.method]?.apply {
                    callback.invoke(response)
                }
            }
        }
    }

    // Send ping message. It is expected to be executed in the dedicated timer thread
    private fun sendPingMessage() {
        if (!isConnected.get())
            return

        val request = RpcRequestOut("server.ping", RpcMapParams(emptyMap<String, String>())).apply {
            id = nextRequestId.getAndIncrement().toString()
        }

        internalWrite(request.toJson())
    }


    @Synchronized
    private fun internalWrite(msg: String): Boolean {
        var result = true

        if (!isConnected.get()) {
            return false
        }

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
        private val MAX_READ_RESPONSE_TIMEOUT = TimeUnit.SECONDS.toMillis(30)
        private const val WAITING_FOR_CONNECTED_INTERVAL = 300L
        private const val ELECTRUMX_THUMBPRINT = "0D:31:88:C6:35:16:2C:72:7C:54:0C:24:58:DA:62:A9:C1:E5:E0:CD"
    }
}
