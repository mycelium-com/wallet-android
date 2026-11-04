package com.mycelium.wallet.activity.send.helper

import android.content.Context
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.displaySymbol
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.coins.TronCoin

/**
 * Explains why [input] is not a valid recipient for [coinType], instead of a bare "Invalid
 * address": an address of another network (a TRON address for USDT on Ethereum is the usual
 * mistake — both are "USDT"), a malformed string, or a well-formed one whose checksum fails.
 */
object RecipientAddressError {
    private enum class Network(val nameRes: Int) {
        BITCOIN(R.string.network_name_bitcoin),
        ETHEREUM(R.string.network_name_ethereum),
        TRON(R.string.network_name_tron)
    }

    private val ETH_FORMAT = Regex("^(0[xX])?[0-9a-fA-F]{40}$")
    private val TRON_FORMAT = Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$")

    /** @return a user-facing reason, or null when [input] is empty or valid for [coinType]. */
    fun describe(context: Context, coinType: CryptoCurrency, input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || coinType.parseAddress(text) != null) return null
        val target = networkOf(coinType)
        val detected = detectNetwork(text)
        if (target != null && detected != null && detected != target) {
            return context.getString(R.string.recipient_invalid_wrong_network,
                context.getString(detected.nameRes), coinType.displaySymbol, context.getString(target.nameRes))
        }
        return when (target) {
            Network.ETHEREUM -> context.getString(
                if (ETH_FORMAT.matches(text)) R.string.recipient_invalid_eth_checksum
                else R.string.recipient_invalid_eth_format)
            Network.TRON -> context.getString(
                if (TRON_FORMAT.matches(text)) R.string.recipient_invalid_checksum
                else R.string.recipient_invalid_tron_format)
            else -> context.getString(R.string.recipient_invalid)
        }
    }

    private fun networkOf(coinType: CryptoCurrency): Network? = when {
        coinType is EthCoin || coinType is ERC20Token -> Network.ETHEREUM
        coinType is TronCoin || coinType is TRC20Token -> Network.TRON
        coinType == Utils.getBtcCoinType() -> Network.BITCOIN
        else -> null
    }

    private fun detectNetwork(text: String): Network? = when {
        TronAddress.isValid(text) -> Network.TRON
        // checksum ignored on purpose: the format alone already says "Ethereum"
        ETH_FORMAT.matches(text) && text.length == 42 -> Network.ETHEREUM
        Utils.getBtcCoinType().parseAddress(text) != null -> Network.BITCOIN
        else -> null
    }
}
