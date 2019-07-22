package com.mycelium.wapi.wallet.eth

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.infura.InfuraHttpService
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class EthBalanceService(val address: String) {
    private val web3j: Web3j = Web3j.build(InfuraHttpService("https://ropsten.infura.io/WKXR51My1g5Ea8Z5Xh3l"))
    var balance = BigInteger.valueOf(0)
        private set

    fun updateBalanceCache(): Boolean {
        return try {
            val balanceRequest = web3j.ethGetBalance(address, DefaultBlockParameterName.PENDING)
            val balanceResult = balanceRequest.send()
            balance = balanceResult.balance
            true
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: UnknownHostException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
