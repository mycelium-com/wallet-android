package com.mycelium.bequant.remote.client

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

typealias AuthGeneratorFun = (authName: String, Request) -> String?

sealed class AuthInterceptor(
    private val authName: String,
    private val generator: AuthGeneratorFun
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = generator(authName, chain.request()) ?: return chain.proceed(chain.request())
        return handleApiKey(chain, apiKey)
    }

    protected abstract fun handleApiKey(chain: Interceptor.Chain, apiKey: String): Response
}

class HeaderParamInterceptor(
    authName: String,
    private val paramName: String,
    generator: AuthGeneratorFun
) : AuthInterceptor(authName, generator) {

    override fun handleApiKey(chain: Interceptor.Chain, apiKey: String): Response {
        val newRequest = chain.request()
            .newBuilder()
            .addHeader(paramName, apiKey)
            .build()

        return chain.proceed(newRequest)
    }
}

class QueryParamInterceptor(
    authName: String,
    private val paramName: String,
    generator: AuthGeneratorFun
) : AuthInterceptor(authName, generator) {

    override fun handleApiKey(chain: Interceptor.Chain, apiKey: String): Response {
        val newUrl = chain.request().url
            .newBuilder()
            .addQueryParameter(paramName, apiKey)
            .build()

        val newRequest = chain.request()
            .newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}