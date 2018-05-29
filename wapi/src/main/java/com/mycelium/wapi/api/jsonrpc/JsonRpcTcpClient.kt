package com.mycelium.wapi.api.jsonrpc

import java.io.*
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

import info.laht.yajrpc.net.AbstractRpcClient

open class JsonRpcTcpClient(
        host: String,
        port: Int
) : AbstractRpcClient() {

    private val ssf = SSLSocketFactory.getDefault() as SSLSocketFactory
    private val socket: Socket = ssf.createSocket(host, port)
    private val `in` = BufferedReader(InputStreamReader((socket.getInputStream())))
    private val out = BufferedOutputStream(socket.getOutputStream())

    init {
        start()
    }

    private fun start() {
        Thread {
            try {
                while (true) {
                    messageReceived(`in`.readLine())
                }
            } catch (ex: IOException) {
            }

        }.start()
    }


    override fun close() = socket.close()

    override fun internalWrite(msg: String) {
        val bytes = msg.toByteArray()
        out.write(bytes)
        out.flush()
    }

}