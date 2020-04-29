package com.mycelium.bequant.remote.client

import com.mycelium.wallet.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitHolder {

    const val API_VERSION = "v0.0.50"
    const val BASE_URL = "https://xwpe71x4sg.execute-api.us-east-1.amazonaws.com/prd-reg/"

    val clientBuilder: OkHttpClient.Builder by lazy {
        OkHttpClient().newBuilder()
                .addNetworkInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                            .removeHeader(AUTH_NAME_HEADER)
                            .build()
                    chain.proceed(newRequest)
                }
                .addInterceptor(HeaderParamInterceptor("ApiKeyAuth", "X-API-KEY", this::apiKeyGenerator))
                .addInterceptor(HeaderParamInterceptor("BearerAuth", "Authorization", this::apiKeyGenerator))

                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                        })
                    }
                }
    }

    val retrofitBuilder: Retrofit.Builder by lazy {
        val moshi = Moshi.Builder()
                .add(EnumJsonAdapterFactory)
                .add(KotlinJsonAdapterFactory())
                .build()

        Retrofit.Builder()
                .callFactory(object : Call.Factory {
                    //create client lazy on demand in background thread
                    //see https://www.zacsweers.dev/dagger-party-tricks-deferred-okhttp-init/
                    private val client by lazy { clientBuilder.build() }
                    override fun newCall(request: Request): Call = client.newCall(request)
                })
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(EnumRetrofitConverterFactory)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
    }

    val retrofit: Retrofit by lazy { retrofitBuilder.build() }


    private val securityDefinitions = HashMap<String, String>()

    fun setApiKey(authMethod: AuthMethod, apiKey: String) {
        securityDefinitions[authMethod.authName] = apiKey
    }

    fun removeAuthInfo(authMethod: AuthMethod) {
        securityDefinitions -= authMethod.authName
    }

    fun setBasicAuth(authMethod: AuthMethod, username: String, password: String) {
        securityDefinitions[authMethod.authName] = Credentials.basic(username, password)
    }

    fun setBearerAuth(authMethod: AuthMethod, bearer: String) {
        securityDefinitions[authMethod.authName] = "Bearer $bearer"
    }

    private fun apiKeyGenerator(authName: String, request: Request): String? =
            if (requestHasAuth(request, authName))
                securityDefinitions[authName]
            else
                null


    private fun requestHasAuth(request: Request, authName: String): Boolean {
        val headers = request.headers(AUTH_NAME_HEADER)
        return headers.contains(authName)
    }

    const val AUTH_NAME_HEADER = "X-Auth-Name"

}

enum class AuthMethod(internal val authName: String) {
    ApiKeyAuth("ApiKeyAuth"),

    BearerAuth("BearerAuth")
}
