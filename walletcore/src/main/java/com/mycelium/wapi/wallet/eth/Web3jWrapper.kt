package com.mycelium.wapi.wallet.eth

import com.mycelium.net.HttpEndpoint
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.erc20.StandardToken
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.*
import org.web3j.protocol.core.methods.response.EthTransaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.ContractGasProvider
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.timerTask

/**
 * As we can have multiple ethereum node endpoints for backup purposes and we want
 * to switch between them if one of them went down or out of sync,
 * we need to rebuild client with new endpoint from time to time.
 * This class maintains endpoints health checks and switching and
 * provides wrapper methods that use Web3j client built with the latest endpoint.
 */
class Web3jWrapper(endpoints: List<HttpEndpoint>) : ServerEthListChangedListener {
    private var walletManager: WalletManager? = null
    private lateinit var web3j: Web3j
    private var endpoints = ServerEndpoints(endpoints.toTypedArray())
    private val logger = Logger.getLogger(Web3jWrapper::class.simpleName)

    init {
        updateClient()
        Timer().scheduleAtFixedRate(timerTask {
            selectEndpoint()
        }, 1000L * 60, 1000L * 60)
    }

    fun setWalletManager(walletManager: WalletManager) {
        this.walletManager = walletManager
    }

    private fun updateClient() {
        web3j = buildCurrentEndpoint()
    }

    private fun buildCurrentEndpoint(): Web3j =
            Web3j.build(HttpService(endpoints.currentEndpoint.baseUrl)).also {
                logger.log(Level.INFO, "web3j built with: ${endpoints.currentEndpoint.baseUrl}")
            }

    private fun selectEndpoint() {
        if (walletManager?.isNetworkConnected != true) {
            return
        }

        logger.log(Level.INFO, "Starting health check...")
        for (x in 0 until endpoints.size()) {
            try {
                if (EthSyncChecker(web3j).isSynced) {
                    return
                }
            } catch (ex: Exception) {
                logger.log(Level.SEVERE, "Error synchronizing ETH, $ex, ${endpoints.currentEndpoint.baseUrl}")
                logger.log(Level.SEVERE, "Switching to next endpoint...")
            }
            endpoints.switchToNextEndpoint()
            updateClient()
        }
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        endpoints = ServerEndpoints(newEndpoints)
        logger.log(Level.INFO, "New endpoints supplied: ${newEndpoints.joinToString()}")
        updateClient()
    }

    fun ethGetBalance(address: String, blockParameterName: DefaultBlockParameterName = DefaultBlockParameterName.LATEST): Request<*, EthGetBalance> =
            web3j.ethGetBalance(address, blockParameterName)

    fun ethSendRawTransaction(hex: String?): Request<*, EthSendTransaction> = web3j.ethSendRawTransaction(hex)

    fun ethGetTransactionByHash(txid: String): Request<*, EthTransaction> = web3j.ethGetTransactionByHash(txid)

    fun ethGetTransactionCount(address: String, blockParameterName: DefaultBlockParameterName = DefaultBlockParameterName.PENDING): Request<*, EthGetTransactionCount> =
            web3j.ethGetTransactionCount(address, blockParameterName)

    fun ethBlockNumber(): Request<*, EthBlockNumber> = web3j.ethBlockNumber()

    fun loadContract(contractAddress: String, credentials: Credentials, gasProvider: ContractGasProvider) =
            StandardToken.load(contractAddress, web3j, credentials, gasProvider)
}

interface ServerEthListChangedListener {
    fun serverListChanged(newEndpoints: Array<HttpEndpoint>)
}