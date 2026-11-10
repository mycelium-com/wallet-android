package com.mycelium.wallet.external.changelly

import android.util.Log
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wapi.wallet.AesKeyCipher
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

/**
 * Identifies the wallet to the exchange backend the same way the gift card API does: the identity
 * key for [WEBSITE] signs the request body as a Bitcoin signed message, and its mainnet P2PKH
 * address is the user id. Must be added after [ChangellyInterceptor], which builds the JSON-RPC
 * body that goes over the wire. Wallets without a master seed, or with the setting turned off,
 * send no identity.
 */
class ExchangeWalletSignatureInterceptor(
    private val identityKey: () -> InMemoryPrivateKey? = ::walletIdentityKey
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body ?: return chain.proceed(request)
        val signedRequest = try {
            identityKey()?.let { key ->
                val message = Buffer().also { body.writeTo(it) }.readUtf8()
                request.newBuilder()
                    .header(WALLET_ADDRESS_HEADER, key.publicKey
                        .toAddress(NetworkParameters.productionNetwork, AddressType.P2PKH).toString())
                    .header(WALLET_SIGNATURE_HEADER, key.signMessage(message).base64Signature)
                    .build()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exchange request sent without wallet identity", e)
            null
        }
        return chain.proceed(signedRequest ?: request)
    }

    companion object {
        const val WEBSITE = "exchange.mycelium"
        const val WALLET_ADDRESS_HEADER = "wallet-address"
        const val WALLET_SIGNATURE_HEADER = "wallet-signature"
        private const val TAG = "ExchangeIdentity"
    }
}

// The master seed never changes once configured, so the derived key is cached for the process:
// otherwise each of the parallel exchange requests at startup decrypted/derived it again.
@Volatile
private var cachedIdentityKey: InMemoryPrivateKey? = null

private fun walletIdentityKey(): InMemoryPrivateKey? {
    if (!SettingsPreference.exchangeWalletIdentityEnabled) return null
    cachedIdentityKey?.let { return it }
    val masterSeedManager = MbwManager.getInstance(WalletApplication.getInstance()).masterSeedManager
    if (!masterSeedManager.hasBip32MasterSeed()) return null
    val cipher = AesKeyCipher.defaultKeyCipher()
    return masterSeedManager.getIdentityAccountKeyManager(cipher)
        .getPrivateKeyForWebsite(ExchangeWalletSignatureInterceptor.WEBSITE, cipher)
        .also { cachedIdentityKey = it }
}
