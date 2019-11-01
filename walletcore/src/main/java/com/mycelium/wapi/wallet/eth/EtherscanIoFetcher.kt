package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.net.URL

class EtherscanIoFetcher {

    companion object {
        @JvmStatic
        fun fetchTransactions(receivingAddress: String, backing: EthAccountBacking, coinType: CryptoCurrency) {
            GlobalScope.launch {
                val apiKey = "KWQPBBFJQYAT5P447MM8322R5BVY8C2MG2"
                val subDomain = if (coinType == EthMain) PRODNET_SUBDOMAIN else TESTNET_SUBDOMAIN
                val urlString = "http://${subDomain}.etherscan.io/api?module=account&action=txlist&address=${receivingAddress}&startblock=0&endblock=99999999&sort=asc&apikey=${apiKey}"
                val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                withContext(Dispatchers.IO) {
                    val txlist = mapper.readValue(URL(urlString), EtherscanApiTxlist::class.java)
                    txlist.result.forEach { tx ->
                        backing.putTransaction(tx.blockNumber, tx.timeStamp, tx.hash.substring(2),
                                "", tx.from, tx.to, Value.valueOf(coinType, tx.value),
                                Value.valueOf(coinType, tx.gasPrice), tx.confirmations)
                    }
                }
            }
        }

        private const val PRODNET_SUBDOMAIN = "api"
        private const val TESTNET_SUBDOMAIN = "api-ropsten"
    }

    class EtherscanApiTxlist {
        var result: List<EtherscanApiTx> = emptyList()
    }

    class EtherscanApiTx {
        var blockNumber: Int = 0
        var timeStamp: Long = 0
        var hash: String = ""
        var from: String = ""
        var to: String = ""
        var value: BigInteger = BigInteger.ZERO
        var gasPrice: BigInteger = BigInteger.ZERO
        var confirmations: Int = 0
        var nonce: BigInteger = BigInteger.ZERO
    }
}