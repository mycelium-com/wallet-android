package com.mycelium.wapi.wallet.genericdb

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface AccountContextDAO {
    fun getContextsForCurrency(currency: CryptoCurrency): Flowable<AccountContext>

    suspend fun insert(accountContext: AccountContext): Completable

    suspend fun deleteAll(): Single<Int>
}