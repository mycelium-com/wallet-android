package com.mycelium.wallet

import android.content.SharedPreferences
import com.google.gson.annotations.SerializedName
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOError
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

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

class WalletConfiguration(private val prefs: SharedPreferences, val network : NetworkParameters) {

    init {
        updateConfig()
    }

    // Makes a request to S3 storage to retrieve nodes.json and parses it to extract electrum servers list
    fun updateConfig() {
        var count = 0
        if(prefs.getStringSet(PREFS_ELECTRUM_SERVERS,null) == null){
            count = 1
        }
        var latch = CountDownLatch(count)

        thread(start = true) {
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
                latch.countDown()
            }
        }

        // Wait for S3 server response for MAX_WAITING_TIMEOUT milliseconds
        try {
            latch.await(MAX_WAITING_TIMEOUT, TimeUnit.MILLISECONDS)
        } catch (ex : InterruptedException) {
        }
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