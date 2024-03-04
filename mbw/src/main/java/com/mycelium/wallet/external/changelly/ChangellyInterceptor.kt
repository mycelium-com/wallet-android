package com.mycelium.wallet.external.changelly

import com.mrd.bitlib.crypto.Hmac
import com.mrd.bitlib.util.HexUtils
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * This Interceptor is necessary to comply with changelly's authentication scheme and follows
 * roughly their example implementation in JS:
 * https://github.com/changelly/api-changelly#authentication
 *
 * It wraps the parameters passed in, in a params object and signs the request with the api key secret.
 */
class ChangellyInterceptor : Interceptor {
    private companion object {
        const val API_KEY_DATA_BCH_TO_BTC = "4397e419ed0140ee81d28f66bd72a118"
        val API_SECRET_BCH_TO_BTC =
            "6ff5e3e4956b7c87213650babf977a56deab9b4ae37ea133a389dc997a9a3cae"
                .toByteArray(StandardCharsets.US_ASCII)

        const val API_KEY_DATA_ELSE = "8fb168fe8b6b4656867c846be47dccce"
        val API_SECRET_ELSE =
            "ec97042bcfba5d43f4741dbb3da9861cc59fb7c8d6123333d7823e4c7810d6c0"
                .toByteArray(StandardCharsets.US_ASCII)

        const val API_HEADER_KEY = "api-key"
        const val SIGN_HEADER_KEY = "sign"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val (apiKeyData, apiSecret) = getApiDetails(request)

        val params = getParamsFromRequest(request)
        val method = getMethodFromRequest(request)
        val requestBodyJson = JSONObject().apply {
            put("id", "test")
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
        }

        val messageBytes = requestBodyJson.toString().toByteArray()
        val signData = getSignedData(apiSecret, messageBytes)

        val mediaType = MediaType.parse("application/json; charset=UTF-8")
        val requestBody = RequestBody.create(mediaType, messageBytes)

        val newRequest = request.newBuilder()
            .delete()
            .addHeader(API_HEADER_KEY, apiKeyData)
            .addHeader(SIGN_HEADER_KEY, signData)
            .post(requestBody)
            .build()

        return chain.proceed(newRequest)
    }

    private fun getApiDetails(request: Request): Pair<String, ByteArray> {
        val params = getParamsFromRequest(request)
        val paramsFromTo = "${params.optString("from")}2${params.optString("to")}"
        return if ("BCH2BTC".equals(paramsFromTo, true)) {
            API_KEY_DATA_BCH_TO_BTC to API_SECRET_BCH_TO_BTC
        } else {
            API_KEY_DATA_ELSE to API_SECRET_ELSE
        }
    }

    private fun getMethodFromRequest(request: Request) = request.url().pathSegments().last()

    private fun getParamsFromRequest(request: Request): JSONObject = JSONObject().apply {
        request.url().queryParameterNames().forEach { name ->
            val values = request.url().queryParameterValues(name)
            if (values.size > 1) {
                put(name, JSONArray(values))
            } else {
                put(name, request.url().queryParameter(name))
            }
        }
    }

    private fun getSignedData(secret: ByteArray, data: ByteArray): String {
        val sha512bytes = Hmac.hmacSha512(secret, data)
        return HexUtils.toHex(sha512bytes)
    }
}
