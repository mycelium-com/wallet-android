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


interface ColuApi {
    @Throws(IOException::class)
    fun broadcastTx(coluSignedTransaction: Transaction): String?

    @Throws(IOException::class)
    fun getAddressTransactions(address: GenericAddress): AddressTransactionsInfo.Json

    @Throws(IOException::class)
    fun getCoinTypes(address: Address): List<ColuMain>

    @Throws(IOException::class)
    fun prepareTransaction(toAddress: BtcAddress, fromAddress: List<BtcAddress>, amount: Value, txFee: Value): ColuBroadcastTxHex.Json?

}