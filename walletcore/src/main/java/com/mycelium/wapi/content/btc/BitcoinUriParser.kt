package com.mycelium.wapi.content.btc

import com.mrd.bitlib.crypto.Bip38
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.ContentResolver.UriParser
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.coins.Value
import org.apache.http.client.utils.URLEncodedUtils
import java.math.BigDecimal
import java.net.URI


class BitcoinUriParser(val network: NetworkParameters) : UriParser {
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

            // Address
            var address: BtcAddress? = null
            val addressString = uri.host
            if (addressString != null && addressString.isNotEmpty()) {
                address = BtcLegacyAddress(BitcoinMain.get()
                        , Address.fromString(addressString.trim { it <= ' ' }, network).allAddressBytes)
            }
            val params = URLEncodedUtils.parse(uri, "UTF-8")
            // Amount
            val amountStr = params.first { it.name == "amount" }.value
            var amount: Value? = null
            if (amountStr != null) {
                amount = Value.valueOf(BitcoinMain.get(), BigDecimal(amountStr).movePointRight(8).toBigIntegerExact().toLong())
            }

            // Label
            // Bip21 defines "?label" and "?message" - lets try "label" first and if it does not
            // exist, lets use "message"
            var label = params.first { it.name == "label" }.value
            if (label == null) {
                label = params.first { it.name == "message" }.value
            }

            // Check if the supplied "address" is actually an encrypted private key
            if (Bip38.isBip38PrivateKey(addressString)) {
                return PrivateKeyUri(addressString, label)
            }

            // Payment Uri
            val paymentUri = params.first { it.name == "r" }.value

            return if (address == null && paymentUri == null) {
                null
            } else {
                BitcoinUri(address, amount, label, paymentUri)
            }

        } catch (e: Exception) {
        }
        return null
    }
}