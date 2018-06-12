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
import kotlin.concurrent.thread

interface  MyceliumNodesApi {
    @GET("/nodes.json")
    fun getNodes(): Call<MyceliumNodesResponse>
}

class MyceliumNodesResponse(@SerializedName("BTC-testnet") val btcTestnet: BTCNetResponse,
                            @SerializedName("BTC-mainnet") val btcMainnet: BTCNetResponse)

class BTCNetResponse(val electrumX: ElectrumXResponse)

class ElectrumXResponse(val nodes : Array<String>)

class WalletConfiguration(private val prefs: SharedPreferences, val network : NetworkParameters) {

    init {
        updateConfig()
    }

    fun updateConfig() {
        var latch = CountDownLatch(1)

        thread(start = true) {
            try {
                val resp = Retrofit.Builder()
                        .baseUrl("https://mycelium-wallet.s3.amazonaws.com")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(MyceliumNodesApi::class.java)
                        .getNodes()
                        .execute()

                if (resp.isSuccessful) {
                    val myceliumNodesResponse = resp.body()

                    val nodes = mutableSetOf(*if (network.isTestnet) {
                        myceliumNodesResponse?.btcTestnet?.electrumX?.nodes!!
                    } else {
                        myceliumNodesResponse?.btcMainnet?.electrumX?.nodes!!
                    })

                        // the new peers are different from the old peers and not empty. store them!
                    prefs.edit().putStringSet(PREFS_ELECTRUM_SERVERS, nodes).apply()
                }

            } catch (ex :IOException) {
            } finally {
                latch.countDown()
            }
        }

        latch.await()
    }

    val electrumServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_SERVERS, mutableSetOf(*BuildConfig.ElectrumServers))

    fun getElectrumEndpoints(): List<TcpEndpoint>
    {
        val result = ArrayList<TcpEndpoint>()
        electrumServers.forEach {
            val strs = it.split(":")
            result.add(TcpEndpoint(strs[0], strs[1].toInt()))
        }
        return result
    }

    companion object {
        const val PREFS_ELECTRUM_SERVERS = "electrum_servers"
    }
}