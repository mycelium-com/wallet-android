package com.mycelium.wapi.content.tron

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.AssetUriParser
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.coins.TronMain
import com.mycelium.wapi.wallet.tron.coins.TronTest
import java.math.BigDecimal
import java.net.URI

/**
 * Parses `tron:` URIs:
 *  - `tron:<address>[?amount=<trx>&label=..]` — TRX payment
 *  - `tron:<contractAddress>?address=<to>&amount=<tokens>` — TRC20 payment (TRC-21)
 *  - `tron:<address>?req-asset=<contractAddress>&amount=..` — EIP-681-style token payment
 */
class TronUriParser(override val network: NetworkParameters, private val supportedTokens: Map<String, TRC20Token>)
    : AssetUriParser(network) {

    private val tronCoin: CryptoCurrency get() = if (network.isProdnet) TronMain else TronTest

    override fun parse(content: String): AssetUri? {
        try {
            var uri = URI.create(content.trim())
            if (!uri.scheme.equals("tron", ignoreCase = true)) return null
            var schemeSpecific = uri.toString().substring("tron:".length)
            if (schemeSpecific.startsWith("//")) schemeSpecific = schemeSpecific.substring(2)
            uri = URI.create("tron://$schemeSpecific")
            return parseTronParams(uri)
        } catch (e: Exception) {
        }
        return null
    }

    private fun parseTronParams(uri: URI): AssetUri? {
        val params = splitQuery(uri.rawQuery)
        val host = uri.host ?: uri.authority ?: return null

        val recipientParam = params["address"]
        val contractAddress: String?
        val recipient: String
        if (recipientParam != null) {
            // TRC-21: host is the token contract, recipient in ?address=
            contractAddress = host
            recipient = recipientParam
        } else {
            contractAddress = params["req-asset"] ?: params["token"]
            recipient = host
        }
        val coinType: CryptoCurrency = if (contractAddress != null) {
            supportedTokens.values.firstOrNull { it.contractAddress == contractAddress }
                ?: TRC20Token(contractAddress = contractAddress)
        } else tronCoin

        val address: Address = TronAddress.decode(recipient)?.let { TronAddress(tronCoin, recipient) } ?: return null
        val amount: Value? = (params["amount"] ?: params["value"])?.let {
            Value.valueOf(coinType, BigDecimal(it).multiply(BigDecimal.TEN.pow(coinType.unitExponent)).toBigIntegerExact())
        }
        val label: String? = params["label"] ?: params["message"]
        return TronUri(address, amount, label, null, contractAddress)
    }
}
