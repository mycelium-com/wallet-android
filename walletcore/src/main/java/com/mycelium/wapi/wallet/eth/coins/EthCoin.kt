package com.mycelium.wapi.wallet.eth.coins

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.eth.EthAddress
import org.web3j.abi.datatypes.Address as W3jAddress
import org.web3j.crypto.Keys
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric

abstract class EthCoin(id: String?, name: String?, symbol: String?)
        : CryptoCurrency(id, name, symbol, 18, 8, false) {

    /**
     * Whether EIP-1559 (type-2) fees and signing are enabled for this chain.
     * Off by default → legacy gas price + type-0 signing. Enabled on mainnet and Sepolia.
     */
    open val supportsEip1559: Boolean = false

    override fun parseAddress(addressString: String?): Address? = parseAddress(this, addressString)

    companion object {
        @JvmStatic val BLOCK_TIME_IN_SECONDS = 15

        fun parseAddress(cryptoCurrency: CryptoCurrency, addressString: String?): Address? = when {
            addressString == null -> null
            !WalletUtils.isValidAddress(addressString) || !hasValidChecksum(addressString) -> null
            // additional wrap of addressString into Address is called upon to unify addresses with and
            // without '0x' prefix
            else -> EthAddress(cryptoCurrency, W3jAddress(addressString).toString())
        }

        /**
         * EIP-55: a mixed-case address carries a checksum in its letter case, so a typo in a copied
         * address is detectable. All-lowercase / all-uppercase addresses have no checksum.
         */
        private fun hasValidChecksum(addressString: String): Boolean {
            val hex = Numeric.cleanHexPrefix(addressString)
            if (hex == hex.lowercase() || hex == hex.uppercase()) return true
            return Numeric.cleanHexPrefix(Keys.toChecksumAddress(hex)) == hex
        }
    }
}