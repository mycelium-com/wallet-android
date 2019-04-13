package com.mycelium.wallet

import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.HttpsEndpoint
import com.mycelium.net.TorHttpsEndpoint
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.mycelium.wapi.api.ServerListChangedListener
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface  MyceliumNodesApi {
    @GET("/nodes-b.json")
    fun getNodes(): Call<MyceliumNodesResponse>
}

// A set of classes for parsing nodes.json file

// MyceliumNodesResponse is intended for parsing nodes.json file
class MyceliumNodesResponse(@SerializedName("BTC-testnet") val btcTestnet: BTCNetResponse,
                            @SerializedName("BTC-mainnet") val btcMainnet: BTCNetResponse)

const val ONION_DOMAIN = ".onion"

// BTCNetResponse is intended for parsing nodes.json file
class BTCNetResponse(val electrumx: ElectrumXResponse, @SerializedName("WAPI") val wapi: WapiSectionResponse)

class WapiSectionResponse(val primary : Array<HttpsUrlResponse>)

class ElectrumXResponse(val primary : Array<UrlResponse>)

class UrlResponse(val url: String)

class HttpsUrlResponse(val url: String, @SerializedName("cert-sha1") val cert: String)

class WalletConfiguration(private val prefs: SharedPreferences,
                          val network : NetworkParameters) {

    val gson = GsonBuilder().create()

    init {
        updateConfig()
    }

    // Makes a request to S3 storage to retrieve nodes.json and parses it to extract electrum servers list
    fun updateConfig() {
        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT) {
            try {
                val resp = Retrofit.Builder()
                        .baseUrl(AMAZON_S3_STORAGE_ADDRESS)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(MyceliumNodesApi::class.java)
                        .getNodes()
                        .execute()
                if (resp.isSuccessful) {
                    val myceliumNodesResponse = resp.body()

                    val electrumXnodes = if (network.isTestnet())
                        myceliumNodesResponse?.btcTestnet?.electrumx?.primary?.map { it.url }?.toSet()
                    else
                        myceliumNodesResponse?.btcMainnet?.electrumx?.primary?.map { it.url }?.toSet()

                    val wapiNodes = if (network.isTestnet())
                        myceliumNodesResponse?.btcTestnet?.wapi?.primary
                    else
                        myceliumNodesResponse?.btcMainnet?.wapi?.primary

                    prefs.edit()
                            .putStringSet(PREFS_ELECTRUM_SERVERS, electrumXnodes)
                            .putString(PREFS_WAPI_SERVERS, gson.toJson(wapiNodes))
                            .apply()

                    serverListChangedListener?.serverListChanged(getElectrumEndpoints())
                }
            } catch (_: Exception) {}
        }
    }

    // Returns the set of electrum servers
    val electrumServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_SERVERS, mutableSetOf(*BuildConfig.ElectrumServers))!!

    // Returns the set of Wapi servers
    val wapiServers: String
        get() = prefs.getString(PREFS_WAPI_SERVERS, BuildConfig.WapiServers)!!

    // Returns the list of TcpEndpoint objects
    fun getElectrumEndpoints(): List<TcpEndpoint> {
        val result = ArrayList<TcpEndpoint>()
        electrumServers.forEach {
            val strs = it.replace(TCP_TLS_PREFIX, "").split(":")
            result.add(TcpEndpoint(strs[0], strs[1].toInt()))
        }
        return result
    }

    fun getWapiEndpoints(): List<HttpEndpoint> {
        var resp = gson.fromJson(wapiServers, Array<HttpsUrlResponse>::class.java)
        return resp.map {
            if (it.url.contains(ONION_DOMAIN)) {
                TorHttpsEndpoint(it.url, it.cert)
            } else {
                HttpsEndpoint(it.url, it.cert)
            }
        }
    }

    private var serverListChangedListener: ServerListChangedListener? = null

    fun setServerListChangedListener(serverListChangedListener : ServerListChangedListener) {
        this.serverListChangedListener = serverListChangedListener
    }

    companion object {
        const val PREFS_ELECTRUM_SERVERS = "electrum_servers"
        const val PREFS_WAPI_SERVERS = "wapi_servers"

        const val TCP_TLS_PREFIX = "tcp-tls://"
        const val AMAZON_S3_STORAGE_ADDRESS = "https://mycelium-wallet.s3.amazonaws.com"
    }
}