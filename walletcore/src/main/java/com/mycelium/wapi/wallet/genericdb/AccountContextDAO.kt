package com.mycelium.wapi.wallet.genericdb

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import io.reactivex.Flowable

interface AccountContextDAO {
    fun getContextsForCurrency(currency: CryptoCurrency): Flowable<out AccountContext>

    suspend fun insert(accountContext: AccountContext)

    suspend fun deleteAll()
}