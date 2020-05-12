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
            ERC20Token("TrueUSD", "TUSD", 18, "0x0000000000085d4780B73119b644AE5ecd22b376"),
            ERC20Token("ChainLink", "LINK", 18, "0x514910771AF9Ca656af840dff83E8264EcF986CA"),
            ERC20Token("Paxos Standard", "PAX", 18, "0x8E870D67F660D95d5be530380D0eC0bd388289E1"),
            ERC20Token("ZBToken", "ZB", 18, "0xBd0793332e9fB844A52a205A233EF27a5b34B927"),
            ERC20Token("OKB", "OKB", 18, "0x75231F58b43240C9718Dd58B4967c5114342a86c"),
            ERC20Token("OmiseGO", "OMG", 18, "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07"),
            ERC20Token("Basic Attention Token", "BAT", 18, "0x0D8775F648430679A709E98d2b0Cb6250d2887EF"),
            ERC20Token("BIX Token", "BIX", 18, "0xb3104b4B9Da82025E8b9F8Fb28b3553ce2f67069"),
            ERC20Token("MCO", "MCO", 8, "0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d"),
            ERC20Token("Storj", "STORJ", 8, "0xB64ef51C888972c908CFacf59B47C1AfBC0Ab8aC"),
            ERC20Token("Gemini dollar", "GUSD", 2, "0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd"),
            ERC20Token("KyberNetwork", "KNC", 18, "0xdd974D5C2e2928deA5F71b9825b8b646686BD200"),
            ERC20Token("StatusNetwork", "SNT", 18, "0x744d70FDBE2Ba4CF95131626614a1763DF805B9E"),
            ERC20Token("Lambda", "LAMB", 18, "0x8971f9fd7196e5cEE2C1032B50F656855af7Dd26"),
            ERC20Token("chiliZ", "CHZ", 18, "0x3506424F91fD33084466F402d5D97f05F8e3b4AF"),
            ERC20Token("Augur", "REP", 18, "0x1985365e9f78359a9B6AD760e32412f4a445E862"),
            ERC20Token("IOSToken", "IOST", 18, "0xFA1a856Cfa3409CFa145Fa4e20Eb270dF3EB21ab"),
            ERC20Token("ICON", "ICX", 18, "0xb5A5F22694352C15B00323844aD545ABb2B11028"),
            ERC20Token("HUSD", "HUSD", 8, "0xdF574c24545E5FfEcb9a659c229253D4111d87e1"),
            ERC20Token("Streamr", "DATA", 18, "0x0Cf0Ee63788A0849fE5297F3407f701E122cC023"),
            ERC20Token("Loom Network", "LOOM", 18, "0xA4e8C3Ec456107eA67d3075bF9e3DF3A75823DB0"),
            ERC20Token("Matic", "MATIC", 18, "0x7D1AfA7B718fb893dB30A3aBc0Cfc608AaCfeBB0"),
            ERC20Token("Decentraland", "MANA", 18, "0x0F5D2fB29fb7d3CFeE444a200298f468908cC942"),
            ERC20Token("aelf", "ELF", 18, "0xbf2179859fc6D5BEE9Bf9158632Dc51678a4100e"),
            ERC20Token("Dai Stablecoin", "DAI", 18, "0x6B175474E89094C44Da98b954EedeAC495271d0F"),
            ERC20Token("Uquid Coin", "UQC", 18, "0xD01DB73E047855Efb414e6202098C4Be4Cd2423B"),
            ERC20Token("HoloToken", "HOT", 18, "0x6c6EE5e31d828De241282B9606C8e98Ea48526E2"),
            ERC20Token("Origin Protocol", "OGN", 18, "0x8207c1FfC5B6804F6024322CcF34F29c3541Ae26"),
            ERC20Token("Tellor", "TRB", 18, "0x0Ba45A8b5d5575935B8158a88C631E9F9C95a2e5"),
            ERC20Token("Chromia", "CHR", 6, "0x915044526758533dfB918ecEb6e44bc21632060D"),
            ERC20Token("Crypto.com Coin", "CRO", 8, "0xA0b73E1Ff0B80914AB6fe0444E65848C4C34450b"),
            ERC20Token("EnjinCoin", "ENJ", 18, "0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c"),
            ERC20Token("HedgeTrade", "HEDG", 18, "0xF1290473E210b2108A85237fbCd7b6eb42Cc654F"),
            ERC20Token("Maker", "MKR", 18, "0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2"),
            ERC20Token("Hyperion", "HYN", 18, "0xE99A894a69d7c2e3C92E61B64C505A6a57d2bC07"),
            ERC20Token("Synthetix Network", "SNX", 18, "0xC011A72400E58ecD99Ee497CF89E3775d4bd732F"),
            ERC20Token("Kucoin Shares", "KCS", 6, "0x039B5649A59967e3e936D7471f9c3700100Ee1ab"),
            ERC20Token("Numeraire", "NMR", 18, "0x1776e1F26f98b1A5dF9cD347953a26dd3Cb46671"),
            ERC20Token("Crypterium", "CRPT", 18, "0x80A7E048F37A50500351C204Cb407766fA3baE7f"),
            ERC20Token("Aave", "LEND", 18, "0x80fB784B7eD66730e8b1DBd9820aFD29931aab03"),
            ERC20Token("DigixDAO", "DGD", 9, "0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A"),
            ERC20Token("Quant", "QNT", 18, "0x4a220E6096B25EADb88358cb44068A3248254675"),
            ERC20Token("Golem", "GNT", 18, "0xa74476443119A942dE498590Fe1f2454d7D4aC0d"),
            ERC20Token("Metal", "MTL", 8, "0xF433089366899D83a9f26A773D59ec7eCF30355e"),
            ERC20Token("Nexo", "NEXO", 18, "0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206"),
            ERC20Token("Theta Token", "THETA", 18, "0x3883f5e181fccaF8410FA61e12b59BAd963fb645"),
            ERC20Token("Bytom", "BTM", 8, "0xcB97e65F07DA24D46BcDD078EBebd7C6E6E3d750"),
            ERC20Token("Aragon", "ANT", 18, "0x960b236A07cf122663c4303350609A66A7B288C0"),
            ERC20Token("Swipe", "SXP", 18, "0x8CE9137d39326AD0cD6491fb5CC0CbA0e089b6A9"),
            ERC20Token("Gnosis", "GNO", 18, "0x6810e776880C02933D47DB1b9fc05908e5386b96"),
            ERC20Token("SwissBorg", "CHSB", 8, "0xba9d4199faB4f26eFE3551D490E3821486f135Ba"),
            ERC20Token("Function X", "FX", 18, "0x8c15Ef5b4B21951d50E53E4fbdA8298FFAD25057"),
            ERC20Token("Sai Stablecoin v1.0", "SAI", 18, "0x89d24A6b4CcB1B6fAA2625fE562bDD9a23260359"),
            ERC20Token("Mixin", "XIN", 18, "0xA974c709cFb4566686553a20790685A47acEAA33"),
            ERC20Token("Republic", "REN", 18, "0x408e41876cCCDC0F92210600ef50372656052a38"),
            ERC20Token("Centrality Token", "CENNZ", 18, "0x1122B6a0E00DCe0563082b6e2953f3A943855c1F"),
            ERC20Token("Abyss", "ABYSS", 18, "0x0E8d6b471e332F140e7d9dbB99E5E3822F728DA6"),
            ERC20Token("Aeron", "ARN", 8, "0xBA5F11b16B155792Cf3B2E6880E8706859A8AEB6"),
            ERC20Token("ATLANT", "ATL", 18, "0x78B7FADA55A64dD895D8c8c35779DD8b67fA8a05"),
            ERC20Token("Atlas Token", "ATLS", 18, "0xd36E9F8F194A47B10aF16C7656a68EBa1DFe88e4"),
            ERC20Token("Coinlancer", "CL", 18, "0xe81D72D14B1516e68ac3190a46C93302Cc8eD60f"),
            ERC20Token("DENT", "DENT", 8, "0x3597bfD533a99c9aa083587B074434E61Eb0A258"),
            ERC20Token("Dentacoin", "DCN", 0, "0x08d32b0da63e2C3bcF8019c9c5d849d7a9d791e6"),
            ERC20Token("Polymath", "POLY", 18, "0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC"),
            ERC20Token("Enigma", "ENG", 8, "0xf0Ee6b27b759C9893Ce4f094b49ad28fd15A23e4"),
            ERC20Token("RipioCreditNetwork", "RCN", 18, "0xF970b8E36e23F7fC3FD752EeA86f8Be8D83375A6"),
            ERC20Token("Flexacoin", "FXC", 18, "0x4a57E687b9126435a9B19E4A802113e266AdeBde"),
            ERC20Token("Rocket Pool", "RPL", 18, "0xB4EFd85c19999D84251304bDA99E90B92300Bd93"),
            ERC20Token("LAtoken", "LA", 18, "0xE50365f5D679CB98a1dd62D6F6e58e59321BcdDf"),
            ERC20Token("Bread", "BRD", 18, "0x558EC3152e2eb2174905cd19AeA4e34A23DE9aD6"),
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
