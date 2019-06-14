package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
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

    override fun getAddressTransactions(address: GenericAddress): ColuApi.ColuTransactionsInfo? {
        var result: ColuApi.ColuTransactionsInfo? = null
        try {
            val json = coluClient.getAddressTransactions(address.toString())
            val transactions = mutableListOf<GenericTransactionSummary>()
            for (transaction in json.transactions) {
                var transferred = Value.zeroValue(address.coinType)

                val input = mutableListOf<GenericInputViewModel>()
                transaction.vin.forEach { vin ->
                    vin.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                        val value = Value.valueOf(address.coinType, asset.amount)
                        val _address = Address.fromString(vin.previousOutput.addresses[0])
                        input.add(GenericInputViewModel(
                                BtcAddress(address.coinType, _address), value, false))
                        if (vin.previousOutput.addresses.contains(address.toString())) {
                            transferred = transferred.subtract(value)
                        }
                    }
                }

                val output = mutableListOf<GenericOutputViewModel>()
                transaction.vout.forEach { vout ->
                    vout.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                        val value = Value.valueOf(address.coinType, asset.amount)
                        val _address = Address.fromString(vout.scriptPubKey.addresses[0])
                        output.add(GenericOutputViewModel(
                                BtcAddress(address.coinType, _address), value, false))
                        if (vout.scriptPubKey.addresses.contains(address.toString())) {
                            transferred = transferred.add(value)
                        }
                    }
                }

                if (input.size > 0 || output.size > 0) {
                    transactions.add(GenericTransactionSummary(
                            address.coinType,
                            Sha256Hash.fromString(transaction.txid).bytes,
                            Sha256Hash.fromString(transaction.hash).bytes,
                            transferred,
                            transaction.time / 1000,
                            transaction.blockheight.toInt(),
                            transaction.confirmations,
                            false,
                            output[0].address,
                            input,
                            output,
                            ConfirmationRiskProfileLocal(0, false, false),
                            0,
                            Value.valueOf(address.coinType, 0)
                    ))
                }
            }
            val utxos = mutableListOf<TransactionOutputEx>()
            for (utxo in json.utxos) {
                utxo.assets.filter { it.assetId == address.coinType.id }.forEach { asset ->
                    utxos.add(TransactionOutputEx(OutPoint(Sha256Hash.fromString(utxo.txid), utxo.index), utxo.blockheight,
                            asset.amount, utxo.scriptPubKey.asm.toByteArray(), false))
                }
            }
            result = ColuApi.ColuTransactionsInfo(transactions, utxos, Value.valueOf(address.coinType, json.balance))
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
