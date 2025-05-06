package com.mycelium.wallet.external.changelly

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Locale

/**
 * This Interceptor is necessary to comply with changelly's authentication scheme and follows
 * roughly their example implementation in JS:
 * https://github.com/changelly/api-changelly#authentication
 *
 * It wraps the parameters passed in, in a params object and signs the request with the api key secret.
 */
class ChangellyInterceptor : Interceptor {
    private companion object {
        const val API_HEADER_KEY = "X-Api-Key"
        const val SIGN_HEADER_KEY = "X-Api-Signature"

        val privateKey = getChangellyApiKey()
        fun getChangellyApiKey(): PrivateKey {
            val keySpec = PKCS8EncodedKeySpec(ChangellyHeaderInterceptor.PRIVATE_KEY)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(keySpec)
        }
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val params = getParamsFromRequest(request)
        val method = getMethodFromRequest(request)
        val baseUrl = request.url.toString().substringBefore(method)

        val requestBodyJson = JSONObject().apply {
            put("id", "test")
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
        }

        val messageBytes = requestBodyJson.toString().toByteArray()

        val mediaType = "application/json; charset=UTF-8".toMediaType()
        val requestBody = RequestBody.create(mediaType, messageBytes)
        val newRequest = request.newBuilder()
            .url("$baseUrl#$method")
            .addHeader(API_HEADER_KEY, ChangellyHeaderInterceptor.PUBLIC_KEY_BASE64)
            .addHeader(SIGN_HEADER_KEY, getSignature(messageBytes))
            .post(requestBody)
            .build()

        return chain.proceed(newRequest)
    }

    private fun getMethodFromRequest(request: Request) = request.url.pathSegments.last()

    private fun getParamsFromRequest(request: Request): JSONObject = JSONObject().apply {
        request.url.queryParameterNames.forEach { name ->
            val values = request.url.queryParameterValues(name)
            if (values.size > 1) {
                put(name, JSONArray(values))
            } else {
                val param = request.url.queryParameter(name)
                val value = param?.let {
                    if (name == "from" || name == "to") it.toLowerCase(Locale.ROOT) else it
                }
                put(name, value)
            }
        }
    }

    private fun getSignature(data: ByteArray): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        val signedData = signature.sign()
        return Base64.encodeToString(signedData, Base64.NO_WRAP)
    }
}
