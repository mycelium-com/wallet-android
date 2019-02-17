package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.colu.coins.MTCoin
import java.io.IOException


class ColuApiImpl(val coluClient: ColuClient) : ColuApi {
    override fun prepareTransaction(toAddress: BtcAddress, fromBtcAddress: List<BtcAddress>, amount: Value, txFee: Value): String? {
        val fromAddress = mutableListOf<Address>()
        fromBtcAddress.forEach {
            fromAddress.add(it.address)
        }
        val result = coluClient.prepareTransaction(toAddress.address, fromAddress, amount, txFee.getValue())
        return result?.txHex
    }

    @Throws(IOException::class)
    override fun broadcastTx(coluSignedTransaction: Transaction): String? {
        val result = coluClient.broadcastTransaction(coluSignedTransaction)
        return result.txid
    }

    override fun getAddressTransactions(address: GenericAddress): List<ColuTransaction>? {
        var result: MutableList<ColuTransaction>? = null
        try {
            val json = coluClient.getAddressTransactions(address.toString())
            result = mutableListOf()
            for (transaction in json.transactions) {
                var transferred = Value.zeroValue(address.coinType)

                val input = mutableListOf<GenericTransaction.GenericInput>()
                transaction.vin.forEach { vin ->
                    vin.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                        val value = Value.valueOf(address.coinType, asset.amount)
                        val _address = Address.fromString(vin.previousOutput.addresses[0])
                        input.add(GenericTransaction.GenericInput(
                                BtcAddress(address.coinType, _address), value))
                        if (vin.previousOutput.addresses.contains(address.toString())) {
                            transferred = transferred.subtract(value)
                        }
                    }
                }

                val output = mutableListOf<GenericTransaction.GenericOutput>()
                transaction.vout.forEach { vout ->
                    vout.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                        val value = Value.valueOf(address.coinType, asset.amount)
                        val _address = Address.fromString(vout.scriptPubKey.addresses[0])
                        output.add(GenericTransaction.GenericOutput(
                                BtcAddress(address.coinType, _address), value))
                        if (vout.scriptPubKey.addresses.contains(address.toString())) {
                            transferred = transferred.add(value)
                        }
                    }
                }

                if (input.size > 0 && output.size > 0) {
                    result.add(ColuTransaction(Sha256Hash.fromString(transaction.txid), MTCoin
                            , transferred
                            , transaction.time / 1000, null, transaction.blockheight.toInt()
                            , transaction.confirmations, false, input, output))
                }
            }
        } catch (e: IOException) {
            //Log.e("ColuApiImpl", "", e)
        }
        return result
    }

    override fun getCoinTypes(address: Address): List<ColuMain> {
        val assetsList = mutableListOf<ColuMain>()
        try {
            val addressInfo = coluClient.getBalance(address)
            if (addressInfo != null) {
                if (addressInfo.utxos != null) {
                    for (utxo in addressInfo.utxos) {
                        // adding utxo to list of txid list request
                        for (txidAsset in utxo.assets) {
                            for (coin in ColuUtils.allColuCoins()) {
                                if (txidAsset.assetId == coin.id) {
                                    assetsList.add(coin)
                                }
                            }
                        }
                    }
                }
            }
        } catch (ignore: IOException) {
        }
        return assetsList
    }
}
