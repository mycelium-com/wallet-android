package com.mycelium.wapi.content.eth

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.AssetUriParser
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import java.math.BigDecimal
import org.web3j.tx.ChainIdLong
import java.net.URI

class EthUriParser(override val network: NetworkParameters, private val supportedERC20Tokens: Map<String, ERC20Token>) : AssetUriParser(network) {
    /**
     * EIP-681: `ethereum:[pay-]<target>[@<chain_id>][/<function>]?<params>`.
     * `target` is the recipient, or the token contract for `/transfer?address=<recipient>&uint256=<amount>`.
     */
    private fun parseParams(uri: URI): AssetUri? {
        val params = splitQuery(uri.rawQuery)
        val path = uri.rawSchemeSpecificPart.removePrefix("//").substringBefore('?').removePrefix("pay-")
        var target = path.substringBefore('/')
        val function = path.substringAfter('/', "")
        val chainId = target.substringAfter('@', "")
        target = target.substringBefore('@')
        // a request for another chain (e.g. mainnet in the testnet build) must not be paid here
        if (chainId.isNotEmpty() && chainId.toLongOrNull() != expectedChainId) {
            return null
        }

        val isTokenTransfer = function.equals("transfer", ignoreCase = true)
        if (function.isNotEmpty() && !isTokenTransfer) {
            return null
        }
        // Identify asset first to define coin type
        val asset: String? = if (isTokenTransfer) target else params["req-asset"]
        val coinType = if (asset != null) {
            supportedERC20Tokens.values.firstOrNull {
                it.contractAddress.equals(asset, ignoreCase = true)
            } ?: ERC20Token(contractAddress = asset)
        } else {
            if (network.isProdnet) EthMain else EthTest
        }

        // Address
        var address: Address? = null
        val addressString = if (isTokenTransfer) params["address"] else target
        if (addressString?.isNotEmpty() == true) {
            // for both eth and erc20 ethereum address is used
            address = AddressUtils.from(if (network.isProdnet) EthMain else EthTest, addressString)
        }

        // Amount
        // EIP-681: `value` (and `uint256` of a token transfer) is expressed in wei / the token's smallest unit,
        // scientific notation allowed ("1e18"); the non-standard `amount` parameter stays in whole coins
        // for backwards compatibility.
        val amount: Value? = (if (isTokenTransfer) params["uint256"] else params["value"])?.let {
            Value.valueOf(coinType, BigDecimal(it).toBigIntegerExact())
        } ?: params["amount"]?.let {
            Value.valueOf(coinType, BigDecimal(it).multiply(BigDecimal.TEN.pow(coinType.unitExponent)).toBigIntegerExact())
        }

        val label: String? = params["label"] ?: params["message"]

        return if (address == null) {
            null
        } else {
            EthUri(address, amount, label, null, asset)
        }
    }

    private val expectedChainId: Long
        get() = if (network.isProdnet) ChainIdLong.MAINNET else ChainIdLong.SEPOLIA

    override fun parse(content: String): AssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("ethereum", ignoreCase = true)) {
                // not an rmc URI
                return null
            }

            var schemeSpecific = uri.schemeSpecificPart
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid rmc URI in the form "rmc://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("ethereum://$schemeSpecific")

            return parseParams(uri)
        } catch (e: Exception) {
        }
        return null
    }
}