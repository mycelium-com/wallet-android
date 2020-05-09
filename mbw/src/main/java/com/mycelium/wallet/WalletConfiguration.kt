package com.mycelium.wallet

import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.HttpsEndpoint
import com.mycelium.net.TorHttpsEndpoint
import com.mycelium.wallet.external.partner.model.PartnersLocalized
import com.mycelium.wapi.api.ServerElectrumListChangedListener
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.ServerEthListChangedListener
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*
import kotlin.collections.ArrayList


interface  MyceliumNodesApi {
    @GET("/nodes-b.json")
    fun getNodes(): Call<MyceliumNodesResponse>
}

// A set of classes for parsing nodes-b.json file

// MyceliumNodesResponse is intended for parsing nodes-b.json file
class MyceliumNodesResponse(@SerializedName("BTC-testnet") val btcTestnet: BTCNetResponse,
                            @SerializedName("BTC-mainnet") val btcMainnet: BTCNetResponse,
                            @SerializedName("ETH-testnet") val ethTestnet: ETHNetResponse?,
                            @SerializedName("ETH-mainnet") val ethMainnet: ETHNetResponse?,
                            @SerializedName("partner-info") val partnerInfos: Map<String, PartnerDateInfo>?,
                            @SerializedName("Business") val partners: Map<String, PartnersLocalized>?)

data class PartnerDateInfo(@SerializedName("start-date") val startDate: Date?, @SerializedName("end-date") val endDate: Date?)

// BTCNetResponse is intended for parsing nodes-b.json file
class BTCNetResponse(val electrumx: ElectrumXResponse, @SerializedName("WAPI") val wapi: WapiSectionResponse)

class ETHNetResponse(@SerializedName("blockbook-servers") val ethBBServers: EthServerResponse)

class WapiSectionResponse(val primary : Array<HttpsUrlResponse>)

class ElectrumXResponse(val primary : Array<UrlResponse>)

class EthServerResponse(val primary : Array<UrlResponse>)

class UrlResponse(val url: String)

class HttpsUrlResponse(val url: String, @SerializedName("cert-sha1") val cert: String)

