package com.mycelium.wallet

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.HttpsEndpoint
import com.mycelium.net.TorHttpsEndpoint
import com.mycelium.wallet.external.partner.model.AccountsContent
import com.mycelium.wallet.external.partner.model.BalanceContent
import com.mycelium.wallet.external.partner.model.BuySellContent
import com.mycelium.wallet.external.partner.model.CommonContent
import com.mycelium.wallet.external.partner.model.MainMenuContent
import com.mycelium.wallet.external.partner.model.MediaFlowContent
import com.mycelium.wallet.external.partner.model.PartnersLocalized
import com.mycelium.wapi.api.ServerElectrumListChangedListener
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.ServerEthListChangedListener
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.ConfigObfuscation
import com.mycelium.wapi.wallet.tron.remote.ServerTronListChangedListener
import com.mycelium.wapi.wallet.tron.remote.TronEndpoint
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger


interface MyceliumNodesApi {
    @GET("/nodes-b-v3160200.json")
    fun getNodes(): Call<MyceliumNodesResponse>

    @GET("/nodes-b-v3160200-test.json")
    fun getNodesTest(): Call<MyceliumNodesResponse>
}

// A set of classes for parsing nodes-b.json file

// MyceliumNodesResponse is intended for parsing nodes-b.json file
class MyceliumNodesResponse(@SerializedName("BTC-testnet") val btcTestnet: BTCNetResponse,
                            @SerializedName("BTC-mainnet") val btcMainnet: BTCNetResponse,
                            @SerializedName("ETH-testnet") val ethTestnet: ETHNetResponse?,
                            @SerializedName("ETH-mainnet") val ethMainnet: ETHNetResponse?,
                            @SerializedName("TRX-testnet") val trxTestnet: TRXNetResponse?,
                            @SerializedName("TRX-mainnet") val trxMainnet: TRXNetResponse?,
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

/**
 * TRON section in the WalletKMP `wallet-config` layout so both apps share one config snippet:
 * `"TRX-mainnet": { "endpoints": [ { "url", "type", "header-key", "header-value", "priority", "rps" } ] }`.
 * `header-value` may be obfuscated (`enc1_`/`enc2_`, see [ConfigObfuscation]).
 */
class TRXNetResponse(val endpoints: List<TrxEndpointResponse> = emptyList())

data class TrxEndpointResponse(
    val url: String,
    /** DEFAULT (TronGrid full HTTP API, the only type this app speaks), REST, GRPC … */
    val type: String? = null,
    @SerializedName("header-key") val headerKey: String? = null,
    @SerializedName("header-value") val headerValue: String? = null,
    val priority: Int = 0,
    val rps: Int? = null,
)

class WapiSectionResponse(val primary: Array<HttpsUrlResponse>)

class ElectrumXResponse(val primary: Array<UrlResponse>, val tor: Array<UrlResponse>)

class EthServerResponse(val primary: Array<UrlResponse>)

class UrlResponse(val url: String)

class HttpsUrlResponse(val url: String, @SerializedName("cert-sha1") val cert: String)

class WalletConfiguration(private val prefs: SharedPreferences,
                          val network: NetworkParameters) {

    private val logger = Logger.getLogger(WalletConfiguration::class.java.simpleName)

    val gson = GsonBuilder().create()

    fun loadFromAssetsIfNeed() {
        if (!prefs.contains(PREFS_ELECTRUM_SERVERS)) {
            val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()
            val file = WalletApplication.getInstance().assets.open("nodes-b.json")
            val myceliumNodesResponse =
                gson.fromJson(InputStreamReader(file, "UTF-8"), MyceliumNodesResponse::class.java)
            saveToPrefs(myceliumNodesResponse, gson)
        }
    }

    // Makes a request to S3 storage to retrieve nodes.json and parses it to extract electrum servers list
    fun updateConfig() {
        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT) {
            val oldElectrum = electrumServers
            val oldEth = ethBBServers
            val oldTron = tronServers
            try {
                val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()

                val httpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .configureSSLSocket()
                    .build()
                val service = Retrofit.Builder()
                        .baseUrl(AMAZON_S3_STORAGE_ADDRESS)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient)
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

                    saveToPrefs(myceliumNodesResponse, gson)

                    if (oldElectrum != electrumServers) {
                        serverElectrumListChangedListener?.serverListChanged(getElectrumEndpoints())
                    }

                    if (oldEth != ethBBServers) {
                        for (serverEthListChangedListener in serverEthListChangedListeners) {
                            serverEthListChangedListener.serverListChanged(getBlockBookEndpoints().toTypedArray())
                        }
                    }

                    if (oldTron != tronServers) {
                        for (listener in serverTronListChangedListeners) {
                            listener.serverListChanged(getTronEndpoints())
                        }
                    }

                }
            } catch (ex: Exception) {
                logger.log(Level.WARNING, "Error when read configuration: ${ex.localizedMessage}")
            }
        }
    }

    private fun saveToPrefs(myceliumNodesResponse: MyceliumNodesResponse?, gson: Gson) {
        val electrumXnodes = if (network.isTestnet) {
            myceliumNodesResponse?.btcTestnet
        } else {
            myceliumNodesResponse?.btcMainnet
        }?.electrumx?.primary?.map { it.url }?.toSet()

        val electrumXTorNodes = if (network.isTestnet) {
            myceliumNodesResponse?.btcTestnet
        } else {
            myceliumNodesResponse?.btcMainnet
        }?.electrumx?.tor?.map { it.url }?.toSet()

        val wapiNodes = if (network.isTestnet) {
            myceliumNodesResponse?.btcTestnet
        } else {
            myceliumNodesResponse?.btcMainnet
        }?.wapi?.primary

        val ethServersFromResponse = if (network.isTestnet) {
            myceliumNodesResponse?.ethTestnet
        } else {
            myceliumNodesResponse?.ethMainnet
        }?.ethBBServers?.primary?.map { it.url }?.toSet()

        val tronServersFromResponse = if (network.isTestnet) {
            myceliumNodesResponse?.trxTestnet
        } else {
            myceliumNodesResponse?.trxMainnet
        }?.endpoints

        val prefEditor = prefs.edit()
        electrumXnodes?.let {
            prefEditor.putStringSet(PREFS_ELECTRUM_SERVERS, electrumXnodes)
        }
        electrumXTorNodes?.let {
            prefEditor.putStringSet(PREFS_ELECTRUM_TOR_SERVERS, electrumXTorNodes)
        }

        wapiNodes?.let {
            prefEditor.putString(PREFS_WAPI_SERVERS, gson.toJson(wapiNodes))
        }

        ethServersFromResponse?.let {
            prefEditor.putStringSet(PREFS_ETH_BB_SERVERS, ethServersFromResponse)
        }
        tronServersFromResponse?.let {
            prefEditor.putString(PREFS_TRON_SERVERS, gson.toJson(it))
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
    }

    // Returns the set of electrum servers
    private val electrumServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_SERVERS, mutableSetOf(*BuildConfig.ElectrumServers))!!

    private val electrumTorServers: Set<String>
        get() = prefs.getStringSet(PREFS_ELECTRUM_TOR_SERVERS, setOf())!!

    // Returns the set of Wapi servers
    private val wapiServers: String
        get() = prefs.getString(PREFS_WAPI_SERVERS, BuildConfig.WapiServers)!!

    // Returns the set of ethereum blockbook servers
    private val ethBBServers: Set<String>
        get() = prefs.getStringSet(PREFS_ETH_BB_SERVERS, mutableSetOf(*BuildConfig.EthBlockBook))!!

    // TRON endpoints as stored from the config (JSON list of TrxEndpointResponse); null when absent/unparsable
    private val tronServers: List<TrxEndpointResponse>?
        get() = prefs.getString(PREFS_TRON_SERVERS, null)?.let {
            try {
                gson.fromJson(it, Array<TrxEndpointResponse>::class.java).toList()
            } catch (e: Exception) {
                null
            }
        }?.takeIf { it.isNotEmpty() }

    // Returns the list of TcpEndpoint objects
    fun getElectrumEndpoints(): List<TcpEndpoint> {
        return getElectrumEndpoints(electrumServers)
    }

    fun getElectrumTorEndpoints(): List<TcpEndpoint> = getElectrumEndpoints(electrumTorServers)

    private fun getElectrumEndpoints(electrumServers: Set<String>): List<TcpEndpoint> =
        electrumServers.mapNotNull {
            try {
                it.replace(TCP_TLS_PREFIX, "").split(":").let { strs ->
                    TcpEndpoint(strs[0], strs[1].toInt())
                }
            } catch (ex: Exception) {
                // We ignore endpoints given in wrong format
                null
            }
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

    /**
     * HTTP endpoints for [com.mycelium.wapi.wallet.tron.remote.TronBlockchainService], ordered by priority.
     * Only `DEFAULT` (TronGrid-compatible full API) entries are used — `GRPC`/`REST` entries of the shared
     * KMP config are skipped. Falls back to the built-in TronGrid list + build-time key.
     */
    fun getTronEndpoints(): List<TronEndpoint> {
        val buildKey = BuildConfig.TronGridApiKey.takeIf { it.isNotBlank() }
        val fromConfig = tronServers.orEmpty()
            .filter { it.type.isNullOrBlank() || it.type.equals("DEFAULT", ignoreCase = true) }
            .sortedBy { it.priority }
            .map {
                val key = ConfigObfuscation.decode(it.headerValue)?.takeIf { k -> k.isNotBlank() }
                // A keyless TronGrid entry inherits the build-time key (if any) so keyed access survives config updates
                val apiKey = key ?: buildKey?.takeIf { _ -> it.headerKey.isNullOrBlank() || it.headerKey == TronEndpoint.DEFAULT_API_KEY_HEADER }
                TronEndpoint(it.url, apiKey, it.headerKey?.takeIf { h -> h.isNotBlank() } ?: TronEndpoint.DEFAULT_API_KEY_HEADER,
                    it.priority, it.rps)
            }
            // The same URL+key may appear twice in the KMP layout (REST + DEFAULT); keep one
            .distinctBy { it.baseUrl.trimEnd('/') to it.apiKey }
        return fromConfig.ifEmpty { BuildConfig.TronServers.map { TronEndpoint(it, buildKey) } }
    }

    private var serverElectrumListChangedListener: ServerElectrumListChangedListener? = null
    private var serverEthListChangedListeners: ArrayList<ServerEthListChangedListener> = arrayListOf()
    private var serverTronListChangedListeners: ArrayList<ServerTronListChangedListener> = arrayListOf()

    //https://github.com/bokkypoobah/WeenusTokenFaucet
    fun getSupportedERC20Tokens(): Map<String, ERC20Token> {
        val namePostfix = if (Utils.isProdnet()) "" else " test"
        val symbolPrefix = if (Utils.isProdnet()) "" else "t"
        return TOKENS
            // Mainnet contracts don't exist on Sepolia — testnet only lists tokens
            // that have a testnet address of their own. Falling back to the mainnet
            // address offered ~100 tokens whose contract has no code on Sepolia:
            // their balance could only ever read 0, and a "send" to an address
            // without code succeeds as a plain 0-value transfer, burning gas while
            // moving nothing.
            .filter { if (Utils.isProdnet()) it.prodAddress.isNotEmpty() else !it.testnetAddress.isNullOrEmpty() }
            .map { ERC20Token(it.name + namePostfix, "$symbolPrefix${it.symbol}", it.unitExponent,
                if (Utils.isProdnet()) it.prodAddress else it.testnetAddress!!) }
            .associateBy { it.name }
    }

    fun setElectrumServerListChangedListener(serverElectrumListChangedListener: ServerElectrumListChangedListener) {
        this.serverElectrumListChangedListener = serverElectrumListChangedListener
    }

    fun addEthServerListChangedListener(serverEthListChangedListener: ServerEthListChangedListener) {
        this.serverEthListChangedListeners.add(serverEthListChangedListener)
    }

    fun addTronServerListChangedListener(listener: ServerTronListChangedListener) {
        this.serverTronListChangedListeners.add(listener)
    }

    fun getSupportedTRC20Tokens(): Map<String, TRC20Token> {
        val namePostfix = if (Utils.isProdnet()) "" else " test"
        val symbolPrefix = if (Utils.isProdnet()) "" else "t"
        return TRC20_TOKENS
            // Mainnet contracts don't exist on Nile — testnet only lists tokens with a testnet address
            .filter { if (Utils.isProdnet()) it.prodAddress.isNotEmpty() else !it.testnetAddress.isNullOrEmpty() }
            .map { TRC20Token(it.name + namePostfix, "$symbolPrefix${it.symbol}", it.unitExponent,
                if (Utils.isProdnet()) it.prodAddress else it.testnetAddress!!) }
            .associateBy { it.name }
    }

    /**
     * Whether a TRC20 account's stored label is a default the app gave it before labels became
     * "USDT(TRC20)": the token's name or one of its [TokenData.legacyNames], possibly with a
     * " (n)" counter. Anything else is the user's own name and is never overwritten.
     */
    fun isDefaultTRC20Label(contractAddress: String, storedLabel: String): Boolean =
        isDefaultLabel(TRC20_TOKENS, contractAddress, storedLabel, ignoreCase = false)

    /** [isDefaultTRC20Label] for ERC20 accounts (hex contract addresses, compared case-insensitively). */
    fun isDefaultERC20Label(contractAddress: String, storedLabel: String): Boolean =
        isDefaultLabel(TOKENS, contractAddress, storedLabel, ignoreCase = true)

    private fun isDefaultLabel(tokens: List<TokenData>, contractAddress: String, storedLabel: String,
                               ignoreCase: Boolean): Boolean {
        val namePostfix = if (Utils.isProdnet()) "" else " test"
        val token = tokens.firstOrNull {
            contractAddress.equals(if (Utils.isProdnet()) it.prodAddress else it.testnetAddress, ignoreCase)
        } ?: return false
        return (listOf(token.name) + token.legacyNames).any { name ->
            val default = name + namePostfix
            storedLabel == default || Regex("^" + Regex.escape(default) + " \\(\\d+\\)$").matches(storedLabel)
        }
    }

    companion object {
        const val PREFS_ELECTRUM_SERVERS = "electrum_servers"
        const val PREFS_ELECTRUM_TOR_SERVERS = "electrum_tor_servers"
        const val PREFS_WAPI_SERVERS = "wapi_servers"
        const val PREFS_ETH_BB_SERVERS = "eth_bb_servers"
        const val PREFS_TRON_SERVERS = "tron_servers"
        const val ONION_DOMAIN = ".onion"

        const val TCP_TLS_PREFIX = "tcp-tls://"
        const val AMAZON_S3_STORAGE_ADDRESS = "https://wallet-config.mycelium.com"

        // TRC20 tokens (contract addresses on TRON mainnet; testnet = Nile)
        val TRC20_TOKENS = listOf(
            TokenData("USDT", "USDT", 6, "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
                legacyNames = listOf("Tether USD", "Tether USDT")),
            TokenData("USD Coin", "USDC", 6, "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"),
            TokenData("USD1", "USD1", 18, "TPFqcBAaaUMCSVRCqPaQ9QnzKhmuoLR6Rc", "TNuoKL1ni8aoshfFL1ASca1Gou9RXwAzfn"),
            TokenData("Decentralized USD", "USDD", 18, "TXDk8mbtRbXeYuMNS83CfKPaYYT8XWv9Hz"),
            TokenData("TrueUSD", "TUSD", 18, "TUpMhErZL2fhh4sVNULAbNKLokS4GjC1F4"),
            TokenData("WINk", "WIN", 6, "TLa2f6VPqDgRE67v1736s7bJ8Ray5wYjU7"),
            TokenData("SUN", "SUN", 18, "TSSMHYeV2uE9qYH95DqyoCuNCzEL1NvU3S"),
            TokenData("JUST", "JST", 18, "TCFLL5dx5ZJdKnWuesXxi1VPwjLVmWZZy9"),
            TokenData("USDJ", "USDJ", 18, "TMwFHYXLJaRUPeW6421aqXL4ZEzPRFGkGT"),
            TokenData("Wrapped TRX", "WTRX", 6, "TNUC9Qb1rRpS5CbWLmNMxXBjyFoydXjWFR"),
            TokenData("BitTorrent", "BTT", 18, "TAFjULxiVgT4qWVNAx5f7kaNdS3LDZTYaj"),
        )

        val TOKENS = listOfNotNull(
            if (BuildConfig.DEBUG)
                TokenData("PayPal USD", "PYUSD", 6, "", "0xCaC524BcA292aaade2DF8A05cC58F0a65B1B3bB9") else null,
                //            TokenData("FAUCET", "FAU", 18, "0x55296f69f40Ea6d20E478533C15A6B08B654E759", "0xBA62BCfcAaFc6622853cca2BE6Ac7d845BC0f2Dc"),
                //            TokenData("WEENUS", "WEENUS", 18, "0x2823589Ae095D99bD64dEeA80B4690313e2fB519", "0xaFF4481D10270F50f203E0763e2597776068CBc5"),
                //            TokenData("XEENUS", "XEENUS", 18, "0xeEf5E2d8255E973d587217f9509B416b41CA5870", "0x022E292b44B5a146F2e8ee36Ff44D3dd863C915c"),
                //            TokenData("YEENUS", "YEENUS", 8, "0x187E63F9eBA692A0ac98d3edE6fEb870AF0079e1", "0xc6fDe3FD2Cc2b173aEC24cc3f267cb3Cd78a26B7"),
            TokenData("0x", "ZRX", 18, "0xe41d2489571d322189246dafa5ebde1f4699f498"),
            TokenData(
                "USDT", "USDT20",
                6,
                "0xdac17f958d2ee523a2206206994597c13d831ec7",
                "0xaA8E23Fb1079EA71e0a56F48a2aA51851D8433D0",
                legacyNames = listOf("Tether USD(ERC20)")
            ),
            TokenData(
                "USD Coin",
                "USDC",
                6,
                "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238"
            ),
            TokenData("HuobiToken", "HT", 18, "0x6f259637dcd74c767781e37bc6133cd6a68aa161"),
            TokenData(
                "Binance USD",
                "BUSD",
                18,
                "0x4fabb145d64652a948d72533023f6e7a623c7c53"),
            TokenData("Bitfinex LEO", "LEO", 18, "0x2af5d2ad76741191d15dfe7bf6ac92d4bd912ca3"),
            TokenData(
                "TrueUSD",
                "TUSD",
                18,
                "0x0000000000085d4780B73119b644AE5ecd22b376"),
            TokenData(
                "ChainLink",
                "LINK",
                18,
                "0x514910771AF9Ca656af840dff83E8264EcF986CA",
                "0x779877A7B0D9E8603169DdbD7836e478b4624789"
            ),
            TokenData("Pax Dollar", "USDP", 18, "0x8E870D67F660D95d5be530380D0eC0bd388289E1"),
            TokenData("ZBToken", "ZB", 18, "0xBd0793332e9fB844A52a205A233EF27a5b34B927"),
            TokenData("OKB", "OKB", 18, "0x75231F58b43240C9718Dd58B4967c5114342a86c"),
            TokenData("OmiseGO", "OMG", 18, "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07"),
            TokenData(
                "Basic Attention Token",
                "BAT",
                18,
                "0x0D8775F648430679A709E98d2b0Cb6250d2887EF"),
            TokenData("BIX Token", "BIX", 18, "0xb3104b4B9Da82025E8b9F8Fb28b3553ce2f67069"),
            TokenData(
                "Storj",
                "STORJ",
                8,
                "0xB64ef51C888972c908CFacf59B47C1AfBC0Ab8aC"),
            TokenData(
                "Gemini dollar",
                "GUSD",
                2,
                "0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd"),
            TokenData(
                "Kyber Network Crystal v2",
                "KNC",
                18,
                "0xdeFA4e8a7bcBA345F687a2f1456F5Edd9CE97202"
            ),
            TokenData("StatusNetwork", "SNT", 18, "0x744d70FDBE2Ba4CF95131626614a1763DF805B9E"),
            TokenData("Lambda", "LAMB", 18, "0x8971f9fd7196e5cEE2C1032B50F656855af7Dd26"),
            TokenData("chiliZ", "CHZ", 18, "0x3506424F91fD33084466F402d5D97f05F8e3b4AF"),
            TokenData(
                "Augur (REPv2)",
                "REP",
                18,
                "0x221657776846890989a759ba2973e427dff5c9bb"),
            TokenData("IOSToken", "IOST", 18, "0xFA1a856Cfa3409CFa145Fa4e20Eb270dF3EB21ab"),
            TokenData("ICON", "ICX", 18, "0xb5A5F22694352C15B00323844aD545ABb2B11028"),
            TokenData("HUSD", "HUSD", 8, "0xdF574c24545E5FfEcb9a659c229253D4111d87e1"),
            TokenData("Streamr", "DATA", 18, "0x8f693ca8D21b157107184d29D398A8D082b38b76"),
            TokenData("Loom Token", "LOOM", 18, "0x42476F744292107e34519F9c357927074Ea3F75D"),
            TokenData(
                "Decentraland",
                "MANA",
                18,
                "0x0F5D2fB29fb7d3CFeE444a200298f468908cC942"),
            TokenData("aelf", "ELF", 18, "0xbf2179859fc6D5BEE9Bf9158632Dc51678a4100e"),
            TokenData(
                "Dai Stablecoin",
                "DAI",
                18,
                "0x6B175474E89094C44Da98b954EedeAC495271d0F",
                "0xFF34B3d4Aee8ddCd6F9AFFFB6Fe49bD371b8a357"
            ),
            TokenData("Uquid Coin", "UQC", 18, "0xD01DB73E047855Efb414e6202098C4Be4Cd2423B"),
            TokenData("Holo", "HOT", 18, "0x6c6EE5e31d828De241282B9606C8e98Ea48526E2"),
            TokenData("Origin Protocol", "OGN", 18, "0x8207c1FfC5B6804F6024322CcF34F29c3541Ae26"),
            TokenData("Tellor", "TRB", 18, "0x88dF592F8eb5D7Bd38bFeF7dEb0fBc02cf3778a0"),
            TokenData("Chromia", "CHR", 6, "0x8A2279d4A90B6fe1C4B30fa660cC9f926797bAA2"),
            TokenData(
                "EnjinCoin",
                "ENJ",
                18,
                "0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c"),
            TokenData("HedgeTrade", "HEDG", 18, "0xF1290473E210b2108A85237fbCd7b6eb42Cc654F"),
            TokenData("Maker", "MKR", 18, "0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2"),
            TokenData("Hyperion", "HYN", 18, "0xE99A894a69d7c2e3C92E61B64C505A6a57d2bC07"),
            TokenData("Synthetix", "SNX", 18, "0xc011a73ee8576fb46f5e1c5751ca3b9fe0af2a6f"),
            TokenData("Kucoin Shares", "KCS", 6, "0x039B5649A59967e3e936D7471f9c3700100Ee1ab"),
            TokenData("Numeraire", "NMR", 18, "0x1776e1F26f98b1A5dF9cD347953a26dd3Cb46671"),
            TokenData("Crypterium", "CRPT", 18, "0x08389495D7456E1951ddF7c3a1314A4bfb646d8B"),
            TokenData("DigixDAO", "DGD", 9, "0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A"),
            TokenData(
                "Quant",
                "QNT",
                18,
                "0x4a220E6096B25EADb88358cb44068A3248254675"),
            TokenData("Metal", "MTL", 8, "0xF433089366899D83a9f26A773D59ec7eCF30355e"),
            TokenData("Nexo", "NEXO", 18, "0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206"),
            TokenData("Theta Token", "THETA", 18, "0x3883f5e181fccaF8410FA61e12b59BAd963fb645"),
            TokenData("Bytom", "BTM", 8, "0xcB97e65F07DA24D46BcDD078EBebd7C6E6E3d750"),
            TokenData("Aragon V2", "ANT", 18, "0xa117000000f279d81a1d3cc75430faa017fa5a2e"),
            TokenData("Swipe", "SXP", 18, "0x8CE9137d39326AD0cD6491fb5CC0CbA0e089b6A9"),
            TokenData("Gnosis", "GNO", 18, "0x6810e776880C02933D47DB1b9fc05908e5386b96"),
            TokenData("SwissBorg", "CHSB", 8, "0xba9d4199faB4f26eFE3551D490E3821486f135Ba"),
            TokenData("Function X", "FX", 18, "0x8c15Ef5b4B21951d50E53E4fbdA8298FFAD25057"),
            TokenData("Mixin", "XIN", 18, "0xA974c709cFb4566686553a20790685A47acEAA33"),
            TokenData("Ren", "REN", 18, "0x408e41876cCCDC0F92210600ef50372656052a38"),
            TokenData(
                "Centrality Token",
                "CENNZ",
                18,
                "0x1122B6a0E00DCe0563082b6e2953f3A943855c1F"
            ),
            TokenData("Abyss", "ABYSS", 18, "0x0E8d6b471e332F140e7d9dbB99E5E3822F728DA6"),
            TokenData("Aeron", "ARN", 8, "0xBA5F11b16B155792Cf3B2E6880E8706859A8AEB6"),
            TokenData("ATLANT", "ATL", 18, "0x78B7FADA55A64dD895D8c8c35779DD8b67fA8a05"),
            TokenData("Atlas Token", "ATLS", 18, "0xd36E9F8F194A47B10aF16C7656a68EBa1DFe88e4"),
            TokenData("Coinlancer", "CL", 18, "0xe81D72D14B1516e68ac3190a46C93302Cc8eD60f"),
            TokenData("DENT", "DENT", 8, "0x3597bfD533a99c9aa083587B074434E61Eb0A258"),
            TokenData("Dentacoin", "DCN", 0, "0x08d32b0da63e2C3bcF8019c9c5d849d7a9d791e6"),
            TokenData("Polymath", "POLY", 18, "0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC"),
            TokenData(
                "RipioCreditNetwork",
                "RCN",
                18,
                "0xF970b8E36e23F7fC3FD752EeA86f8Be8D83375A6"
            ),
            TokenData("Rocket Pool", "RPL", 18, "0xD33526068D116cE69F19A9ee46F0bd304F21A51f"),
            TokenData("LAtoken", "LA", 18, "0xE50365f5D679CB98a1dd62D6F6e58e59321BcdDf"),
            TokenData(
                "Bread",
                "BRD",
                18,
                "0x558EC3152e2eb2174905cd19AeA4e34A23DE9aD6"),
            TokenData("Tether GOLD", "XAUT", 6, "0x68749665FF8D2d112Fa859AA293F07A622782F38"),
            TokenData(
                "Mycelium Token",
                "MT",
                7,
                "0x364f56e35e75227516878cc249f11ea9b3e41b09"),
            TokenData("SHIBA INU", "SHIB", 18, "0x95aD61b0a150d79219dCF64E1E6Cc01f0B64C4cE"),
            TokenData("Paxos Gold", "PAXG", 18, "0x45804880De22913dAFE09f4980848ECE6EcbAf78"),
            TokenData("Wrapped Bitcoin", "WBTC", 8, "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599",
                "0x29f2D40B0605204364af54EC677bD022dA425d03"),
            TokenData("Loopring", "LRC", 18, "0xBBbbCA6A901c926F240b89EacB641d8Aec7AEafD"),
            TokenData("Curve DAO Token", "CRV", 18, "0xD533a949740bb3306d119CC777fa900bA034cd52"),
            TokenData("HEX", "HEX", 8, "0x2b591e99afE9f32eAA6214f7B7629768c40Eeb39"),
            TokenData("The Sandbox", "SAND", 18, "0x3845badAde8e6dFF049820680d1F14bD3903a5d0"),
            TokenData("FTX Token", "FTT", 18, "0x50D1c9771902476076eCFc8B2A83Ad6b9355a4c9"),
            TokenData("Aave Token", "AAVE", 18, "0x7Fc66500c84A76Ad7e9c93437bFc5Ac33E2DDaE9"),
            TokenData("Uniswap", "UNI", 18, "0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984"),
            TokenData(
                "Axie Infinity",
                "AXS",
                18,
                "0xBB0E17EF65F82Ab018d8EDd776e8DD940327B28b"
            ),
            TokenData("RLC", "RLC", 9, "0x607F4C5BB672230e8672085532f7e901544a7375"),
            TokenData("yearn.finance", "YFI", 18, "0x0bc529c00C6401aEF6D220BE8C6Ea1667F6Ad93e"),
            TokenData("The Graph", "GRT", 18, "0xc944E90C64B2c07662A292be6244BDf05Cda44a7"),
            TokenData("Civic", "CVC", 8, "0x41e5560054824eA6B0732E656E3Ad64E20e94E45"),
            TokenData("Gala", "GALA", 8, "0xd1d2eb1b1e90b638588728b4130137d262c87cae"),
            TokenData("Illuvium", "ILV", 18, "0x767FE9EDC9E0dF98E07454847909b5E959D7ca0E"),
            TokenData("SushiToken", "SUSHI", 18, "0x6B3595068778DD592e39A122f4f5a5cF09C90fE2"),
            TokenData("1INCH Token", "1INCH", 18, "0x111111111117dC0aa78b770fA6A738034120C302"),
            TokenData("PowerLedger", "POWR", 6, "0x595832F8FC6BF59c85C527fEC3740A1b7a361269"),
            TokenData("Bancor", "BNT", 18, "0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C"),
            TokenData("Compound", "COMP", 18, "0xc00e94Cb662C3520282E6f5717214004A7f26888"),
            TokenData("dYdX", "DYDX", 18, "0x92D6C1e31e14520e676a687F0a93788B716BEff5"),
            TokenData("Ankr", "ANKR", 18, "0x8290333ceF9e6D528dD5618Fb97a76f268f3EDD4"),
            TokenData("XYO", "XYO", 18, "0x55296f69f40Ea6d20E478533C15A6B08B654E758"),
            TokenData("Request", "REQ", 18, "0x8f8221afbb33998d8584a2b05749ba73c37a938a"),
            TokenData("UMA", "UMA", 18, "0x04Fa0d235C4abf4BcF4787aF4CF447DE572eF828"),
            TokenData("Viberate", "vib", 18, "0x2C974B2d0BA1716E644c1FC59982a89DDD2fF724"),
            TokenData("PancakeSwap Token", "CAKE", 18, "0x152649eA73beAb28c5b49B26eb48f7EAD6d4c898"),
            TokenData("district0x", "dnt", 18, "0x0abdace70d3790235af448c88547603b945604ea")

        )
    }
}

data class TokenData(val name: String, val symbol: String, val unitExponent: Int, val prodAddress: String,
                    val testnetAddress: String? = null,
                    /** Names this token shipped under before; accounts still carrying one get renamed. */
                    val legacyNames: List<String> = emptyList())
