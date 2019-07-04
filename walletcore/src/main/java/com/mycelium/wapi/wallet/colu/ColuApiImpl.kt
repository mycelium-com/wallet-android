package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.colu.json.AddressTransactionsInfo
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex
import java.io.IOException


class ColuApiImpl(val coluClient: ColuClient) : ColuApi {

    override fun prepareTransaction(toAddress: BtcAddress, fromBtcAddress: List<BtcAddress>, amount: Value, txFee: Value): ColuBroadcastTxHex.Json? {
        val fromAddress = mutableListOf<Address>()
        fromBtcAddress.forEach {
            fromAddress.add(it.address)
        }
        return coluClient.prepareTransaction(toAddress.address, fromAddress, amount, txFee.getValue())

    }

    @Throws(IOException::class)
    override fun broadcastTx(coluSignedTransaction: Transaction): String? {
        val result = coluClient.broadcastTransaction(coluSignedTransaction)
        return result.txid
    }

    override fun getAddressTransactions(address: GenericAddress): AddressTransactionsInfo.Json {
        return coluClient.getAddressTransactions(address.toString())
    }

    override fun getCoinTypes(address: Address): List<ColuMain> {
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