class WalletConfiguration(private val prefs: SharedPreferences,
                          val network : NetworkParameters) {

    val gson = GsonBuilder().create()

    // Makes a request to S3 storage to retrieve nodes.json and parses it to extract electrum servers list
    fun updateConfig() {
        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT) {
            try {
                val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()
                val resp = Retrofit.Builder()
                        .baseUrl(AMAZON_S3_STORAGE_ADDRESS)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build()
                        .create(MyceliumNodesApi::class.java)
                        .getNodes()
                        .execute()
                if (resp.isSuccessful) {
                    val myceliumNodesResponse = resp.body()

                    val electrumXnodes = if (network.isTestnet)
                        myceliumNodesResponse?.btcTestnet?.electrumx?.primary?.map { it.url }?.toSet()
                    else
                        myceliumNodesResponse?.btcMainnet?.electrumx?.primary?.map { it.url }?.toSet()

                    val wapiNodes = if (network.isTestnet)
                        myceliumNodesResponse?.btcTestnet?.wapi?.primary
                    else
                        myceliumNodesResponse?.btcMainnet?.wapi?.primary

                    val ethServersFromResponse = if (network.isTestnet)
                        myceliumNodesResponse?.ethTestnet?.ethBBServers?.primary?.map { it.url }?.toSet()
                    else
                        myceliumNodesResponse?.ethMainnet?.ethBBServers?.primary?.map { it.url }?.toSet()

                    val prefEditor = prefs.edit()
                            .putStringSet(PREFS_ELECTRUM_SERVERS, electrumXnodes)
                            .putString(PREFS_WAPI_SERVERS, gson.toJson(wapiNodes))

                    val oldElectrum = electrumServers
                    val oldEth = ethBBServers
                    ethServersFromResponse?.let {
                        prefEditor.putStringSet(PREFS_ETH_BB_SERVERS, ethServersFromResponse)
                    }
                    myceliumNodesResponse?.partnerInfos?.get("fio-presale")?.endDate?.let {
                        prefEditor.putLong(PREFS_FIO_END_DATE, it.time)
                    }
                    myceliumNodesResponse?.partnerInfos?.get("fio-presale")?.startDate?.let {
                        prefEditor.putLong(PREFS_FIO_START_DATE, it.time)
                    }
                    myceliumNodesResponse?.partners?.let { map ->
                        map.keys.forEach {
                            prefEditor.putString("partners-$it", gson.toJson(map[it]))
                        }
                    }
                    prefEditor.apply()

                    if (oldElectrum != electrumServers){
                        serverElectrumListChangedListener?.serverListChanged(getElectrumEndpoints().toTypedArray())
                    }

                    if (oldEth != ethBBServers) {
                        for (serverEthListChangedListener in serverEthListChangedListeners) {
                            serverEthListChangedListener.serverListChanged(getBlockBookEndpoints().toTypedArray())
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Returns the set of electrum servers
    private val electrumServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_SERVERS, mutableSetOf(*BuildConfig.ElectrumServers))!!

    // Returns the set of Wapi servers
    private val wapiServers: String
        get() = prefs.getString(PREFS_WAPI_SERVERS, BuildConfig.WapiServers)!!

    // Returns the set of ethereum blockbook servers
    private val ethBBServers: Set<String>
        get() = prefs.getStringSet(PREFS_ETH_BB_SERVERS, mutableSetOf(*BuildConfig.EthBlockBook))!!

    // Returns the list of TcpEndpoint objects
    fun getElectrumEndpoints(): List<TcpEndpoint> {
        val result = ArrayList<TcpEndpoint>()
        electrumServers.forEach {
            try {
                val strs = it.replace(TCP_TLS_PREFIX, "").split(":")
                result.add(TcpEndpoint(strs[0], strs[1].toInt()))
            } catch (ex: Exception) {
                // We ignore endpoints given in wrong format
            }
        }
        return result
    }

    fun getWapiEndpoints(): List<HttpEndpoint> {
        val resp = gson.fromJson(wapiServers, Array<HttpsUrlResponse>::class.java)
        return resp.map {
            if (it.url.contains(ONION_DOMAIN)) {
                TorHttpsEndpoint(it.url, it.cert)
            } else {
                HttpsEndpoint(it.url, it.cert)
            }
        }
    }

    //We are not going to call HttpsEndpoint.getClient() , that's why certificate is empty
    fun getBlockBookEndpoints(): List<HttpsEndpoint> = ethBBServers.map { HttpsEndpoint(it) }

    private var serverElectrumListChangedListener: ServerElectrumListChangedListener? = null
    private var serverEthListChangedListeners : ArrayList<ServerEthListChangedListener> = arrayListOf()

    fun getSupportedERC20Tokens(): Map<String, ERC20Token> = listOf(
            ERC20Token("Tether USD", "USDT", 6, "0xdac17f958d2ee523a2206206994597c13d831ec7"),
            ERC20Token("USD Coin", "USDC", 6, "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"),
            ERC20Token("HuobiToken", "HT", 18, "0x6f259637dcd74c767781e37bc6133cd6a68aa161"),
            ERC20Token("Binance USD", "BUSD", 18, "0x4fabb145d64652a948d72533023f6e7a623c7c53"),
            ERC20Token("Bitfinex LEO", "LEO", 18, "0x2af5d2ad76741191d15dfe7bf6ac92d4bd912ca3"),
            when (BuildConfig.FLAVOR) {
                "prodnet" -> ERC20Token("0x", "ZRX", 18, "0xe41d2489571d322189246dafa5ebde1f4699f498")
                // for testing purposes
                else -> ERC20Token("0x", "ZRX", 18, "0xd676189f67CAB2D5f9b16a5c0898A0E30ed86560")
            })
            .associateBy { it.name }

    fun setElectrumServerListChangedListener(serverElectrumListChangedListener : ServerElectrumListChangedListener) {
        this.serverElectrumListChangedListener = serverElectrumListChangedListener
    }

    fun addEthServerListChangedListener(servereEthListChangedListener : ServerEthListChangedListener) {
        this.serverEthListChangedListeners.add(servereEthListChangedListener)
    }

    companion object {
        const val PREFS_ELECTRUM_SERVERS = "electrum_servers"
        const val PREFS_WAPI_SERVERS = "wapi_servers"
        const val PREFS_ETH_BB_SERVERS = "eth_bb_servers"
        const val ONION_DOMAIN = ".onion"
        const val PREFS_FIO_END_DATE = "fio_end_date"
        const val PREFS_FIO_START_DATE = "fio_start_date"

        const val TCP_TLS_PREFIX = "tcp-tls://"
        const val AMAZON_S3_STORAGE_ADDRESS = "https://mycelium-wallet.s3.amazonaws.com"
    }
}
