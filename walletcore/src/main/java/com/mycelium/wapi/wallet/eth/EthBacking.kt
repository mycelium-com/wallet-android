package com.mycelium.wapi.wallet.eth

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.Backing
import java.math.BigInteger
import java.util.*

open class EthBacking(walletDB: WalletDB, private val generalBacking: Backing<AccountContext>)
    : Backing<EthAccountContext> {
    private val ethQueries = walletDB.ethContextQueries
    private val erc20Queries = walletDB.eRC20ContextQueries

    val accountMapper = { uuid: UUID,
                          currency: CryptoCurrency,
                          accountName: String,
                          archived: Boolean,
                          balance: Balance,
                          blockHeight: Int,
                          nonce: BigInteger,
                          enabledTokens: List<String>?,
                          accountIndex: Int ->
        val tokens = erc20Queries.selectAllERC20ContextByParent(uuid).executeAsList()
        EthAccountContext(uuid, currency, accountName, balance,
                this::updateAccountContext,
                this::loadAccountContext,
                accountIndex, tokens.map { it.contractAddress }, archived, blockHeight, nonce)
    }

    override fun loadAccountContexts():List<EthAccountContext> = ethQueries.selectAll(accountMapper)
            .executeAsList()

    override fun loadAccountContext(accountId: UUID): EthAccountContext? = ethQueries.selectByUUID(accountId, accountMapper)
            .executeAsOneOrNull()

    override fun createAccountContext(context: EthAccountContext) {
        generalBacking.createAccountContext(context)
        ethQueries.insert(context.uuid, context.nonce, context.enabledTokens, context.accountIndex)
    }

    override fun updateAccountContext(context: EthAccountContext) {
        generalBacking.updateAccountContext(context)
        ethQueries.update(context.nonce, context.enabledTokens, context.uuid)
    }

    override fun deleteAccountContext(uuid: UUID) {
        generalBacking.deleteAccountContext(uuid)
    }
}