package com.mycelium.wapi.wallet.eth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.AccountListener
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
import java.util.concurrent.TimeUnit

object EtherscanIoFetcher {

    @JvmStatic
    fun syncWithRemote(coinType: CryptoCurrency, receivingAddress: String, backing: EthAccountBacking, callback: () -> Unit) {
        GlobalScope.launch {
            val apiKey = "KWQPBBFJQYAT5P447MM8322R5BVY8C2MG2"
            val subDomain = if (coinType == EthMain) PRODNET_SUBDOMAIN else TESTNET_SUBDOMAIN
            val urlString = "http://${subDomain}.etherscan.io/api?module=account&action=txlist&address=${receivingAddress}&startblock=0&endblock=99999999&sort=asc&apikey=${apiKey}"
            val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            withContext(Dispatchers.IO) {
                val remoteTxs = mapper.readValue(URL(urlString), EtherscanApiTxlist::class.java).result
                val localTxs = backing.getTransactionSummaries(0, Long.MAX_VALUE, receivingAddress)
                val remoteIds = remoteTxs.map { it.hash }
                val localIds = localTxs.map { "0x" + HexUtils.toHex(it.id) }
                val toAdd = remoteTxs.filter { !localIds.contains(it.hash) }
                val toUpdate = remoteTxs.filter { it.hash in remoteIds.intersect(localIds) }
                val toDelete = localIds.filter { !remoteIds.contains(it) }

                toAdd.forEach { tx ->
                    backing.putTransaction(tx.blockNumber, tx.timeStamp, tx.hash,
                            "", tx.from, tx.to, Value.valueOf(coinType, tx.value),
                            Value.valueOf(coinType, tx.gasPrice), tx.confirmations)
                }
                toUpdate.forEach { tx ->
                    backing.updateTransaction(tx.hash, tx.blockNumber, tx.confirmations)
                }
                toDelete.forEach { id ->
                    backing.deleteTransaction(id)
                }
                callback()
            }
        }
    }

    private const val PRODNET_SUBDOMAIN = "api"
    private const val TESTNET_SUBDOMAIN = "api-ropsten"
    private val TIME_WAIT_FOR_CONFIRMATION = TimeUnit.DAYS.toSeconds(14)
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