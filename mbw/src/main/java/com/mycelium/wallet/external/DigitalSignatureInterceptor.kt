package com.mycelium.wallet.external

import android.util.Base64
import android.util.Base64.NO_WRAP
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner

class DigitalSignatureInterceptor(private val keyPair: AsymmetricCipherKeyPair) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBody = originalRequest.body() ?: return chain.proceed(originalRequest)
        val requestBodyJson = try {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            return chain.proceed(originalRequest)
        }

        val signedRequest = originalRequest.newBuilder()
            .addHeader(HEADER_API_KEY, encodePublicKey())
            .addHeader(HEADER_SIGN_KEY, signMessage(requestBodyJson))
            .build()
        return chain.proceed(signedRequest)
    }

    private fun encodePublicKey(): String {
        val publicKeyParams = keyPair.public as ECPublicKeyParameters
        val encodedPublicKey = publicKeyParams.q.getEncoded(false)
        return Base64.encodeToString(encodedPublicKey, NO_WRAP)
    }

    private fun signMessage(message: String): String {
        val signedBytes = signData(message.toByteArray())
        return Base64.encodeToString(signedBytes, NO_WRAP)
    }

    private fun signData(data: ByteArray): ByteArray {
        val signer = ECDSASigner()
        signer.init(true, keyPair.private)
        val signatureComponents = signer.generateSignature(data)
        // skip first 0 byte if appears
        val leftPart = signatureComponents[0].toByteArray().takeLast(32)
        val rightPart = signatureComponents[1].toByteArray().takeLast(32)
        val signature = leftPart + rightPart
        return signature.toByteArray()
    }

    private companion object {
        const val HEADER_API_KEY = "X-Client-Api-Key"
        const val HEADER_SIGN_KEY = "X-Client-Api-Signature"
    }
}