package com.mycelium.wapi.content

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest
import java.net.URI


class BitcoinVaultUriParser(override val network: NetworkParameters) : AssetUriParser(network) {
    override fun parse(content: String): AssetUri? {
        try {
            var uri = URI.create(content.trim { it <= ' ' })
            val scheme = uri.scheme
            if (!scheme!!.equals("bitcoinvault", ignoreCase = true)) {
                // not a bitcoin URI
                return null
            }
            var schemeSpecific = uri.toString().substring("bitcoinvault:".length)
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid bitcoin URI in the form "bitcoin://"
                schemeSpecific = schemeSpecific.substring(2)
            }
            uri = URI.create("bitcoinvault://$schemeSpecific")

            return parseParameters(uri, if (network.isProdnet) BitcoinVaultMain else BitcoinVaultTest)
        } catch (e: Exception) {
        }
        return null
    }
}