package com.mycelium.wallet.external.changelly

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.MediaType
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
        const val privateKeyBase64 =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCFfDtC0NMgamE/SnBfqoiMS2bn75z8J+48vJubFGws6WKugv6M4CB0cg+u7Y5jUuxJxC2oX9e9UkXBx+OvKUIHufidr9uTtG0GHJEmQ11oP+teubX047nTqBROuY9gs+NPksAkcZfbQNsfdef9ZVEOea1ApnXah0GmTsftCZOSSpbAufsUhoxirK6uedNGs6VpUZrajSf26W07Bq7bQi59H630adyt70W4A6JktCVZWvWLGQ8rmUG8vPpFGd5pM37HVzuB0BigB9AG7IVH+RyVcdJTnw0jn9O1M1p0csi9QvbC5na1mRoIKd8fsCXxEK3yrEO3RT9G6vqye8A0WRPjAgMBAAECggEAOxSJrCB+GY5L/XXGd+kkJ6gl40j/+/D2dmZqHsDywgwIE8JBxPtcEf376AoXp+lnUJzmMmw9MfusiUCeCwRhR8ctfSl9L4o/aOGS8tMFECOeWt4qZTm3oTD20AM8LOphlPIYXejy8+VoNqv6YoKJ1jTPlFo4tmCAE4ox3b2L1cbO+SFJSb9VovsArwO7eWtuas+qOX1OHf0mXjEhEscrYyVQDzlvDzRWsGTtKsUdfVpkHStvt9M6g/diIE4d2VbB5fk1oGVLfk0TjNiUipBfzvwdx+8aoxobMRvcWs1rOJzArGM/PGBOyVCxXRhFnUqSDQ79TcG2+bHBOCcG2m8uxQKBgQC8UyC48Zzus2paD6Cjr2lM6Y572JXkT3ta+DAZv+wcRLv0lVx2PuwJmbZU0NfnFbyOb0cXY8/mp4dBcqDMHXPbOmnj7A6prAbbOKUOYSi94LurOXGcaA/GrZXSpht3qbBPikhULGMqGPS/Q1zDzQfrWylFxubJEBfFu+j12h6mLwKBgQC1dCrDMEO+fJKJRiASwsMD6GuBLG4WP4KvT0zMk5ih6D8xbz40B4Uhjj61mC6wk/6fzunUM7unyxbAiLlY1b/QM52MhkNP4bmsU38DinNGLkfRKre7i6e39TNEHdgaq7RbDg+zpUISLlv9qKwysZhoOIw7y6L7U8MdOR3GrCo0jQKBgCEywkT4CsMli6z+rkHMrVJqpbx9TMcnn8ZElC4l4BiHoV6XaepKY0+58iN3gWfyNAAj67Na3A58H+LQsznoQ0E1Re9w8JDGi5rfnHExfX4jfNHNWZLJ4WYTuaKdt5/boQIUjXWRMZX9Oj/xPwwhO7Eoq9jqHEr7dEVeP83/OoHvAoGBAKUOseNx4P3C1Y03k+9c6QaCAmCzaMSmKxuLeCHT1RDacblnJt8vRAQdH6ASec44IXN/Raa5FGdyzxR+ipNrhJtAiH0OmOZuP3apUS2IYImjicKUKCPayssEqgi5WR4RuPLnHJNerXZaY2WfbFyEvk13uuCdwXj7Xc4UaaiSbaX1AoGAJokaFWPJ4eciKgUhbp5aJ8SC0GdvfyXNHV6d4tcz4OyKCh9fmkMciQghu8gRa4VrsqgQ3rg/pi+BMCO9Zrc6emjOjpIsr5y3bFK3j8yvexocikn8vr/Jqoh/qrm0SE4/KKPdS77/evqqSukP8UWNcDDVNAJj9GHcMLH9w7g05tg="
        const val publicKeyBase64 = "BREhI4nAIIHctkxs9s2sXSWjhW+RPqbN5sY7Ua6797I="

        val privateKey = getChangellyApiKey()
        fun getChangellyApiKey(): PrivateKey {
            val privateKeyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP)
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(keySpec)
        }
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val params = getParamsFromRequest(request)
        val method = getMethodFromRequest(request)
        val baseUrl = request.url().toString().substringBefore(method)

        val requestBodyJson = JSONObject().apply {
            put("id", "test")
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
        }

        val messageBytes = requestBodyJson.toString().toByteArray()

        val mediaType = MediaType.parse("application/json; charset=UTF-8")
        val requestBody = RequestBody.create(mediaType, messageBytes)
        val newRequest = request.newBuilder()
            .delete()
            .url("$baseUrl#$method")
            .addHeader(API_HEADER_KEY, publicKeyBase64)
            .addHeader(SIGN_HEADER_KEY, getSignature(messageBytes))
            .post(requestBody)
            .build()

        return chain.proceed(newRequest)
    }

    private fun getMethodFromRequest(request: Request) = request.url().pathSegments().last()

    private fun getParamsFromRequest(request: Request): JSONObject = JSONObject().apply {
        request.url().queryParameterNames().forEach { name ->
            val values = request.url().queryParameterValues(name)
            if (values.size > 1) {
                put(name, JSONArray(values))
            } else {
                val param = request.url().queryParameter(name)
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
