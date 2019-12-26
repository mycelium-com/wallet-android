package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.families.Families
import com.mycelium.wapi.wallet.eth.EthAddress
import org.web3j.crypto.WalletUtils

abstract class EthCoin : CryptoCurrency() {
    init {
        family = Families.ETHEREUM
        unitExponent = 18
        addressPrefix = ""
    }

    override fun parseAddress(addressString: String): GenericAddress? {
        return if (WalletUtils.isValidAddress(addressString)) {
            EthAddress(this, addressString)
        } else {
            null
        }
    }

    companion object {
        @JvmStatic val BLOCK_TIME_IN_SECONDS = 15
    }
}