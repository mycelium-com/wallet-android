package com.mycelium.wallet

import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.HttpsEndpoint
import com.mycelium.net.TorHttpsEndpoint
import com.mycelium.wallet.external.partner.model.*
import com.mycelium.wapi.api.ServerElectrumListChangedListener
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import com.mycelium.wapi.wallet.IServerFioEventsPublisher
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.ServerEthListChangedListener
import com.mycelium.wapi.wallet.fio.FioTpidChangedListener
import com.mycelium.wapi.wallet.fio.ServerFioApiListChangedListener
import com.mycelium.wapi.wallet.fio.ServerFioHistoryListChangedListener
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.logging.Level
import java.util.logging.Logger


interface MyceliumNodesApi {
    @GET("/nodes-b.json")
    fun getNodes(): Call<MyceliumNodesResponse>

    @GET("/nodes-b-test.json")
    fun getNodesTest(): Call<MyceliumNodesResponse>
}

// A set of classes for parsing nodes-b.json file

// MyceliumNodesResponse is intended for parsing nodes-b.json file
class MyceliumNodesResponse(@SerializedName("BTC-testnet") val btcTestnet: BTCNetResponse,
                            @SerializedName("BTC-mainnet") val btcMainnet: BTCNetResponse,
                            @SerializedName("ETH-testnet") val ethTestnet: ETHNetResponse?,
                            @SerializedName("ETH-mainnet") val ethMainnet: ETHNetResponse?,
                            @SerializedName("FIO-mainnet") val fioMainnet: FIONetResponse?,
                            @SerializedName("FIO-testnet") val fioTestnet: FIONetResponse?,
                            @SerializedName("BTCV-testnet") val btcVTestnet: BTCNetResponse?,
                            @SerializedName("BTCV-mainnet") val btcVMainnet: BTCNetResponse?,
                            @SerializedName("partner-info") val partnerInfos: Map<String, PartnerInfo>?,
                            @SerializedName("Business") val partners: Map<String, PartnersLocalized>?,
                            @SerializedName("MediaFlow") val mediaFlowSettings: Map<String, MediaFlowContent>,
                            @SerializedName("Accounts") val accountsSettings: Map<String, AccountsContent>,
                            @SerializedName("MainMenu") val mainMenuSettings: Map<String, MainMenuContent>,
                            @SerializedName("Balance") val balanceSettings: Map<String, BalanceContent>,
                            @SerializedName("Buy-Sell") val buySellSettings: Map<String, BuySellContent>)

data class PartnerInfo(val id: String? = null,
                       val name: String? = null) : CommonContent()

// BTCNetResponse is intended for parsing nodes-b.json file
class BTCNetResponse(val electrumx: ElectrumXResponse, @SerializedName("WAPI") val wapi: WapiSectionResponse)

class ETHNetResponse(@SerializedName("blockbook-servers") val ethBBServers: EthServerResponse)

class FIONetResponse(@SerializedName("api-servers") val fioApiServers: FioServerResponse,
                     @SerializedName("history-servers") val fioHistoryServers: FioServerResponse,
                     @SerializedName("tpid") val tpid: String)

class WapiSectionResponse(val primary: Array<HttpsUrlResponse>)

class ElectrumXResponse(val primary: Array<UrlResponse>)

class EthServerResponse(val primary: Array<UrlResponse>)

class FioServerResponse(val primary: Array<UrlResponse>)

class UrlResponse(val url: String)

class HttpsUrlResponse(val url: String, @SerializedName("cert-sha1") val cert: String)

