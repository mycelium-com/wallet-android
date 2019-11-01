package com.mycelium.wapi.content.eth

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.colu.ColuAssetUriParser
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import java.net.URI

class EthUriParser(override val network: NetworkParameters) : ColuAssetUriParser(network) {
    override fun parse(content: String): GenericAssetUri? {
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

            return parseParameters(uri, if (network.isProdnet) EthMain else EthTest)
        } catch (e: Exception) {
        }
        return null
    }
}