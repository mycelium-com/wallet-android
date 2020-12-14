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


interface ColuApi {
    @Throws(IOException::class)
    fun broadcastTx(coluSignedTransaction: BitcoinTransaction): String?

    @Throws(IOException::class)
    fun getAddressTransactions(address: Address): AddressTransactionsInfo.Json

    @Throws(IOException::class)
    fun getCoinTypes(address: BitcoinAddress): List<ColuMain>

    @Throws(IOException::class)
    fun prepareTransaction(toAddress: BtcAddress, fromAddress: List<BtcAddress>, amount: Value, txFee: Value): ColuBroadcastTxHex.Json?

}