package com.mycelium.wallet.persistence

import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContext
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class AccountContextDAO : com.mycelium.wapi.wallet.genericdb.AccountContextDAO {
    @Query("SELECT * FROM account_context WHERE currency = :currency")
    abstract override fun getContextsForCurrency(currency: CryptoCurrency): Flowable<AccountContext>

    @Insert
    @WorkerThread
    abstract override suspend fun insert(accountContext: AccountContext): Completable

    @Delete
    abstract override suspend fun deleteAll(): Single<Int>
}