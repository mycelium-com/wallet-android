package com.mycelium.wapi.content.colu.mt

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.colu.ColuAssetUriParser
import com.mycelium.wapi.wallet.colu.coins.MTCoin
import com.mycelium.wapi.wallet.colu.coins.MTCoinTest
import java.net.URI

class MTUriParser(override val network: NetworkParameters) : ColuAssetUriParser(network) {
    override fun parse(content: String): GenericAssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("mt", ignoreCase = true)) {
                // not a rmc URI
                return null
            }

            var schemeSpecific = uri.schemeSpecificPart
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid mt URI in the form "mt://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("mt://$schemeSpecific")

            return parseParameters(uri, if (network.isProdnet) MTCoin else MTCoinTest)

        } catch (e: Exception) {
        }
        return null
    }
}