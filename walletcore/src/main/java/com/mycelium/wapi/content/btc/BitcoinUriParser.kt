package com.mycelium.wapi.content.btc

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.GenericAssetUriParser
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import java.net.URI


class BitcoinUriParser(override val network: NetworkParameters) : GenericAssetUriParser(network) {
    override fun parse(content: String): GenericAssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("bitcoin", ignoreCase = true)) {
                // not a bitcoin URI
                return null
            }
            var schemeSpecific = uri.schemeSpecificPart
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid bitcoin URI in the form "bitcoin://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("bitcoin://$schemeSpecific")

            return parseParameters(uri, if(network.isProdnet) BitcoinMain.get() else BitcoinTest.get())

        } catch (e: Exception) {
        }
        return null
    }
}