package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.eth.EthAddress
import org.web3j.abi.datatypes.Address as W3jAddress
import org.web3j.crypto.WalletUtils

abstract class EthCoin(id: String?, name: String?, symbol: String?)
        : CryptoCurrency(id, name, symbol, 18, 2, false) {

    override fun parseAddress(addressString: String?): Address? = parseAddress(this, addressString)

    companion object {
        @JvmStatic val BLOCK_TIME_IN_SECONDS = 15

        fun parseAddress(cryptoCurrency: CryptoCurrency, addressString: String?): Address? = when {
            addressString == null -> null
            // additional wrap of addressString into Address is called upon to unify addresses with and
            // without '0x' prefix
            WalletUtils.isValidAddress(addressString) -> EthAddress(cryptoCurrency, W3jAddress(addressString).toString())
            else -> null
        }
    }
}