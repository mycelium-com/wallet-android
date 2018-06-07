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
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread

typealias Consumer<T> = (T) -> Unit

class TcpEndpoint(val host: String, val port: Int)

open class JsonRpcTcpClient(private val endpoints : Array<TcpEndpoint>,
                            val logger: WapiLogger) : ConnectionMonitor {
    private val observers = ArrayList<ConnectionMonitor.ConnectionObserver>()
    private var curEndpointIndex = 0
    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory

    @Volatile
    private var isConnected = false
    @Volatile
    private var socket: Socket? = null
    @Volatile
    private var incoming : BufferedReader? = null
    @Volatile
    private var outgoing : BufferedOutputStream? = null

    private val callbacks = mutableMapOf<String, Consumer<AbstractResponse>>()

    override fun register(o: ConnectionMonitor.ConnectionObserver?) {
        observers.add(o!!)
    }

    override fun unregister(o: ConnectionMonitor.ConnectionObserver?) {
        observers.remove(o!!)
    }

    fun notify(methodName: String, params: RpcParams) {
        internalWrite(RpcRequestOut(methodName, params).toJson())
    }

    fun writeAsync(methodName: String, params: RpcParams, callback: Consumer<AbstractResponse>) {
        val request = RpcRequestOut(methodName, params).apply {
            id = UUID.randomUUID().toString()
            callbacks[id.toString()] = callback
        }.toJson()
        internalWrite(request)
    }

    @Throws(TimeoutException::class)
    fun write(requests: List<RpcRequestOut>, timeOut: Long): BatchedRpcResponse {
        var response: BatchedRpcResponse? = null
        val latch = CountDownLatch(1)

        requests.forEach {
            it.id = UUID.randomUUID().toString()
        }

        val compoundIds = requests.map {it.id.toString()}.toTypedArray().sortedArray().joinToString()

        callbacks[compoundIds] = {
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
    fun write(methodName: String, params: RpcParams, timeOut: Long): RpcResponse {
        var response: RpcResponse? = null
        val latch = CountDownLatch(1)
        val request = RpcRequestOut(methodName, params).apply {
            id = UUID.randomUUID().toString()
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
            it.connectionChanged(if (isConnected) WENT_ONLINE else WENT_OFFLINE)
        }
    }

    fun start() {
        thread(start = true) {
            while(true) {
                val currentEndpoint = endpoints[curEndpointIndex]
                try {
                    socket = ssf.createSocket(currentEndpoint.host, currentEndpoint.port)
                    socket!!.keepAlive = true
                    incoming = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    outgoing = BufferedOutputStream(socket!!.getOutputStream())
                    write("server.version", RpcParams.mapParams(
                            "client_name" to "wapi",
                            "protocol_version" to "1.2"),
                            10000)
                    isConnected = true
                    notifyListeners()
                } catch(ex: Exception) {
                    isConnected = false
                    // Sleep for some time before moving to the next endpoint
                    Thread.sleep(INTERVAL_BETWEEN_SOCKET_RECONNECTS)
                    curEndpointIndex = (curEndpointIndex + 1) % endpoints.size
                    continue
                }

                // Inner loop for reading data from socket. If the connection breaks, we should
                // exit this loop and try creating new socket in order to restore connection
                while(true) {
                    try {
                        val line: String? = `in`!!.readLine()

                        // There can be a use case when BufferedReader.readline() returns null
                        if (line == null) {
                            continue
                        }

                        messageReceived(line!!)
                    } catch(ex: IOException) {
                        isConnected = false
                        notifyListeners()
                        break
                    }
                }
            }
        }
    }

    fun close() = socket?.close()

    private fun messageReceived(message: String) {
        val isBatched = message[0] == '['
        if (isBatched) {
            val response = BatchedRpcResponse.fromJson(message)
            val compoundId = response.responses.map {it.id.toString()}.toTypedArray().sortedArray().joinToString { it }

            callbacks[compoundId]?.also { callback ->
                callback.invoke(response)
            }
            callbacks.remove(compoundId)
        } else {
            val response = RpcResponse.fromJson(message)
            val id = response.id.toString()
            callbacks[id]?.also { callback ->
                callback.invoke(response)
            }
            callbacks.remove(id)
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
    }
}
