package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.AccountContextQueries
import com.mycelium.generated.wallet.database.WalletDB
import java.util.*

class InMemoryAccountContextsBacking() : GenericBacking {
    private val accountContexts = hashMapOf<UUID, AccountContext>()
    override fun loadAccountContexts() = accountContexts.values.toList()

    override fun loadAccountContext(accountId: UUID) = accountContexts[accountId]

    override fun createAccountContext(context: AccountContext) {
        accountContexts[context.uuid] = context
    }

    override fun updateAccountContext(context: AccountContext) = createAccountContext(context)

    override fun deleteAccountContext(uuid: UUID) {
        accountContexts.remove(uuid)
    }
}