package com.mycelium.wapi.api.jsonrpc

import com.mycelium.WapiLogger
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask
import kotlin.system.measureTimeMillis

typealias Consumer<T> = (T) -> Unit

data class TcpEndpoint(val host: String, val port: Int)

open class JsonRpcTcpClient(private val endpoints : Array<TcpEndpoint>,
                            val logger: WapiLogger) {
    private var curEndpointIndex = (Math.random() * endpoints.size).toInt()
    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory

    var isConnected = AtomicBoolean(false)
    @Volatile var lastSuccessTime = System.currentTimeMillis()
    @Volatile private var isStopped = false
    @Volatile private var socket: Socket? = null
    @Volatile private var incoming : BufferedReader? = null
    @Volatile private var outgoing : BufferedOutputStream? = null
    private val nextRequestId = AtomicInteger(0)
    private val isStarted = AtomicBoolean(false)
    // Timer responsible for periodically executing ping requests
    private var pingTimer: Timer? = null
    @Volatile private var currentResponceTimeout = SMALL_RESPONSE_TIMEOUT
    @Volatile private var connectionAttempt = 0

    private val callbacks = mutableMapOf<String, Consumer<AbstractResponse>>()
    private val subscriptions = mutableMapOf<String, Subscription>()

    @Throws(IllegalStateException::class)
    fun start() {
        if (isStarted.getAndSet(true)) {
            throw IllegalStateException("RPC client could not be started twice.")
        }
        thread(start = true) {
            while(!isStopped) {
                val currentEndpoint = endpoints[curEndpointIndex]
                try {
                    synchronized(this) {
                        socket = ssf.createSocket().apply {
                            soTimeout = currentResponceTimeout.toInt()
                            connect(InetSocketAddress(currentEndpoint.host, currentEndpoint.port))
                            keepAlive = true
                            incoming = BufferedReader(InputStreamReader(getInputStream()))
                            outgoing = BufferedOutputStream(getOutputStream())
                        }
                        callbacks.clear()
                        notify("server.version", RpcParams.mapParams(
                                "client_name" to "wapi",
                                "protocol_version" to "1.4"))
                        logger.logInfo("Connected to ${currentEndpoint.host}:${currentEndpoint.port}")
                        isConnected.set(true)
                        connectionAttempt = 0

                        // Schedule periodic ping requests execution
                        pingTimer = Timer().apply {
                            scheduleAtFixedRate(timerTask {
                                sendPingMessage()
                            }, 0, INTERVAL_BETWEEN_PING_REQUESTS)
                        }
                        if (subscriptions.isNotEmpty()) {
                            renewSubscriptions()
                        }
                    }

                    // Inner loop for reading data from socket. If the connection breaks, we should
                    // exit this loop and try creating new socket in order to restore connection
                    while (isConnected.get()) {
                        val msgStart = CharArray(200)
                        incoming!!.mark(200)
                        if(incoming!!.read(msgStart) > 0) {
                            incoming!!.reset()
                            messageReceived(msgStart.joinToString(""))
                        }
                    }
                } catch (exception: Exception) {
                    logger.logError("Socket creation or receiving failed: ${exception.message} - ${currentEndpoint.host}:${currentEndpoint.port}")
                }
                close()
                isConnected.set(false)
                // Sleep for some time before moving to the next endpoint
                Thread.sleep(INTERVAL_BETWEEN_SOCKET_RECONNECTS)
                if (curEndpointIndex + 1 == endpoints.size && connectionAttempt < 3) {
                    connectionAttempt++
                }
                curEndpointIndex = (curEndpointIndex + 1) % endpoints.size
                currentResponceTimeout = calculateNewTimeout(connectionAttempt)
                pingTimer?.cancel()
            }
        }
    }

    fun getSubscriptions(): Map<String, Subscription> = subscriptions

    /**
     * Must be called to correctly stop all the threads.
     */
    fun stop() {
        isConnected.set(false)
        try {
            socket?.close()
        } catch (e: Exception) {
            // we have nothing to do with this
        }
        isStopped = true
    }

    fun notify(methodName: String, params: RpcParams) {
        internalWrite(RpcRequestOut(methodName, params).apply {
            id = nextRequestId.getAndIncrement().toString()
        }.toJson())
    }

    fun renewSubscriptions() {
        logger.logInfo("Subscriptions been renewed")
        synchronized(subscriptions) {
            val toRenew = subscriptions.toMutableMap()
            toRenew.forEach { subscribe(it.value) }
        }
    }

    fun subscribe(subscription: Subscription) {
        val request = RpcRequestOut(subscription.methodName, subscription.params).apply {
            id = nextRequestId.getAndIncrement().toString()
            callbacks[id.toString()] = subscription.callback
            synchronized(subscriptions) {
                subscriptions[subscription.methodName] = subscription
            }
        }.toJson()
        internalWrite(request)
    }

    /**
     * Computes a "short" compound ID based on an array of IDs.
     */
    private fun compoundId(ids: Array<String>): String = ids.sortedArray().joinToString("")

    @Throws(TimeoutException::class)
    @Synchronized
    fun write(requests: List<RpcRequestOut>): BatchedRpcResponse {
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
        internalWrite(batchedRequest)

        val executedIn = measureTimeMillis {
            if (!latch.await(SMALL_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                isConnected.set(false)
                throw TimeoutException("Timeout")
            }
        }
        currentResponceTimeout = calculateNewTimeout(executedIn)

        return response!!
    }

    @Throws(TimeoutException::class)
    @Synchronized
    fun write(methodName: String, params: RpcParams): RpcResponse {
        var response: RpcResponse? = null
        val latch = CountDownLatch(1)
        val request = RpcRequestOut(methodName, params).apply {
            id = nextRequestId.getAndIncrement().toString()
            callbacks[id.toString()] = {
                response = it as RpcResponse
                latch.countDown()
            }
        }.toJson()
        internalWrite(request)

        val executedIn = measureTimeMillis {
            if (!latch.await(SMALL_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                isConnected.set(false)
                throw TimeoutException("Timeout")
            }
        }
        currentResponceTimeout = calculateNewTimeout(executedIn)
        return response!!
    }

    private fun calculateNewTimeout(executedIn: Long) = when {
        executedIn <= SMALL_RESPONSE_TIMEOUT -> SMALL_RESPONSE_TIMEOUT
        executedIn <= MEDIUM_RESPONSE_TIMEOUT -> MEDIUM_RESPONSE_TIMEOUT
        else -> MAX_RESPONSE_TIMEOUT
    }

    private fun calculateNewTimeout(attempt: Int) = when (attempt) {
        0 -> SMALL_RESPONSE_TIMEOUT
        1 -> MEDIUM_RESPONSE_TIMEOUT
        else -> MAX_RESPONSE_TIMEOUT
    }

    private fun close() = socket?.close()

    private fun messageReceived(message: String) {
        if (message.contains("error")) {
            logger.logError(message)
        }
        lastSuccessTime = System.currentTimeMillis()
        val isBatched = message[0] == '['
        if (isBatched) {
            val response = BatchedRpcResponse.fromJson(incoming!!)
            val compoundId = compoundId(response.responses.map {it.id.toString()}.toTypedArray())

            callbacks[compoundId]?.also { callback ->
                callback.invoke(response)
            }
            callbacks.remove(compoundId)
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
            callbacks.remove(id)
        }
    }

    // Send ping message. It is expected to be executed in the dedicated timer thread
    private fun sendPingMessage() {
        try {
            val pong = write("server.ping", RpcMapParams(emptyMap<String, String>()))
            logger.logInfo("Pong! $pong")
        } catch (ex: Exception) {
            isConnected.set(false)
            socket?.close()
            ex.printStackTrace()
        }
    }

    private fun internalWrite(msg: String) {
        thread(start = true) {
            try {
                val bytes = (msg + "\n").toByteArray()
                outgoing!!.write(bytes)
                outgoing!!.flush()
            } catch (ex : Exception) {
                ex.printStackTrace()
            }
        }
    }

    companion object {
        private val INTERVAL_BETWEEN_SOCKET_RECONNECTS = TimeUnit.SECONDS.toMillis(5)
        private val INTERVAL_BETWEEN_PING_REQUESTS = TimeUnit.SECONDS.toMillis(10)
        private val SMALL_RESPONSE_TIMEOUT = TimeUnit.SECONDS.toMillis(10)
        private val MEDIUM_RESPONSE_TIMEOUT = TimeUnit.MINUTES.toMillis(1)
        private val MAX_RESPONSE_TIMEOUT = TimeUnit.MINUTES.toMillis(5)
    }
}
