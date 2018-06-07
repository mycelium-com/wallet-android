package com.mycelium.wapi.api.jsonrpc

import com.mycelium.WapiLogger
import com.mycelium.wapi.api.ConnectionMonitor
import java.io.*
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

typealias Consumer<T> = (T) -> Unit

class TcpEndpoint(val host: String, val port: Int)

open class JsonRpcTcpClient(val endpoints : Array<TcpEndpoint>,
                            val logger: WapiLogger) : ConnectionMonitor {
    private val observers = ArrayList<ConnectionMonitor.ConnectionObserver>()
    private val INTERVAL_BETWEEN_SOCKET_RECONNECTS = 5000L

    private var curEdpointIndex = 0
    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory

    @Volatile
    public var isConnected = false
    @Volatile
    private var socket: Socket? = null
    @Volatile
    private var `in` : BufferedReader? = null
    @Volatile
    private var out : BufferedOutputStream? = null

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

        val compoundIds = requests.map {it.id.toString()}.toTypedArray().sortedArray().joinToString { it }

        callbacks[compoundIds] = {
            response = it as BatchedRpcResponse
            latch.countDown()
        }

        val batchedRequest = '[' + requests.map { it.toJson() }.joinToString() + ']'
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

    fun notifyListeners() {
        this.observers.forEach {
            if (isConnected) {
                it.connectionChanged(ConnectionMonitor.ConnectionEvent.WENT_ONLINE)
            } else {
                it.connectionChanged(ConnectionMonitor.ConnectionEvent.WENT_OFFLINE)
            }
        }
    }

    fun start() {
        thread(start = true) {

            while(true) {
                val currentEndpoint = endpoints.get(curEdpointIndex)
                try {
                    socket = ssf.createSocket(currentEndpoint.host, currentEndpoint.port)
                    socket!!.keepAlive = true
                    `in` = BufferedReader(InputStreamReader((socket!!.getInputStream())))
                    out = BufferedOutputStream(socket!!.getOutputStream())
                    isConnected = true
                    notifyListeners()
                } catch(ex: Exception) {
                    isConnected = false
                    // Sleep for some time before moving to the next endpoint
                    Thread.sleep(INTERVAL_BETWEEN_SOCKET_RECONNECTS)
                    curEdpointIndex = (curEdpointIndex + 1) % endpoints.size
                    continue
                }

                // Inner loop for reading data from socket. If the connection breaks, we should
                // exit this loop and try creating new socket in order to restore connection
                while(true) {
                    try {
                        val line: String = `in`!!.readLine()
                        messageReceived(line)
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
                out!!.write(bytes)
                out!!.flush()
            } catch (ex : Exception) {
                ex.printStackTrace()
            }
        }
    }
}
