package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.colu.coins.MTCoin
import java.io.IOException


class ColuApiImpl(val coluClient: ColuClient) : ColuApi {

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
                var sent = Value.zeroValue(address.coinType)
                var receive = Value.zeroValue(address.coinType)

                val input = mutableListOf<GenericTransaction.GenericInput>()
                transaction.vin.forEach { vin ->
                    vin.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                        val value = Value.valueOf(address.coinType, asset.amount)
                        val _address = Address.fromString(vin.previousOutput.addresses[0])
                        input.add(GenericTransaction.GenericInput(
                                BtcLegacyAddress(address.coinType, _address.allAddressBytes), value))
                        if (vin.previousOutput.addresses.contains(address.toString())) {
                            sent = sent.add(value)
                        }
                    }
                }

                val output = mutableListOf<GenericTransaction.GenericOutput>()
                transaction.vout.forEach { vout ->
                    vout.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                        val value = Value.valueOf(address.coinType, asset.amount)
                        val _address = Address.fromString(vout.scriptPubKey.addresses[0])
                        output.add(GenericTransaction.GenericOutput(
                                BtcLegacyAddress(address.coinType, _address.allAddressBytes), value))
                        if (vout.scriptPubKey.addresses.contains(address.toString())) {
                            receive = receive.add(value)
                        }
                    }
                }

                if (input.size > 0 && output.size > 0) {
                    result.add(ColuTransaction(Sha256Hash.fromString(transaction.txid), MTCoin
                            , if (sent.isGreaterThan(receive)) sent.subtract(receive) else Value.zeroValue(address.coinType)
                            , if (receive.isGreaterThan(sent)) receive.subtract(sent) else Value.zeroValue(address.coinType)
                            , transaction.time / 1000, null
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
        val addressInfo = coluClient.getBalance(address)
        if (addressInfo != null) {
            if (addressInfo.utxos != null) {
                for (utxo in addressInfo.utxos) {
                    // adding utxo to list of txid list request
                    for (txidAsset in utxo.assets) {
                        for (knownAssetId in ColuAccount.ColuAsset.getAssetMap().keys) {
                            val coluMain = ColuUtils.getColuCoin(knownAssetId)
                            coluMain.let {
                                assetsList.add(coluMain)
                            }
                        }
                    }
                }
            }
        }
        return assetsList
    }
}
