package com.mycelium.wapi.content.colu.mss

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.colu.ColuAssetUriParser
import com.mycelium.wapi.wallet.colu.coins.MASSCoin
import com.mycelium.wapi.wallet.colu.coins.MASSCoinTest
import java.net.URI

class MSSUriParser(override val network: NetworkParameters) : ColuAssetUriParser(network) {
    override fun parse(content: String): GenericAssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("mss", ignoreCase = true)) {
                // not a rmc URI
                return null
            }

            var schemeSpecific = uri.schemeSpecificPart
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid rmc URI in the form "mss://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("mss://$schemeSpecific")

            return parseParameters(uri, if (network.isProdnet) MASSCoin else MASSCoinTest)

        } catch (e: Exception) {
        }
        return null
    }
}