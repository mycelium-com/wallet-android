package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.WalletBacking
import java.util.*
import kotlin.collections.HashMap

class GenericWalletBacking<Context: AccountContext>: WalletBacking<Context> {
    private val accountContexts = HashMap<UUID, Context>()
    private val accountBackings = HashMap<UUID, AccountBacking>()

    override fun loadAccountContexts(): MutableList<Context> {
        return accountContexts.values
                .toMutableList()
    }

    override fun getAccountBacking(accountId: UUID) = accountBackings[accountId]

    override fun createAccountContext(context: Context) {
        accountContexts[context.uuid] = context
        accountBackings[context.uuid] = AccountBacking()
    }

    override fun updateAccountContext(context: Context) {
        accountContexts[context.uuid] = context
    }

    override fun deleteAccountContext(uuid: UUID) {
        accountContexts.remove(uuid)
        accountBackings.remove(uuid)
    }
}