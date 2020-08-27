package com.mycelium.wapi.content.fio

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.AssetUriParser
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import java.net.URI


class FIOUriParser(override val network: NetworkParameters) : AssetUriParser(network) {
    override fun parse(content: String): AssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("FIO", ignoreCase = true)) {
                // not a bitcoin URI
                return null
            }
            var schemeSpecific = uri.toString().substring("FIO:".length)
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid FIO URI in the form "FIO://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("FIO://$schemeSpecific")

            return parseParameters(uri, if (network.isProdnet) FIOMain else FIOTest)
        } catch (e: Exception) {
        }
        return null
    }
}