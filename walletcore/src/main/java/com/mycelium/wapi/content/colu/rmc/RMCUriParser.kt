package com.mycelium.wapi.content.colu.rmc

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.colu.ColuAssetUriParser
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest
import java.net.URI

class RMCUriParser(override val network: NetworkParameters) : ColuAssetUriParser(network) {
    override fun parse(content: String): GenericAssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("rmc", ignoreCase = true)) {
                // not a rmc URI
                return null
            }

            var schemeSpecific = uri.schemeSpecificPart
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid rmc URI in the form "rmc://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("rmc://$schemeSpecific")

            return parseParameters(uri, if (network.isProdnet) RMCCoin else RMCCoinTest)

        } catch (e: Exception) {
        }
        return null
    }
}