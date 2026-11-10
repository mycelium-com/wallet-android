package com.mycelium.wallet.external.changelly

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.SignedMessage
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.util.HexUtils
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExchangeWalletSignatureInterceptorTest {
    // Shared vector with the exchange backend and WalletKMP tests
    private val key = InMemoryPrivateKey(
        HexUtils.toBytes("1f2e3d4c5b6a79880f1e2d3c4b5a69788796a5b4c3d2e1f00112233445566778"), true)
    private val address = "1KCwMKxjHyNtRTepngA49qqsgw5iwRwkP2"
    private val body = """{"id": "test", "jsonrpc": "2.0", "method": "createFixTransaction", "params": {"from": "btc", "to": "eth", "amountFrom": "0.01", "fromAddresses": ["bc1qexample"]}}"""
    // bitlib does not normalize to low-S, so this differs from pycoin's signature in s and the
    // recovery id; pycoin (the gift card backend's verifier) accepts both
    private val bitlibSignature = "Hy/ShyRysU3jcqQyLGoUOrXHM86icPWhPW/QcdhQ0HtcSLc8nJxXo5i3WJhWHHJQg6TfC4RnXoPYeI6yVvaD3vA="
    private val pycoinSignature = "IC/ShyRysU3jcqQyLGoUOrXHM86icPWhPW/QcdhQ0Htct0jDY2OoXGdIp2ep442vexXP0WJH6hxjR0OsNdmyYlE="

    @Test
    fun signsBodyWithMainnetIdentity() {
        val sent = intercept { key }

        assertEquals(address, sent.header(ExchangeWalletSignatureInterceptor.WALLET_ADDRESS_HEADER))
        val signature = sent.header(ExchangeWalletSignatureInterceptor.WALLET_SIGNATURE_HEADER)!!
        assertEquals(bitlibSignature, signature)
        SignedMessage.validate(BitcoinAddress.fromString(address), body, signature)
        SignedMessage.validate(BitcoinAddress.fromString(address), body, pycoinSignature)
    }

    @Test
    fun noIdentityWithoutMasterSeed() {
        val sent = intercept { null }

        assertNull(sent.header(ExchangeWalletSignatureInterceptor.WALLET_ADDRESS_HEADER))
        assertNull(sent.header(ExchangeWalletSignatureInterceptor.WALLET_SIGNATURE_HEADER))
    }

    @Test
    fun keyFailureStillSendsRequest() {
        val sent = intercept { throw IllegalStateException("accessed identity account with no master seed configured") }

        assertNull(sent.header(ExchangeWalletSignatureInterceptor.WALLET_ADDRESS_HEADER))
        assertEquals(body.length.toLong(), sent.body!!.contentLength())
    }

    private fun intercept(identityKey: () -> InMemoryPrivateKey?): Request {
        val chain = RecordingChain(Request.Builder()
            .url("https://mbwswaps.com/api/v1/#createFixTransaction")
            .post(body.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build())
        ExchangeWalletSignatureInterceptor(identityKey).intercept(chain)
        return chain.proceeded!!
    }

    private class RecordingChain(private val request: Request) : Interceptor.Chain {
        var proceeded: Request? = null

        override fun request() = request

        override fun proceed(request: Request): Response {
            proceeded = request
            return Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK").build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }
}
