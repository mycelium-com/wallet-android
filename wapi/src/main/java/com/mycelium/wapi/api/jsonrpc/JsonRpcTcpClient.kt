package com.mycelium.wapi.api.jsonrpc

import java.io.*
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

typealias Consumer<T> = (T) -> Unit

open class JsonRpcTcpClient(
        host: String,
        port: Int
) {

    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory
    private val socket: Socket = ssf.createSocket(host, port)
    private val `in` = BufferedReader(InputStreamReader((socket.getInputStream())))
    private val out = BufferedOutputStream(socket.getOutputStream())

    private val callbacks = mutableMapOf<String, Consumer<RpcResponse>>()

    fun notify(methodName: String, params: RpcParams) {
        internalWrite(RpcRequestOut(methodName, params).let { it.toJson() })
    }

    fun writeAsync(methodName: String, params: RpcParams, callback: Consumer<RpcResponse>) {
        val request = RpcRequestOut(methodName, params).apply {
            id = UUID.randomUUID().toString()
            callbacks[id.toString()] = callback
        }.let { it.toJson() }
        internalWrite(request)
    }


    @Throws(TimeoutException::class)
    fun write(methodName: String, params: RpcParams, timeOut: Long): RpcResponse {

        var response: RpcResponse? = null
        val latch = CountDownLatch(1)
        val request = RpcRequestOut(methodName, params).apply {
            id = UUID.randomUUID().toString()
            callbacks[id.toString()] = {
                response = it
                latch.countDown()
            }
        }.let { it.toJson() }
        internalWrite(request)
        if (!latch.await(timeOut, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Timeout")
        }
        return response!!

    }

    fun start() {
        try {
            while (true) {
                messageReceived(`in`.readLine())
            }
        } catch (ex: IOException) {
        }
    }


    fun close() = socket.close()

    fun messageReceived(message: String) {
        val response = RpcResponse.fromJson(message)
        if (response.error != null) {
        } else {
            val id = response.id.toString()
            callbacks[id]?.also { callback ->
                callback.invoke(response)
            }
            callbacks.remove(id)
        }
    }

    fun internalWrite(msg: String) {
        Thread {
            val bytes = (msg + "\n").toByteArray()
            out.write(bytes)
            out.flush()
        }.start()
    }

}
