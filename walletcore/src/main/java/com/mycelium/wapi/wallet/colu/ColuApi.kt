package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import java.io.IOException


interface ColuApi {
    @Throws(IOException::class)
    fun broadcastTx(coluSignedTransaction: Transaction): String?

    fun getAddressTransactions(address: GenericAddress): List<ColuTransaction>?

    fun getCoinTypes(address: Address):List<ColuMain>
}