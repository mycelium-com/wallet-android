package com.mycelium.wapi.wallet.eth

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.JsonRpc2_0Web3j
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.RpcErrors
import org.web3j.protocol.core.filters.Callback
import org.web3j.protocol.core.filters.FilterException
import org.web3j.protocol.core.methods.response.EthFilter
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.core.methods.response.EthLog.LogResult
import java.io.IOException
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.timerTask

class PendingTransactionFilter(private val web3j: Web3j, private val callback: Callback<String>) {
    private val logger: Logger = Logger.getLogger(Web3jWrapper::class.simpleName)

    @Volatile
    private var filterId: BigInteger? = null

    private var schedule: Timer? = null

    fun run() {
        try {
            val ethFilter = sendRequest()
            if (ethFilter.hasError()) {
                throwException(ethFilter.error)
            }
            filterId = ethFilter.filterId
            schedule = Timer()
            logger.log(Level.INFO, "Start polling timer...")
            schedule!!.scheduleAtFixedRate(timerTask {
                try {
                    pollFilter()
                } catch (e: Throwable) {
                    // All exceptions must be caught, otherwise our job terminates without
                    // any notification
                    logger.log(Level.SEVERE, "Error sending request, $e")
                }
            }, 0, JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong())
        } catch (e: IOException) {
            throwException(e)
        }
    }

    private fun pollFilter() {
        var ethLog: EthLog? = null
        try {
            ethLog = web3j.ethGetFilterChanges(filterId).send()
        } catch (e: IOException) {
            throwException(e)
        }
        if (ethLog!!.hasError()) {
            val error = ethLog.error
            when (error.code) {
                RpcErrors.FILTER_NOT_FOUND -> reinstallFilter()
                else -> throwException(error)
            }
        } else {
            process(ethLog.logs)
        }
    }

    @Throws(IOException::class)
    fun sendRequest(): EthFilter = web3j.ethNewPendingTransactionFilter().send()

    fun process(logResults: List<LogResult<*>>) {
        for (logResult in logResults) {
            if (logResult is EthLog.Hash) {
                val transactionHash = logResult.get()
                callback.onEvent(transactionHash)
            } else {
                throw FilterException("Unexpected result type: ${logResult.get()}, required Hash")
            }
        }
    }

    private fun reinstallFilter() {
        logger.log(Level.WARNING, "The filter has not been found. Filter id: $filterId")
        cancel()
        run()
    }

    fun cancel() {
        logger.log(Level.INFO, "Cancel timer...")
        schedule!!.cancel()
    }

    private fun throwException(error: Response.Error?) {
        throw FilterException("Invalid request: "
                + if (error == null) "Unknown Error" else error.message)
    }

    private fun throwException(cause: Throwable?) {
        throw FilterException("Error sending request", cause)
    }
}
