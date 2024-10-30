package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import java.util.*

class InMemoryAccountContextsBacking<T> : Backing<T> {
    private val accountContexts = hashMapOf<UUID, T>()
    override fun loadAccountContexts(): List<T> = accountContexts.values.toList()

    override fun loadAccountContext(accountId: UUID) = accountContexts[accountId]

    override fun createAccountContext(accountId: UUID, context: T) {
        accountContexts[accountId] = context
    }

    override fun updateAccountContext(accountId: UUID, context: T) =
        createAccountContext(accountId, context)

    override fun deleteAccountContext(uuid: UUID) {
        accountContexts.remove(uuid)
    }
}