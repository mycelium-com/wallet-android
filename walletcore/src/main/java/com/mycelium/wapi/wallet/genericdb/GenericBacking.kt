package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import java.util.*

interface GenericBacking {
    fun loadAccountContexts(): List<AccountContext>

    fun loadAccountContext(accountId: UUID): AccountContext?

    fun createAccountContext(context: AccountContext)

    fun updateAccountContext(context: AccountContext)

    fun deleteAccountContext(uuid: UUID)
}