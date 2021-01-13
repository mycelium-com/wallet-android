package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import java.util.*

class InMemoryAccountContextsBacking<T: AccountContext> : Backing<T> {
    private val accountContexts = hashMapOf<UUID, T>()
    override fun loadAccountContexts(): List<T> = accountContexts.values.toList()

    override fun loadAccountContext(accountId: UUID) = accountContexts[accountId]

    override fun createAccountContext(context: T) {
        accountContexts[context.uuid] = context
    }

    override fun updateAccountContext(context: T) = createAccountContext(context)

    override fun deleteAccountContext(uuid: UUID) {
        accountContexts.remove(uuid)
    }
}