class WalletConfiguration(private val prefs: SharedPreferences,
                          val network: NetworkParameters) : IServerFioEventsPublisher {

    private val logger = Logger.getLogger(WalletConfiguration::class.java.simpleName)

    val gson = GsonBuilder().create()

    // Makes a request to S3 storage to retrieve nodes.json and parses it to extract electrum servers list
    fun updateConfig() {
        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT) {
            try {
                val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()
                val service = Retrofit.Builder()
                        .baseUrl(AMAZON_S3_STORAGE_ADDRESS)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build()
                        .create(MyceliumNodesApi::class.java)
                val resp =
                        if (!BuildConfig.DEBUG) {
                            service.getNodes()
                        } else {
                            service.getNodesTest()
                        }.execute()
                if (resp.isSuccessful) {
                    val myceliumNodesResponse = resp.body()

                    val electrumXnodes = if (network.isTestnet) {
                        myceliumNodesResponse?.btcTestnet
                    } else {
                        myceliumNodesResponse?.btcMainnet
                    }?.electrumx?.primary?.map { it.url }?.toSet()

                    val wapiNodes = if (network.isTestnet) {
                        myceliumNodesResponse?.btcTestnet
                    } else {
                        myceliumNodesResponse?.btcMainnet
                    }?.wapi?.primary

                    val electrumXBTCVnodes = if (network.isTestnet) {
                        myceliumNodesResponse?.btcVTestnet
                    } else {
                        myceliumNodesResponse?.btcVMainnet
                    }?.electrumx?.primary?.map { it.url }?.toSet()

                    val ethServersFromResponse = if (network.isTestnet) {
                        myceliumNodesResponse?.ethTestnet
                    } else {
                        myceliumNodesResponse?.ethMainnet
                    }?.ethBBServers?.primary?.map { it.url }?.toSet()

                    val fioApiServersFromResponse = if (network.isTestnet) {
                        myceliumNodesResponse?.fioTestnet
                    } else {
                        myceliumNodesResponse?.fioMainnet
                    }?.fioApiServers?.primary?.map { it.url }?.toSet()

                    val fioHistoryServersFromResponse = if (network.isTestnet) {
                        myceliumNodesResponse?.fioTestnet
                    } else {
                        myceliumNodesResponse?.fioMainnet
                    }?.fioHistoryServers?.primary?.map { it.url }?.toSet()

                    val fioTpid = if (network.isTestnet) {
                        myceliumNodesResponse?.fioTestnet
                    } else {
                        myceliumNodesResponse?.fioMainnet
                    }?.tpid

                    val prefEditor = prefs.edit()
                    electrumXnodes?.let {
                        prefEditor.putStringSet(PREFS_ELECTRUM_SERVERS, electrumXnodes)
                    }

                    wapiNodes?.let {
                        prefEditor.putString(PREFS_WAPI_SERVERS, gson.toJson(wapiNodes))
                    }

                    electrumXBTCVnodes?.let {
                        prefEditor.putStringSet(PREFS_ELECTRUM_BTCV_SERVERS, electrumXBTCVnodes)
                    }

                    val oldElectrum = electrumServers
                    val oldElectrumBTCV = electrumBTCVServers
                    val oldEth = ethBBServers
                    val oldFioApi = fioApiServers
                    val oldFioHistory = fioHistoryServers
                    val oldFioTpid = tpid

                    ethServersFromResponse?.let {
                        prefEditor.putStringSet(PREFS_ETH_BB_SERVERS, ethServersFromResponse)
                    }
                    fioApiServersFromResponse?.let {
                        prefEditor.putStringSet(PREFS_FIO_API_SERVERS, fioApiServersFromResponse)
                    }
                    fioHistoryServersFromResponse?.let {
                        prefEditor.putStringSet(PREFS_FIO_HISTORY_SERVERS, fioHistoryServersFromResponse)
                    }
                    fioTpid?.let {
                        prefEditor.putString(PREFS_FIO_TPID, it)
                    }
                    myceliumNodesResponse?.partnerInfos?.let {
                        it.entries.forEach {
                            prefEditor.putString("partner-info-${it.key}", gson.toJson(it.value))
                        }
                    }
                    fun <E> Map<String, E>.store(key: String) {
                        keys.forEach {
                            prefEditor.putString("$key-$it", gson.toJson(get(it)))
                        }
                    }
                    myceliumNodesResponse?.run {
                        partners?.store("partners")
                        mediaFlowSettings.store("mediaflow")
                        accountsSettings.store("accounts")
                        mainMenuSettings.store("mainmenu")
                        balanceSettings.store("balance")
                        buySellSettings.store("buysell")
                    }
                    prefEditor.apply()

                    if (oldElectrum != electrumServers) {
                        serverElectrumListChangedListener?.serverListChanged(getElectrumEndpoints().toTypedArray())
                    }

                    if (oldElectrumBTCV != electrumBTCVServers) {
                        serverElectrumVListChangedListener?.serverListChanged(getElectrumVEndpoints().toTypedArray())
                    }

                    if (oldEth != ethBBServers) {
                        for (serverEthListChangedListener in serverEthListChangedListeners) {
                            serverEthListChangedListener.serverListChanged(getBlockBookEndpoints().toTypedArray())
                        }
                    }

                    if (oldFioApi != fioApiServers) {
                        for (serverFioApiListChangedListener in serverFioApiListChangedListeners) {
                            serverFioApiListChangedListener.apiServerListChanged(getFioApiEndpoints().toTypedArray())
                        }
                    }
                    if (oldFioHistory != fioHistoryServers) {
                        for (serverFioHistoryListChangedListener in serverFioHistoryListChangedListeners) {
                            serverFioHistoryListChangedListener.historyServerListChanged(getFioHistoryEndpoints().toTypedArray())
                        }
                    }
                    if (oldFioTpid != tpid) {
                        for (fioTpidChangedListener in fioTpidChangedListeners) {
                            fioTpidChangedListener.tpidChanged(tpid)
                        }
                    }
                }
            } catch (ex: Exception) {
                logger.log(Level.WARNING, "Error when read configuration: ${ex.localizedMessage}")
            }
        }
    }

    // Returns the set of electrum servers
    private val electrumServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_SERVERS, mutableSetOf(*BuildConfig.ElectrumServers))!!

    // Returns the set of Wapi servers
    private val wapiServers: String
        get() = prefs.getString(PREFS_WAPI_SERVERS, BuildConfig.WapiServers)!!

    private val electrumBTCVServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_BTCV_SERVERS, mutableSetOf(*BuildConfig.ElectrumBTCVServers))!!

    // Returns the set of ethereum blockbook servers
    private val ethBBServers: Set<String>
        get() = prefs.getStringSet(PREFS_ETH_BB_SERVERS, mutableSetOf(*BuildConfig.EthBlockBook))!!

    private val fioApiServers: Set<String>
        get() = prefs.getStringSet(PREFS_FIO_API_SERVERS, mutableSetOf(*BuildConfig.FioApiServers))!!

    private val fioHistoryServers: Set<String>
        get() = prefs.getStringSet(PREFS_FIO_HISTORY_SERVERS, mutableSetOf(*BuildConfig.FioHistoryServers))!!

    private val tpid: String
        get() = prefs.getString(PREFS_FIO_TPID, BuildConfig.tpid)!!

    // Returns the list of TcpEndpoint objects
    fun getElectrumEndpoints(): List<TcpEndpoint> {
        return getElectrumEndpoints(electrumServers)
    }

    fun getElectrumVEndpoints(): List<TcpEndpoint> {
        return getElectrumEndpoints(electrumBTCVServers)
    }

    private fun getElectrumEndpoints(electrumServers: Set<String>): ArrayList<TcpEndpoint> {
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
        return getEndpoints(wapiServers)
    }

    private fun getEndpoints(wapiServers: String): List<HttpsEndpoint> {
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

    fun getFioApiEndpoints(): List<HttpsEndpoint> = fioApiServers.map { HttpsEndpoint(it) }
    fun getFioHistoryEndpoints(): List<HttpsEndpoint> = fioHistoryServers.map { HttpsEndpoint(it) }
    fun getFioTpid(): String = tpid

    private var serverElectrumListChangedListener: ServerElectrumListChangedListener? = null
    private var serverElectrumVListChangedListener: ServerElectrumListChangedListener? = null
    private var serverEthListChangedListeners: ArrayList<ServerEthListChangedListener> = arrayListOf()
    private var serverFioApiListChangedListeners: ArrayList<ServerFioApiListChangedListener> = arrayListOf()
    private var serverFioHistoryListChangedListeners: ArrayList<ServerFioHistoryListChangedListener> = arrayListOf()
    private var fioTpidChangedListeners: ArrayList<FioTpidChangedListener> = arrayListOf()

    fun getSupportedERC20Tokens(): Map<String, ERC20Token> {
        data class TokenData(val name: String, val symbol: String, val unitExponent: Int, val prodAddress: String, val testnetAddress: String = prodAddress)

        val tokens = listOf(
                //            TokenData("FAUCET", "FAU", 18, "0x55296f69f40Ea6d20E478533C15A6B08B654E759", "0xBA62BCfcAaFc6622853cca2BE6Ac7d845BC0f2Dc"),
            TokenData("0x", "ZRX", 18, "0xe41d2489571d322189246dafa5ebde1f4699f498", "0xd676189f67CAB2D5f9b16a5c0898A0E30ed86560"),
            TokenData("Tether USD", "USDT", 6, "0xdac17f958d2ee523a2206206994597c13d831ec7", "0x7c352ea63cefc099db667e848e1318878bbbcaaf"),
            TokenData("USD Coin", "USDC", 6, "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", "0x07865c6e87b9f70255377e024ace6630c1eaa37f"),
            TokenData("HuobiToken", "HT", 18, "0x6f259637dcd74c767781e37bc6133cd6a68aa161"),
            TokenData("Binance USD", "BUSD", 18, "0x4fabb145d64652a948d72533023f6e7a623c7c53", "0x5aff428676c5440719adcaf5ecdc7bdf2fbffbe7"),
            TokenData("Bitfinex LEO", "LEO", 18, "0x2af5d2ad76741191d15dfe7bf6ac92d4bd912ca3"),
            TokenData("TrueUSD", "TUSD", 18, "0x0000000000085d4780B73119b644AE5ecd22b376", "0xBA62BCfcAaFc6622853cca2BE6Ac7d845BC0f2Dc"),
            TokenData("ChainLink", "LINK", 18, "0x514910771AF9Ca656af840dff83E8264EcF986CA", "0x88bb053c5ddec8574fcd4d3b1692d43282a11281"),
            TokenData("Paxos Standard", "PAX", 18, "0x8E870D67F660D95d5be530380D0eC0bd388289E1"),
            TokenData("ZBToken", "ZB", 18, "0xBd0793332e9fB844A52a205A233EF27a5b34B927"),
            TokenData("OKB", "OKB", 18, "0x75231F58b43240C9718Dd58B4967c5114342a86c"),
            TokenData("OmiseGO", "OMG", 18, "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07"),
            TokenData("Basic Attention Token", "BAT", 18, "0x0D8775F648430679A709E98d2b0Cb6250d2887EF", "0x6742036904A63661A3feD2CAa9eF0890F5E58769"),
            TokenData("BIX Token", "BIX", 18, "0xb3104b4B9Da82025E8b9F8Fb28b3553ce2f67069"),
            TokenData("MCO", "MCO", 8, "0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d"),
            TokenData("Storj", "STORJ", 8, "0xB64ef51C888972c908CFacf59B47C1AfBC0Ab8aC", "0xc39a4fc849db2fef1e69d81933a0fe4e929717f7"),
            TokenData("Gemini dollar", "GUSD", 2, "0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd", "0x796628d910c97fac85ff6f2f328cefcd2e694a14"),
            TokenData("KyberNetwork", "KNC", 18, "0xdd974D5C2e2928deA5F71b9825b8b646686BD200"),
            TokenData("Kyber Network Crystal v2", "KNC", 18, "0xdeFA4e8a7bcBA345F687a2f1456F5Edd9CE97202"),
            TokenData("StatusNetwork", "SNT", 18, "0x744d70FDBE2Ba4CF95131626614a1763DF805B9E"),
            TokenData("Lambda", "LAMB", 18, "0x8971f9fd7196e5cEE2C1032B50F656855af7Dd26"),
            TokenData("chiliZ", "CHZ", 18, "0x3506424F91fD33084466F402d5D97f05F8e3b4AF"),
            TokenData("Augur", "REP", 18, "0x1985365e9f78359a9B6AD760e32412f4a445E862", "0xa9e5813de9de1732e52e1f2de416006cf7fe7320"),
            TokenData("IOSToken", "IOST", 18, "0xFA1a856Cfa3409CFa145Fa4e20Eb270dF3EB21ab"),
            TokenData("ICON", "ICX", 18, "0xb5A5F22694352C15B00323844aD545ABb2B11028"),
            TokenData("HUSD", "HUSD", 8, "0xdF574c24545E5FfEcb9a659c229253D4111d87e1"),
            TokenData("Streamr", "DATA", 18, "0x0Cf0Ee63788A0849fE5297F3407f701E122cC023"),
            TokenData("Loom Token", "LOOM", 18, "0xA4e8C3Ec456107eA67d3075bF9e3DF3A75823DB0"),
            TokenData("Matic", "MATIC", 18, "0x7D1AfA7B718fb893dB30A3aBc0Cfc608AaCfeBB0", "0xe2fbec39800e1e8fcef4225666e8c347f2585c76"),
            TokenData("Decentraland", "MANA", 18, "0x0F5D2fB29fb7d3CFeE444a200298f468908cC942", "0xce1892092c34832a41068884bbb5dab50e6f007a"),
            TokenData("aelf", "ELF", 18, "0xbf2179859fc6D5BEE9Bf9158632Dc51678a4100e"),
            TokenData("Dai Stablecoin", "DAI", 18, "0x6B175474E89094C44Da98b954EedeAC495271d0F", "0xb365f85a80a4164f8c6df5a35558c2e730019f8b"),
            TokenData("Uquid Coin", "UQC", 18, "0xD01DB73E047855Efb414e6202098C4Be4Cd2423B"),
            TokenData("HoloToken", "HOT", 18, "0x6c6EE5e31d828De241282B9606C8e98Ea48526E2"),
            TokenData("Origin Protocol", "OGN", 18, "0x8207c1FfC5B6804F6024322CcF34F29c3541Ae26"),
            TokenData("Tellor", "TRB", 18, "0x0Ba45A8b5d5575935B8158a88C631E9F9C95a2e5"),
            TokenData("Chromia", "CHR", 6, "0x915044526758533dfB918ecEb6e44bc21632060D"),
            TokenData("Crypto.com Coin", "CRO", 8, "0xA0b73E1Ff0B80914AB6fe0444E65848C4C34450b"),
            TokenData("EnjinCoin", "ENJ", 18, "0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c", "0x100ACe3cd8c091849E596abf1F351eC3e0EDD3C2"),
            TokenData("HedgeTrade", "HEDG", 18, "0xF1290473E210b2108A85237fbCd7b6eb42Cc654F"),
            TokenData("Maker", "MKR", 18, "0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2"),
            TokenData("Hyperion", "HYN", 18, "0xE99A894a69d7c2e3C92E61B64C505A6a57d2bC07"),
            TokenData("Synthetix Network", "SNX", 18, "0xC011A72400E58ecD99Ee497CF89E3775d4bd732F"),
            TokenData("Kucoin Shares", "KCS", 6, "0x039B5649A59967e3e936D7471f9c3700100Ee1ab"),
            TokenData("Numeraire", "NMR", 18, "0x1776e1F26f98b1A5dF9cD347953a26dd3Cb46671"),
            TokenData("Crypterium", "CRPT", 18, "0x80A7E048F37A50500351C204Cb407766fA3baE7f"),
            TokenData("Aave", "LEND", 18, "0x80fB784B7eD66730e8b1DBd9820aFD29931aab03"),
            TokenData("DigixDAO", "DGD", 9, "0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A"),
            TokenData("Quant", "QNT", 18, "0x4a220E6096B25EADb88358cb44068A3248254675", "0x516802d3e54b9fc7328dd0dadb76401dd1d55dc6"),
            TokenData("Golem", "GNT", 18, "0xa74476443119A942dE498590Fe1f2454d7D4aC0d"),
            TokenData("Metal", "MTL", 8, "0xF433089366899D83a9f26A773D59ec7eCF30355e"),
            TokenData("Nexo", "NEXO", 18, "0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206"),
            TokenData("Theta Token", "THETA", 18, "0x3883f5e181fccaF8410FA61e12b59BAd963fb645"),
            TokenData("Bytom", "BTM", 8, "0xcB97e65F07DA24D46BcDD078EBebd7C6E6E3d750"),
            TokenData("Aragon", "ANT", 18, "0x960b236A07cf122663c4303350609A66A7B288C0", "0xc72c77bee61caa5f00ced8d86e64d0fe380c6cf9"),
            TokenData("Swipe", "SXP", 18, "0x8CE9137d39326AD0cD6491fb5CC0CbA0e089b6A9"),
            TokenData("Gnosis", "GNO", 18, "0x6810e776880C02933D47DB1b9fc05908e5386b96"),
            TokenData("SwissBorg", "CHSB", 8, "0xba9d4199faB4f26eFE3551D490E3821486f135Ba"),
            TokenData("Function X", "FX", 18, "0x8c15Ef5b4B21951d50E53E4fbdA8298FFAD25057"),
            TokenData("Sai Stablecoin v1.0", "SAI", 18, "0x89d24A6b4CcB1B6fAA2625fE562bDD9a23260359"),
            TokenData("Mixin", "XIN", 18, "0xA974c709cFb4566686553a20790685A47acEAA33"),
            TokenData("Republic", "REN", 18, "0x408e41876cCCDC0F92210600ef50372656052a38"),
            TokenData("Centrality Token", "CENNZ", 18, "0x1122B6a0E00DCe0563082b6e2953f3A943855c1F"),
            TokenData("Abyss", "ABYSS", 18, "0x0E8d6b471e332F140e7d9dbB99E5E3822F728DA6"),
            TokenData("Aeron", "ARN", 8, "0xBA5F11b16B155792Cf3B2E6880E8706859A8AEB6"),
            TokenData("ATLANT", "ATL", 18, "0x78B7FADA55A64dD895D8c8c35779DD8b67fA8a05"),
            TokenData("Atlas Token", "ATLS", 18, "0xd36E9F8F194A47B10aF16C7656a68EBa1DFe88e4"),
            TokenData("Coinlancer", "CL", 18, "0xe81D72D14B1516e68ac3190a46C93302Cc8eD60f"),
            TokenData("DENT", "DENT", 8, "0x3597bfD533a99c9aa083587B074434E61Eb0A258"),
            TokenData("Dentacoin", "DCN", 0, "0x08d32b0da63e2C3bcF8019c9c5d849d7a9d791e6"),
            TokenData("Polymath", "POLY", 18, "0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC"),
            TokenData("Enigma", "ENG", 8, "0xf0Ee6b27b759C9893Ce4f094b49ad28fd15A23e4"),
            TokenData("RipioCreditNetwork", "RCN", 18, "0xF970b8E36e23F7fC3FD752EeA86f8Be8D83375A6"),
            TokenData("Flexacoin", "FXC", 18, "0x4a57E687b9126435a9B19E4A802113e266AdeBde"),
            TokenData("Rocket Pool", "RPL", 18, "0xB4EFd85c19999D84251304bDA99E90B92300Bd93"),
            TokenData("LAtoken", "LA", 18, "0xE50365f5D679CB98a1dd62D6F6e58e59321BcdDf"),
            TokenData("Bread", "BRD", 18, "0x558EC3152e2eb2174905cd19AeA4e34A23DE9aD6", "0x88bfa6f940acb21357c45538f725a38ac0372325"),
            TokenData("Tether GOLD", "XAUT", 6, "0x4922a015c4407F87432B179bb209e125432E4a2A"),
            TokenData("Mycelium Token", "MT", 7, "0x364f56e35e75227516878cc249f11ea9b3e41b09", "0x71b59f06a47c9c403d0e3ec303008bcf78fa9af6"),
            TokenData("Fantom Token", "FTM", 18, "0x4E15361FD6b4BB609Fa63C81A2be19d873717870"),
            TokenData("SHIBA INU", "SHIB", 18, "0x95aD61b0a150d79219dCF64E1E6Cc01f0B64C4cE"),
            TokenData("Paxos Gold", "PAXG", 18, "0x45804880De22913dAFE09f4980848ECE6EcbAf78"),
            TokenData("Wrapped BTC", "WBTC", 8, "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599"),
            TokenData("Loopring", "LRC", 18, "0xBBbbCA6A901c926F240b89EacB641d8Aec7AEafD"),
            TokenData("Curve DAO Token", "CRV", 18, "0xD533a949740bb3306d119CC777fa900bA034cd52"),
            TokenData("HEX", "HEX", 8, "0x2b591e99afE9f32eAA6214f7B7629768c40Eeb39"),
            TokenData("SAND", "SAND", 18, "0x3845badAde8e6dFF049820680d1F14bD3903a5d0"),
            TokenData("FTT", "FTT", 18, "0x50D1c9771902476076eCFc8B2A83Ad6b9355a4c9"),
            TokenData("Aave Token", "AAVE", 18, "0x7Fc66500c84A76Ad7e9c93437bFc5Ac33E2DDaE9"),
            TokenData("Uniswap", "UNI", 18, "0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984"),
            TokenData("Axie Infinity Shard", "AXS", 18, "0xBB0E17EF65F82Ab018d8EDd776e8DD940327B28b"),
            TokenData("RLC", "RLC", 9, "0x607F4C5BB672230e8672085532f7e901544a7375"),
            TokenData("yearn.finance", "YFI", 18, "0x0bc529c00C6401aEF6D220BE8C6Ea1667F6Ad93e"),
            TokenData("Graph Token", "GRT", 18, "0xc944E90C64B2c07662A292be6244BDf05Cda44a7"),
            TokenData("Civic", "CVC", 8, "0x41e5560054824eA6B0732E656E3Ad64E20e94E45"),
            TokenData("Gala", "GALA", 8, "0x15D4c048F83bd7e37d49eA4C83a07267Ec4203dA"),
            TokenData("Illuvium", "ILV", 18, "0x767FE9EDC9E0dF98E07454847909b5E959D7ca0E"),
            TokenData("SushiToken", "SUSHI", 18, "0x6B3595068778DD592e39A122f4f5a5cF09C90fE2"),
            TokenData("1INCH Token", "1INCH", 18, "0x111111111117dC0aa78b770fA6A738034120C302"),
            TokenData("PowerLedger", "POWR", 6, "0x595832F8FC6BF59c85C527fEC3740A1b7a361269"),
            TokenData("Bancor", "BNT", 18, "0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C"),
            TokenData("Compound", "COMP", 18, "0xc00e94Cb662C3520282E6f5717214004A7f26888"),
            TokenData("Render Token", "RNDR", 18, "0x6De037ef9aD2725EB40118Bb1702EBb27e4Aeb24"),
            TokenData("dYdX", "DYDX", 18, "0x92D6C1e31e14520e676a687F0a93788B716BEff5"),
            TokenData("Ankr Network", "ANKR", 18, "0x8290333ceF9e6D528dD5618Fb97a76f268f3EDD4"),
            TokenData("XYO", "XYO", 18, "0x55296f69f40Ea6d20E478533C15A6B08B654E758")
        )
        val namePostfix = if (BuildConfig.FLAVOR == "prodnet") "" else " test"
        return tokens
            .map { ERC20Token(it.name + namePostfix, it.symbol, it.unitExponent, if (BuildConfig.FLAVOR == "prodnet") it.prodAddress else it.testnetAddress) }
            .associateBy { it.name }
    }

    fun setElectrumServerListChangedListener(serverElectrumListChangedListener: ServerElectrumListChangedListener) {
        this.serverElectrumListChangedListener = serverElectrumListChangedListener
    }

    fun setElectrumVServerListChangedListener(serverElectrumVListChangedListener: ServerElectrumListChangedListener) {
        this.serverElectrumVListChangedListener = serverElectrumVListChangedListener
    }

    fun addEthServerListChangedListener(serverEthListChangedListener: ServerEthListChangedListener) {
        this.serverEthListChangedListeners.add(serverEthListChangedListener)
    }

    override fun setFioServerListChangedListeners(serverFioApiListChangedListener: ServerFioApiListChangedListener,
                                                  serverFioHistoryListChangedListener: ServerFioHistoryListChangedListener) {
        this.serverFioApiListChangedListeners.add(serverFioApiListChangedListener)
        this.serverFioHistoryListChangedListeners.add(serverFioHistoryListChangedListener)
    }

    override fun setFioTpidChangedListener(fioTpidChangedListener: FioTpidChangedListener) {
        this.fioTpidChangedListeners.add(fioTpidChangedListener)
    }

    companion object {
        const val PREFS_ELECTRUM_SERVERS = "electrum_servers"
        const val PREFS_WAPI_SERVERS = "wapi_servers"
        const val PREFS_ELECTRUM_BTCV_SERVERS = "electrum_btcv_servers"
        const val PREFS_ETH_BB_SERVERS = "eth_bb_servers"
        const val PREFS_FIO_API_SERVERS = "fio_api_servers"
        const val PREFS_FIO_HISTORY_SERVERS = "fio_history_servers"
        const val PREFS_FIO_TPID = "fio_tpid"
        const val ONION_DOMAIN = ".onion"

        const val TCP_TLS_PREFIX = "tcp-tls://"
        const val AMAZON_S3_STORAGE_ADDRESS = "https://wallet-config.mycelium.com"
    }
}
