package com.mycelium.wapi.content

import com.mrd.bitlib.crypto.Bip38
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.content.colu.mss.MSSUri
import com.mycelium.wapi.content.colu.mt.MTUri
import com.mycelium.wapi.content.colu.rmc.RMCUri
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.*
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLDecoder


abstract class GenericAssetUriParser(open val network: NetworkParameters) : UriParser {

    protected fun parseParameters(uri: URI, coinType: CryptoCurrency): GenericAssetUri? {
        // Address
        var address: GenericAddress? = null
        val addressString = uri.host
        if (addressString != null && addressString.isNotEmpty()) {
            address = AddressUtils.from(coinType, addressString)
        }

        val params = splitQuery(uri.rawQuery)

        var amount: Value? = null
        // Amount
        try {
            val amountStr = params["amount"]

            if (amountStr != null) {
                amount = Value.valueOf(coinType, (java.lang.Double.parseDouble(amountStr) * Math.pow(10.0, 8.0)).toLong())
            }
        } catch (e: NoSuchElementException) {
        }

        // Label
        // Bip21 defines "?label" and "?message" - lets try "label" first and if it does not
        // exist, lets use "message"
        var label: String? = null
        try {
            label = params["label"]
        } catch (e: NoSuchElementException) {
        }
        if (label == null) {
            try {
                label = params["message"]
            } catch (e: NoSuchElementException) {
            }
        }

        // Check if the supplied "address" is actually an encrypted private key
        if (addressString != null && Bip38.isBip38PrivateKey(addressString)) {
            if (coinType == BitcoinMain.get() || coinType == BitcoinTest.get()) {
                return PrivateKeyUri(addressString, label, "bitcoin")
            }
            return PrivateKeyUri(addressString, label, coinType.symbol.decapitalize())
        }

        // Payment Uri
        var paymentUri: String? = null
        try {
            paymentUri = params["r"]
        } catch (e: NoSuchElementException) {
        }

        return if (address == null && paymentUri == null) {
            null
        } else {
            createUriByCoinType(coinType, address, amount, label, paymentUri)
        }
    }

    @Throws(UnsupportedEncodingException::class)
    fun splitQuery(query: String?): Map<String, String> {
        val result = mutableMapOf<String, String>()
        query?.let { query ->
            val pairs = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                result[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }
        }
        return result
    }

    private fun createUriByCoinType(coinType: CryptoCurrency,
                                    address: GenericAddress?,
                                    amount: Value?,
                                    label: String?,
                                    paymentUri: String?): GenericAssetUri? {
        return when (coinType) {
            is BitcoinMain, is BitcoinTest -> BitcoinUri(address, amount, label, paymentUri)
            is RMCCoin, is RMCCoinTest -> RMCUri(address, amount, label, paymentUri)
            is MTCoin, is MTCoinTest -> MTUri(address, amount, label, paymentUri)
            is MASSCoin, is MASSCoinTest -> MSSUri(address, amount, label, paymentUri)
            else -> null
        }

    }
}
