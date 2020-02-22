package com.mycelium.wapi.wallet.erc20

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.Web3jWrapper
import org.web3j.crypto.Credentials
import org.web3j.tx.gas.DefaultGasProvider

class ERC20BalanceService(private val address: String,
                          private val token: ERC20Token,
                          private val coinType: CryptoCurrency,
                          private var web3jWrapper: Web3jWrapper,
                          private val credentials: Credentials) {
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    fun updateBalanceCache() {
        return try {
            val erc20Contract = web3jWrapper.loadContract(token.contractAddress, credentials, DefaultGasProvider())
            val result = erc20Contract.balanceOf(address).send()

            balance = Balance(Value.valueOf(coinType, result), Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType))
        } catch (e: Exception) {
        }
    }
}