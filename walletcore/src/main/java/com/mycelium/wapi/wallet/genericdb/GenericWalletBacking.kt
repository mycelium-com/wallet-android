package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import java.util.*

class GenericWalletBacking(walletDB: WalletDB) {
    private val queries = walletDB.accountContextQueries

    fun loadAccountContexts() = queries.selectAll()
                .executeAsList()

    fun loadAccountContext(accountId: UUID) = queries.selectByUUID(accountId)
            .executeAsOneOrNull()

    fun createAccountContext(context: AccountContext) {
        queries.insertFullObject(context)
    }

    fun updateAccountContext(context: AccountContext) {
        queries.update(context.accountName, context.balance, context.archived, context.uuid)
    }

    fun deleteAccountContext(uuid: UUID) {
        queries.delete(uuid)
    }
}