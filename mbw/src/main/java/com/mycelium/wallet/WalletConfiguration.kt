package com.mycelium.wallet

import android.content.SharedPreferences
import com.google.gson.annotations.SerializedName
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import kotlinx.coroutines.experimental.launch
import java.io.IOException

interface  MyceliumNodesApi {
    @GET("/nodes.json")
    fun getNodes(): Call<MyceliumNodesResponse>
}

// A set of classes for parsing nodes.json file

// MyceliumNodesResponse is intended for parsing nodes.json file
class MyceliumNodesResponse(@SerializedName("BTC-testnet") val btcTestnet: BTCNetResponse,
                            @SerializedName("BTC-mainnet") val btcMainnet: BTCNetResponse)

// BTCNetResponse is intended for parsing nodes.json file
class BTCNetResponse(val electrumx: ElectrumXResponse)

// ElectrumXResponse is intended for parsing nodes.json file
class ElectrumXResponse(val primary : Array<ElectrumServerResponse>, backup: Array<ElectrumServerResponse>)

// ElectrumServerResponse is intended for parsing nodes.json file
class ElectrumServerResponse(val url: String)

class WalletConfiguration(private val prefs: SharedPreferences, val network : NetworkParameters, val mbwManager : MbwManager) {

    init {
        updateConfig()
    }

    // Makes a request to S3 storage to retrieve nodes.json and parses it to extract electrum servers list
    fun updateConfig() {
        launch {
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

                    val nodes = if (network.isTestnet())
                        myceliumNodesResponse?.btcTestnet?.electrumx?.primary?.map { it.url }?.toSet()
                    else
                        myceliumNodesResponse?.btcMainnet?.electrumx?.primary?.map { it.url }?.toSet()
                    prefs.edit().putStringSet(PREFS_ELECTRUM_SERVERS, nodes).apply()
                }

            } catch (ex :IOException) {
            } finally {
                mbwManager.wapi.serverListChanged(getElectrumEndpoints(), getDefaultElectrumEndpoints())
            }
        }
    }

    // Returns the list of default TcpEndpoint objects
    fun getDefaultElectrumEndpoints(): List<TcpEndpoint>
    {
        val result = ArrayList<TcpEndpoint>()
        val defaultElectrumServers = mutableSetOf(*BuildConfig.ElectrumServers)
        defaultElectrumServers.forEach {
            val strs = it.replace(TCP_TLS_PREFIX, "").split(":")
            result.add(TcpEndpoint(strs[0], strs[1].toInt()))
        }
        return result
    }
    // Returns the set of electrum servers
    val electrumServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_SERVERS, mutableSetOf(*BuildConfig.ElectrumServers))

    // Returns the list of TcpEndpoint objects
    fun getElectrumEndpoints(): List<TcpEndpoint>
    {
        val result = ArrayList<TcpEndpoint>()
        electrumServers.forEach {
            val strs = it.replace(TCP_TLS_PREFIX, "").split(":")
            result.add(TcpEndpoint(strs[0], strs[1].toInt()))
        }
        return result
    }

    companion object {
        const val MAX_WAITING_TIMEOUT = 10000L
        const val PREFS_ELECTRUM_SERVERS = "electrum_servers"

        const val TCP_TLS_PREFIX = "tcp-tls://"
        const val AMAZON_S3_STORAGE_ADDRESS = "https://mycelium-wallet.s3.amazonaws.com"
    }
}