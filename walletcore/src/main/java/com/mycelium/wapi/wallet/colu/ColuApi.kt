package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import java.io.IOException


interface ColuApi {
    @Throws(IOException::class)
    fun broadcastTx(coluSignedTransaction: Transaction): String?

    fun getAddressTransactions(address: GenericAddress): ColuTransactionsInfo?

    fun getCoinTypes(address: Address): List<ColuMain>

    fun prepareTransaction(toAddress: BtcAddress, fromAddress: List<BtcAddress>, amount: Value, txFee: Value): String?

    data class ColuTransactionsInfo(val transactions: List<ColuTransaction>,
                                    val unspent: List<TransactionOutputEx>,
                                    val balance: Value)
}