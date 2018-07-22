package com.mycelium.wapi.api.jsonrpc

import com.mycelium.WapiLogger
import com.mycelium.wapi.api.ConnectionMonitor
import com.mycelium.wapi.api.ConnectionMonitor.ConnectionEvent.*
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
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

typealias Consumer<T> = (T) -> Unit

class TcpEndpoint(val host: String, val port: Int)

open class JsonRpcTcpClient(private val endpoints : Array<TcpEndpoint>,
                            val logger: WapiLogger) : ConnectionMonitor {
    private val observers = ArrayList<ConnectionMonitor.ConnectionObserver>()
    private var curEndpointIndex = (Math.random() * endpoints.size).toInt()
    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory

    private var isConnected = AtomicBoolean(false)
    @Volatile
    var isStopped = false
    @Volatile
    private var socket: Socket? = null
    @Volatile
    private var incoming : BufferedReader? = null
    @Volatile
    private var outgoing : BufferedOutputStream? = null
    private val nextRequestId = AtomicInteger(0)
    private val isStarted = AtomicBoolean(false)
    // Timer responsible for periodically executing ping requests
    private val pingTimer = Timer()

    private val callbacks = mutableMapOf<String, Consumer<AbstractResponse>>()

    override fun register(o: ConnectionMonitor.ConnectionObserver?) {
        observers.add(o!!)
    }

    override fun unregister(o: ConnectionMonitor.ConnectionObserver?) {
        observers.remove(o!!)
    }

    fun notify(methodName: String, params: RpcParams) {
        internalWrite(RpcRequestOut(methodName, params).apply {
            id = nextRequestId.getAndIncrement().toString()
        }.toJson())
    }

    fun subscribe(methodName: String, params: RpcParams, callback: Consumer<AbstractResponse>) {
        val request = RpcRequestOut(methodName, params).apply {
            id = nextRequestId.getAndIncrement().toString()
            callbacks[id.toString()] = callback
            callbacks[methodName] = callback
        }.toJson()
        internalWrite(request)
    }

    /**
     * Computes a "short" compound ID based on an array of IDs.
     */
    private fun compoundId(ids: Array<String>): String = ids.sortedArray().joinToString("")
    //return HashUtils.sha256(string.toByteArray()).toHex()

    @Throws(TimeoutException::class)
    @Synchronized
    fun write(requests: List<RpcRequestOut>, timeOut: Long): BatchedRpcResponse {
        if (!isConnected.get()) {
            synchronized(isConnected) {
                (isConnected as java.lang.Object).wait(timeOut)
                if (!isConnected.get()) {
                    throw TimeoutException("Timeout")
                }
            }
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
        internalWrite(batchedRequest)

        if (!latch.await(timeOut, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Timeout")
        }

        return response!!
    }

    @Throws(TimeoutException::class)
    @Synchronized
    fun write(methodName: String, params: RpcParams, timeOut: Long): RpcResponse {
        if (!isConnected.get()) {
            synchronized(isConnected) {
                (isConnected as java.lang.Object).wait(timeOut)
                if (!isConnected.get()) {
                    throw TimeoutException("Timeout")
                }
            }
        }
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
        if (!latch.await(timeOut, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Timeout")
        }
        return response!!
    }

    private fun notifyListeners() {
        observers.forEach {
            it.connectionChanged(if (isConnected.get()) WENT_ONLINE else WENT_OFFLINE)
        }
    }

    @Throws(IllegalStateException::class)
    fun start() {
        if (isStarted.compareAndSet(true, true)) {
            throw IllegalStateException("RPC client could not be started twice.")
        }
        thread(start = true) {
            while(!isStopped) {
                val currentEndpoint = endpoints[curEndpointIndex]
                try {
                    synchronized(this) {
                        socket = ssf.createSocket(currentEndpoint.host, currentEndpoint.port)
                        socket!!.keepAlive = true
                        incoming = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                        outgoing = BufferedOutputStream(socket!!.getOutputStream())
                        callbacks.clear()
                        notify("server.version", RpcParams.mapParams(
                                "client_name" to "wapi",
                                "protocol_version" to "1.2"))
                        isConnected.set(true)
                        synchronized(isConnected) {
                            (isConnected as java.lang.Object).notifyAll()
                        }
                        notifyListeners()

                        // Schedule periodic ping requests execution
                        pingTimer.scheduleAtFixedRate(timerTask {
                            sendPingMessage()
                        }, 0, INTERVAL_BETWEEN_PING_REQUESTS)
                    }

                    // Inner loop for reading data from socket. If the connection breaks, we should
                    // exit this loop and try creating new socket in order to restore connection
                    while (isConnected.get()) {
                        val line = incoming!!.readLine()
                                // There can be a use case when BufferedReader.readline() returns null
                                ?: continue
                        messageReceived(line)
                    }
                } catch (exception: Exception) {
                    logger.logError("Socket creation or receiving failed: ${exception.message}")
                }
                close()
                isConnected.set(false)
                notifyListeners()
                // Sleep for some time before moving to the next endpoint
                Thread.sleep(INTERVAL_BETWEEN_SOCKET_RECONNECTS)
                curEndpointIndex = (curEndpointIndex + 1) % endpoints.size
            }
            pingTimer.cancel()
        }
    }

    private fun close() = socket?.close()

    private fun messageReceived(message: String) {
        if (message.contains("error")) {
            logger.logError(message)
        }
        val isBatched = message[0] == '['
        if (isBatched) {
            val response = BatchedRpcResponse.fromJson(message)
            val compoundId = compoundId(response.responses.map {it.id.toString()}.toTypedArray())

            callbacks[compoundId]?.also { callback ->
                callback.invoke(response)
            }
            callbacks.remove(compoundId)
        } else {
            val response = RpcResponse.fromJson(message)
            val id = response.id.toString()
            if (id != NO_ID.toString()) {
                callbacks[id]?.also { callback ->
                    callback.invoke(response)
                }
            } else {
                callbacks[response.method]?.also { callback ->
                    callback.invoke(response)
                }
            }
            callbacks.remove(id)
        }
    }

    // Send ping message. It is expected to be executed in the dedicated timer thread
    private fun sendPingMessage() {
        try {
            val pong = write("server.ping", RpcMapParams(emptyMap<String, String>()), DEFAULT_RESPONSE_TIMEOUT)
            logger.logInfo("Pong! $pong " + Date().toString())
        } catch (ex: Exception) {
            isConnected.set(false)
            isStopped = true
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
        private const val INTERVAL_BETWEEN_SOCKET_RECONNECTS = 5000L
        private const val INTERVAL_BETWEEN_PING_REQUESTS = 10000L
        private const val DEFAULT_RESPONSE_TIMEOUT = 10000L
    }
}
