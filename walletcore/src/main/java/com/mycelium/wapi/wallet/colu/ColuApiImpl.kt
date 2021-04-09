package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BitcoinTransaction
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.colu.json.AddressTransactionsInfo
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex
import java.io.IOException


class ColuApiImpl(val coluClient: ColuClient) : ColuApi {

    @Throws(IOException::class)
    override fun prepareTransaction(toAddress: BtcAddress, fromBtcAddress: List<BtcAddress>, amount: Value, txFee: Value): ColuBroadcastTxHex.Json? {
        val fromAddress = mutableListOf<BitcoinAddress>()
        fromBtcAddress.forEach {
            fromAddress.add(it.address)
        }
        return coluClient.prepareTransaction(toAddress.address, fromAddress, amount, txFee.valueAsLong)

    }

    @Throws(IOException::class)
    override fun broadcastTx(coluSignedTransaction: BitcoinTransaction): String? {
        val result = coluClient.broadcastTransaction(coluSignedTransaction)
        return result.txid
    }

    @Throws(IOException::class)
    override fun getAddressTransactions(address: Address): AddressTransactionsInfo.Json {
        return coluClient.getAddressTransactions(address.toString())
    }

    @Throws(IOException::class)
    override fun getCoinTypes(address: BitcoinAddress): List<ColuMain> {
        val assetsList = mutableListOf<ColuMain>()
        try {
            val addressInfo = coluClient.getBalance(address)
            if (addressInfo != null) {
                // adding utxo to list of txid list request
                for (txidAsset in addressInfo.assets) {
                    for (coin in ColuUtils.allColuCoins(address.network.toString())) {
                        if (txidAsset.assetId == coin.id) {
                            assetsList.add(coin)
                        }
                    }
                }
            }
        } catch (ignore: IOException) {
        }
        return assetsList
    }
}
