package com.mycelium.wapi.api.jsonrpc

import com.mycelium.wapi.api.exception.RpcResponseException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.*
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
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
open class JsonRpcTcpClient(private var endpoints : Array<TcpEndpoint>, androidApiVersion: Int) {
    private val logger = Logger.getLogger(JsonRpcTcpClient::class.java.simpleName)
    private var curEndpointIndex = (Math.random() * endpoints.size).toInt()
    private val ssf = if (androidApiVersion < 22) getSocketFactory() else SSLSocketFactory.getDefault() as SSLSocketFactory
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
                    logger.log(Level.INFO, "Waiting until the connection is active again")
                    while (!isConnectionThreadActive) {
                        sleep(300)
                    }
                    logger.log(Level.INFO, "The connection is active again, continue main connection thread loop")
                }
                val currentEndpoint = endpoints[curEndpointIndex]
                try {
                    logger.log(Level.INFO, "Connecting to ${currentEndpoint.host}:${currentEndpoint.port}")

                    socket = ssf.createSocket().apply {
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
            logger.log(Level.INFO, "Couldn't get reply for $timeout milliseconds.")
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

    private fun waitForConnected(timeout: Long): Boolean = runBlocking {
        withTimeoutOrNull(timeout) {
            while (!isConnected.get()) {
                delay(WAITING_FOR_CONNECTED_INTERVAL)
            }
            true
        } ?: false
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
            logger.log(Level.INFO, "Couldn't get reply on $methodName for $timeout milliseconds.")
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
            logger.log(Level.SEVERE, message)
        }
        val isBatched = message[0] == '['
        if (isBatched) {
            val response = BatchedRpcResponse.fromJson(message)
            val compoundId = compoundId(response.responses.map {it.id.toString()}.toTypedArray())

            callbacks.remove(compoundId)?.invoke(response)
            awaitingRequestsMap.remove(compoundId)
        } else {
            val response = RpcResponse.fromJson(message)
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

    private fun getSocketFactory(): SSLSocketFactory {
        // Create a KeyStore containing our trusted CAs
        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)
            ENDPOINTS_CERTS.forEachIndexed { ind, cert ->
                setCertificateEntry("crt$ind", createCert(cert))
            }
        }

        // Create a TrustManager that trusts the CAs inputStream our KeyStore
        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
            init(keyStore)
        }

        // Create an SSLContext that uses our TrustManager
        val context: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, tmf.trustManagers, null)
        }

        return context.socketFactory
    }

    private fun createCert(cert: String): X509Certificate {
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val caInput: InputStream = BufferedInputStream(cert.byteInputStream(Charset.forName("UTF-8")))
        return  caInput.use {
            cf.generateCertificate(it) as X509Certificate
        }
    }

    companion object {
        private val INTERVAL_BETWEEN_SOCKET_RECONNECTS = TimeUnit.SECONDS.toMillis(1)
        private val INTERVAL_BETWEEN_PING_REQUESTS = TimeUnit.SECONDS.toMillis(10)
        private val MAX_READ_RESPONSE_TIMEOUT = TimeUnit.SECONDS.toMillis(30)
        private const val WAITING_FOR_CONNECTED_INTERVAL = 300L
        private val ENDPOINTS_CERTS = arrayListOf("-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n", "-----BEGIN CERTIFICATE-----\n" +
                "MIIHdjCCBl6gAwIBAgIQVOF2KIqc/Y2L32qAE3XPdTANBgkqhkiG9w0BAQsFADCB\n" +
                "jzELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
                "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQD\n" +
                "Ey5TZWN0aWdvIFJTQSBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
                "MB4XDTE5MDUxNDAwMDAwMFoXDTIxMDUxMzIzNTk1OVowWzEhMB8GA1UECxMYRG9t\n" +
                "YWluIENvbnRyb2wgVmFsaWRhdGVkMR0wGwYDVQQLExRQb3NpdGl2ZVNTTCBXaWxk\n" +
                "Y2FyZDEXMBUGA1UEAwwOKi5teWNlbGl1bS5jb20wggIiMA0GCSqGSIb3DQEBAQUA\n" +
                "A4ICDwAwggIKAoICAQC/77SLPl6D0qt3hNYk6nUhHbJJBeeaaAKxZCcXPMvdX5XM\n" +
                "WhzLtxoCKmoWm9zJFuvJ5Nb+Xkr03L8eFPxvbW9RVRkzB1dvVcSlsa6YXY8A1UP4\n" +
                "b4gaPiaWK3YI5ycY4ah43iozykFI0mwhOaCwFE+VGbBt/3af9sk7/YoLz/YkyqcX\n" +
                "GrTff+jF7mlgeiuzaqKeLaOdXlkAbBBqszUUTW66mjHMMD+hB8//de84ZBMxfuyv\n" +
                "gI/adE2yt8shd0L6nu7bLqaJx06w6T8tmVCddd4v9bZKmD5Q+tHgQrEZHtlBRSgY\n" +
                "C+yt1O68kmI4zzbBJXDOOq68StgH8hIe8tgoe58zs6r3oEigRt/DMJYkDRa4LvsH\n" +
                "+o4sLKdZk88QJMjo//fmZObK/UcrlNkKAuaY6csPcTOgonrpffAU+0XgjHk1K1pX\n" +
                "4AQXEI7Db90tEFj4576yQl7VEmcctYcNIVbCknNcssjAYFYdjCc3iKLEnh5qpvLH\n" +
                "TtT6aJxc8kbCYJ9e4PFBGxMrzvMVu8AQD0EwsB84/x7MYjvIHkPDxRVUSGn3qQkf\n" +
                "9VEYk4KJeJgfLYPSJZF0IFAV2XHfS+1cg/LKS9wHpaSd0rD9S2YK5T08nO1Mb/Wj\n" +
                "XfMqguCgqBM8Vaj3oJf36VjEr1OMQEp+yfI0R4vf/ORS+Jg8TlIy+X5Wn7rbsQID\n" +
                "AQABo4IC/zCCAvswHwYDVR0jBBgwFoAUjYxexFStiuF36Zv5mwXhuAGNYeEwHQYD\n" +
                "VR0OBBYEFNS4bTeiC2elgy4m9L6xNbcm+hz9MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
                "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
                "QjBAMDQGCysGAQQBsjEBAgIHMCUwIwYIKwYBBQUHAgEWF2h0dHBzOi8vc2VjdGln\n" +
                "by5jb20vQ1BTMAgGBmeBDAECATCBhAYIKwYBBQUHAQEEeDB2ME8GCCsGAQUFBzAC\n" +
                "hkNodHRwOi8vY3J0LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FEb21haW5WYWxpZGF0\n" +
                "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
                "ZWN0aWdvLmNvbTAnBgNVHREEIDAegg4qLm15Y2VsaXVtLmNvbYIMbXljZWxpdW0u\n" +
                "Y29tMIIBfwYKKwYBBAHWeQIEAgSCAW8EggFrAWkAdgC72d+8H4pxtZOUI5eqkntH\n" +
                "OFeVCqtS6BqQlmQ2jh7RhQAAAWq1qTpOAAAEAwBHMEUCICLrWS+t/4D3TdyYJKWZ\n" +
                "uUwfwkEbf6YG91qT3ZS0+O8lAiEAsyGbfeKkXLbQYQbdoyNFM8viDSWDc5s6MnQ8\n" +
                "spdarSIAdwBElGUusO7Or8RAB9io/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWq1qTpw\n" +
                "AAAEAwBIMEYCIQC4K768uGZOZhNhiPmwArFrpYJ8xYQ3zWp16WJg5zhLYAIhALqZ\n" +
                "Wb2ft7PDErprCAuVGOjwc4xxvBAnu9eWCRjXas83AHYAb1N2rDHwMRnYmQCkURX/\n" +
                "dxUcEdkCwQApBo2yCJo32RMAAAFqtak6agAABAMARzBFAiB+F9IX0Pd8j3F2X0Mo\n" +
                "W9m+j8ZbF/wuqlRnjsjw+ACKxwIhANEJ8QEpkv3foni2qYGk1SQVZgEwFaS+GNxx\n" +
                "RnBsQty4MA0GCSqGSIb3DQEBCwUAA4IBAQB9ESmckWqfZA01+70DWdIrlftewwxP\n" +
                "wgNwJxIxnExeR7+WZYVjxgfujSHdd9MhnmPgfYtV0CdWbpF3yycu6RZSDQAsV6H+\n" +
                "8PBvQNsc2wFnTuaHCS2wW2x0DQ2aK/pw7Ne43bVUEi7sz1KmFxx49sHahs0RzK4x\n" +
                "wk1WqxIsYeso+oellk6wwCZ3zNPMKnLJ0QeuvJUeYIrRz/nQNetB+G0IsQsSoc3y\n" +
                "h+WXjH4OOv8czVyI5y/MMuaEwzGlETt/Yatz4VT6rjSKwIUOPUZ7opsOQr4rRs0L\n" +
                "1c/IkW0UBSZ9dRSYeSgayzUovWxRIV5XGJrEO4A7gFup5Aarq53AnpNa\n" +
                "-----END CERTIFICATE-----\n")
    }
}
