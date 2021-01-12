package com.mycelium.wapi.wallet.btcvault.hd

import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*


class BitcoinVaultHDAccountBacking(walletDB: WalletDB,
                                   private val uuid: UUID,
                                   private val currency: CryptoCurrency) {

    fun getTransactionSummaries(offset: Long, limit: Long): List<TransactionSummary> {
        return emptyList()
    }

    fun getTransactionSummary(txidParameter: String): TransactionSummary? {
        return null
    }
}