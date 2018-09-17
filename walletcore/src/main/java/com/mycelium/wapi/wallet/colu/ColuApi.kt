package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Transaction


interface ColuApi {
    fun broadcastTransaction(coluSignedTransaction: Transaction): String?
}