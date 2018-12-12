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
import org.apache.http.client.utils.URLEncodedUtils
import java.math.BigDecimal
import java.net.URI

abstract class GenericAssetUriParser(open val network: NetworkParameters) : UriParser {

    protected fun parseParameters(uri: URI, coinType: CryptoCurrency): GenericAssetUri? {
        // Address
        var address: GenericAddress? = null
        val addressString = uri.host
        if (addressString != null && addressString.isNotEmpty()) {
            address = AddressUtils.from(coinType, addressString.trim { it <= ' ' })
        }
        val params = URLEncodedUtils.parse(uri, "UTF-8")

        var amount: Value? = null
        // Amount
        try {
            val amountStr = params.first { it.name == "amount" }.value

            if (amountStr != null) {
                amount = Value.valueOf(coinType, BigDecimal(amountStr).movePointRight(8).toBigIntegerExact().toLong())
            }
        }catch (e: NoSuchElementException){
        }

        // Label
        // Bip21 defines "?label" and "?message" - lets try "label" first and if it does not
        // exist, lets use "message"
        var label: String? = null
        try {
            label = params.first { it.name == "label" }.value
        } catch (e: NoSuchElementException) {
        }
        if (label == null) {
            try {
                label = params.first { it.name == "message" }?.value
            } catch (e: NoSuchElementException) {
            }
        }

        // Check if the supplied "address" is actually an encrypted private key
        if (addressString != null && Bip38.isBip38PrivateKey(addressString)) {
            return PrivateKeyUri(addressString, label)
        }

        // Payment Uri
        var paymentUri: String? = null
        try {
            paymentUri = params.first { it.name == "r" }.value
        } catch (e: NoSuchElementException) {
        }

        return if (address == null && paymentUri == null) {
            null
        } else {
            createUriByCoinType(coinType, address, amount, label, paymentUri)
        }
    }

    private fun createUriByCoinType(coinType: CryptoCurrency,
                                    address: GenericAddress?,
                                    amount: Value?,
                                    label: String?,
                                    paymentUri: String?): GenericAssetUri?{
        return when(coinType){
            is BitcoinMain, is BitcoinTest -> BitcoinUri(address, amount, label, paymentUri)
            is RMCCoin, is RMCCoinTest -> RMCUri(address, amount, label, paymentUri)
            is MTCoin, is MTCoinTest -> MTUri(address, amount, label, paymentUri)
            is MASSCoin, is MASSCoinTest -> MSSUri(address, amount, label, paymentUri)
            else -> null
        }

    }
}
