package com.mycelium.wallet.persistence

import androidx.annotation.WorkerThread
import androidx.room.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import io.reactivex.Flowable

@Dao
abstract class AccountContextDAO : com.mycelium.wapi.wallet.genericdb.AccountContextDAO {
    @Query("SELECT * FROM account_context WHERE currency = :currency")
    abstract override fun getContextsForCurrency(currency: CryptoCurrency): Flowable<AccountContext>

    @Ignore
    override suspend fun insert(accountContext: com.mycelium.wapi.wallet.genericdb.AccountContext) =
            insert(accountContext as AccountContext)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @WorkerThread
    abstract suspend fun insert(accountContext: AccountContext)

    @Query("DELETE FROM account_context")
    abstract override suspend fun deleteAll()
}