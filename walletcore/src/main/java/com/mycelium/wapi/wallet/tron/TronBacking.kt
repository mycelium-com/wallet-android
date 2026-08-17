package com.mycelium.wapi.wallet.tron

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.Backing
import java.util.UUID

open class TronBacking(walletDB: WalletDB, private val generalBacking: Backing<AccountContext>)
    : Backing<TronAccountContext> {
    private val tronQueries = walletDB.tronContextQueries
    private val trc20Queries = walletDB.trc20ContextQueries

    val accountMapper = { uuid: UUID,
                          currency: CryptoCurrency,
                          accountName: String,
                          archived: Boolean,
                          balance: Balance,
                          blockHeight: Int,
                          enabledTokens: List<String>?,
                          accountIndex: Int,
                          address: String? ->
        val tokens = trc20Queries.selectAllTRC20ContextByParent(uuid).executeAsList()
        TronAccountContext(uuid, currency, accountName, balance,
            { updateAccountContext(it.uuid, it) },
            this::loadAccountContext,
            accountIndex, tokens.map { it.contractAddress }, archived, blockHeight, address)
    }

    override fun loadAccountContexts(): List<TronAccountContext> =
        tronQueries.selectAll(accountMapper).executeAsList()

    override fun loadAccountContext(accountId: UUID): TronAccountContext? =
        tronQueries.selectByUUID(accountId, accountMapper).executeAsOneOrNull()

    override fun createAccountContext(accountId: UUID, context: TronAccountContext) {
        generalBacking.createAccountContext(accountId, context.accountContext())
        tronQueries.insert(context.uuid, context.enabledTokens, context.accountIndex, context.address)
    }

    override fun updateAccountContext(accountId: UUID, context: TronAccountContext) {
        generalBacking.updateAccountContext(accountId, context.accountContext())
        tronQueries.update(context.enabledTokens, context.uuid)
    }

    override fun deleteAccountContext(uuid: UUID) {
        generalBacking.deleteAccountContext(uuid)
    }
}
