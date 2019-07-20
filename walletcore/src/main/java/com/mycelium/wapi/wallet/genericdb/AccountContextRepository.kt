package com.mycelium.wapi.wallet.genericdb

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import io.reactivex.Flowable


class AccountContextRepository(private val accountContextDAO: AccountContextDAO) {
    fun getContextsForCurrency(currency: CryptoCurrency): Flowable<out AccountContext> =
            accountContextDAO.getContextsForCurrency(currency)

    suspend fun insert(accountContext: AccountContext) = accountContextDAO.insert(accountContext)
}