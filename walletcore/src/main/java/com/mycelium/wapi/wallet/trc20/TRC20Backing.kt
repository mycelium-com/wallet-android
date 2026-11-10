package com.mycelium.wapi.wallet.trc20

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.Backing
import java.util.UUID

open class TRC20Backing(walletDB: WalletDB, private val generalBacking: Backing<AccountContext>)
    : Backing<TRC20AccountContext> {
    private val trc20Queries = walletDB.trc20ContextQueries

    val accountMapper = { uuid: UUID,
                          currency: CryptoCurrency,
                          accountName: String,
                          archived: Boolean,
                          balance: Balance,
                          blockHeight: Int,
                          contractAddress: String,
                          unitExponent: Int,
                          symbol: String,
                          tronAccountId: UUID ->
        TRC20AccountContext(uuid, currency, accountName, balance,
            { updateAccountContext(it.uuid, it) },
            contractAddress, symbol, unitExponent, tronAccountId, archived, blockHeight)
    }

    override fun loadAccountContexts() =
        trc20Queries.selectAllTRC20Contexts(accountMapper).executeAsList()

    override fun loadAccountContext(accountId: UUID) =
        trc20Queries.selectTRC20ContextByUUID(accountId, accountMapper).executeAsOneOrNull()

    override fun createAccountContext(accountId: UUID, context: TRC20AccountContext) {
        generalBacking.createAccountContext(accountId, context.accountContext())
        trc20Queries.insert(context.uuid, context.contractAddress, context.unitExponent, context.symbol, context.tronAccountId)
    }

    override fun updateAccountContext(accountId: UUID, context: TRC20AccountContext) {
        generalBacking.updateAccountContext(accountId, context.accountContext())
    }

    override fun deleteAccountContext(uuid: UUID) {
        generalBacking.deleteAccountContext(uuid)
    }
}
