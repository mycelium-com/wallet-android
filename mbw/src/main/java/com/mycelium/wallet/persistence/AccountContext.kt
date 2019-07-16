package com.mycelium.wallet.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

/**
 * This interface provides generic interface to interact with accounts backings
 */

@Entity(tableName = "account_context")
data class AccountContext(@PrimaryKey override val uuid: UUID,
                          override val currency: CryptoCurrency,
                          override var accountName: String,
                          override var balance: Balance,
                          override var archived: Boolean)
    : com.mycelium.wapi.wallet.genericdb.AccountContext