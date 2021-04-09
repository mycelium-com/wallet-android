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
import java.net.URI

class EthUriParser(override val network: NetworkParameters, private val supportedERC20Tokens: Map<String, ERC20Token>) : AssetUriParser(network) {
    private fun parseParams(uri: URI): AssetUri? {
        val params = splitQuery(uri.rawQuery)

        // Identify asset first to define coin type
        val asset: String? = params["req-asset"]
        val coinType = if (asset != null) {
            supportedERC20Tokens.values.firstOrNull {
                it.contractAddress == asset
            } ?: ERC20Token(contractAddress = asset)
        } else {
            if (network.isProdnet) EthMain else EthTest
        }

        // Address
        var address: Address? = null
        val addressString = uri.host
        if (addressString?.isNotEmpty() == true) {
            // for both eth and erc20 ethereum address is used
            address = AddressUtils.from(if (network.isProdnet) EthMain else EthTest, addressString)
        }

        // Amount
        val amount: Value? = params["value"]?.let {
            Value.valueOf(coinType, BigDecimal(it).multiply(BigDecimal.TEN.pow(coinType.unitExponent)).toBigIntegerExact())
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