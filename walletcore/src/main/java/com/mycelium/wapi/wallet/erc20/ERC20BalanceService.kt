package com.mycelium.wapi.wallet.erc20

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

class ERC20BalanceService(private val address: String,
                          private val token: ERC20Token,
                          private val coinType: CryptoCurrency,
                          private var client: Web3j,
                          private var backing: EthAccountBacking,
                          private val credentials: Credentials) {
    var balance: Balance = Balance.getZeroBalance(coinType)
        private set

    fun updateBalanceCache(): Boolean {
        return try {
            val erc20Contract = StandardToken.load(token.contractAddress, client, credentials, DefaultGasProvider())
            val result = erc20Contract.balanceOf(address).send()

            val txs = backing.getUnconfirmedTransactions()
            val pendingReceiving = txs.filter { it.from != address && it.to == address }
                    .map { BigInteger.valueOf(it.value.valueAsLong) }
                    .fold(BigInteger.ZERO, BigInteger::add)
            val pendingSending = txs.filter { it.from == address && it.to != address }
                    .map { BigInteger.valueOf(it.value.valueAsLong) }
                    .fold(BigInteger.ZERO, BigInteger::add)
            balance = Balance(Value.valueOf(coinType, result - pendingSending),
                    Value.valueOf(coinType, pendingReceiving), Value.valueOf(coinType, pendingSending), Value.zeroValue(coinType))
            true
        } catch (e: Exception) {
            false
        }
    }
}