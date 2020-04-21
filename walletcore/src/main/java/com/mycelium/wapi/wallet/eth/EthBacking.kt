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

    override fun loadAccountContexts() = ethQueries.selectAll(
            mapper = { uuid: UUID,
                       currency: CryptoCurrency,
                       accountName: String,
                       archived: Boolean,
                       balance: Balance,
                       blockHeight: Int,
                       nonce: BigInteger,
                       enabledTokens: List<String>?,
                       accountIndex: Int ->
                EthAccountContext(uuid, currency, accountName, balance, this::updateAccountContext,
                        accountIndex, enabledTokens, archived, blockHeight, nonce)
            })
            .executeAsList()

    override fun loadAccountContext(accountId: UUID) = ethQueries.selectByUUID(accountId,
            mapper = { uuid: UUID,
                       currency: CryptoCurrency,
                       accountName: String,
                       archived: Boolean,
                       balance: Balance,
                       blockHeight: Int,
                       nonce: BigInteger,
                       enabledTokens: List<String>?,
                       accountIndex: Int ->
                EthAccountContext(uuid, currency, accountName, balance, this::updateAccountContext,
                        accountIndex, enabledTokens, archived, blockHeight, nonce)
            })
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