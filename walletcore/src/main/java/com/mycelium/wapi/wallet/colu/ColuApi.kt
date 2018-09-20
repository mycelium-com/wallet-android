package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Transaction
import java.io.IOException


interface ColuApi {
    @Throws(IOException::class)
    fun broadcastTx(coluSignedTransaction: Transaction): String?
}