package com.mycelium.wallet

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.HttpsEndpoint
import com.mycelium.net.ServerEndpoints
import com.mycelium.net.TorHttpsEndpoint
import com.mycelium.wallet.activity.util.BlockExplorer
import com.mycelium.wallet.external.BankCardServiceDescription
import com.mycelium.wallet.external.BuySellServiceDescriptor
import com.mycelium.wallet.external.LocalTraderServiceDescription
import com.mycelium.wallet.external.SepaServiceDescription
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.btcvault.BTCVNetworkParameters
import com.mycelium.wapi.wallet.btcvault.BTCVNetworkParameters.Companion.testNetwork
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.fio.coins.FIOTest

class MbwTestnet4Environment : MbwEnvironment() {
    override fun getNetwork(): NetworkParameters? = NetworkParameters.testNetwork

    override fun getBTCVNetwork(): BTCVNetworkParameters = testNetwork

    override fun getLtEndpoints(): ServerEndpoints = testnetLtEndpoints

    override fun getWapiEndpoints(): ServerEndpoints = testnetWapiEndpoints

    override fun getBlockExplorerMap(): Map<String, List<BlockExplorer>> =
        testnetExplorerClearEndpoints

    override fun getBuySellServices(): List<BuySellServiceDescriptor> =
        listOf(
            BankCardServiceDescription(),
            SepaServiceDescription(),
            LocalTraderServiceDescription()
        )

    companion object {
        /**
         * Local Trader API for testnet
         */
        private val testnetLtEndpoints = ServerEndpoints(
            arrayOf<HttpEndpoint>(
                HttpsEndpoint(
                    "https://mws30.mycelium.com/lttestnet",
                    "ED:C2:82:16:65:8C:4E:E1:C7:F6:A2:2B:15:EC:30:F9:CD:48:F8:DB"
                ),
                TorHttpsEndpoint(
                    "https://grrhi6bwwpiarsfl.onion/lttestnet",
                    "D0:09:70:40:98:71:E0:0E:62:08:1A:36:4C:BC:C7:2E:51:40:50:4C"
                ),
            )
        )

        /**
         * Wapi
         */
        private val testnetWapiEndpoints = ServerEndpoints(
            arrayOf<HttpEndpoint>(
                HttpsEndpoint(
                    "https://mws30.mycelium.com/wapitestnet",
                    "ED:C2:82:16:65:8C:4E:E1:C7:F6:A2:2B:15:EC:30:F9:CD:48:F8:DB"
                ),
                TorHttpsEndpoint(
                    "https://ti4v3ipng2pqutby.onion/wapitestnet",
                    "75:3E:8A:87:FA:95:9F:C6:1A:DB:2A:09:43:CE:52:74:27:B1:80:4B"
                ),
            )
        )

        /**
         * Available BlockExplorers
         *
         *
         * The first is the default block explorer if the requested one is not available
         */
        private val testnetExplorerClearEndpoints: Map<String, List<BlockExplorer>> =
            mapOf(
                BitcoinTest.name to listOf(
                    BlockExplorer(
                        "MPS", "mempool.space",
                        "https://mempool.space/testnet4/address/",
                        "https://mempool.space/testnet4/tx/",
                        null,
                        null
                    )
                ),
                EthTest.name to listOf(
                    BlockExplorer(
                        "ETS", "etherscan.io",
                        "https://goerli.etherscan.io/address/",
                        "https://goerli.etherscan.io/tx/0x",
                        null,
                        null
                    )
                ),
                FIOTest.name to listOf(
                    BlockExplorer(
                        "FBI", "fio.bloks.io",
                        "https://fio-test.bloks.io/account/",
                        "https://fio-test.bloks.io/transaction/",
                        null,
                        null
                    ),
                    BlockExplorer(
                        "EFI", "explorer.fioprotocol.io",
                        "https://explorer.testnet.fioprotocol.io/account/",
                        "https://explorer.testnet.fioprotocol.io/transaction/",
                        null,
                        null
                    )
                )
            )
    }
}