package com.mycelium.wapi.wallet.erc20

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.Backing
import java.math.BigInteger
import java.util.*

open class ERC20Backing(walletDB: WalletDB, private val generalBacking: Backing<AccountContext>)
    : Backing<ERC20AccountContext> {
    private val erc20Queries = walletDB.eRC20ContextQueries

    override fun loadAccountContexts() = erc20Queries.selectAllERC20Contexts(
            mapper = { uuid: UUID,
                       currency: CryptoCurrency,
                       accountName: String,
                       archived: Boolean,
                       balance: Balance,
                       blockHeight: Int,
                       nonce: BigInteger,
                       contractAddress: String,
                       unitExponent: Int,
                       symbol: String,
                       ethAccountId: UUID ->
                ERC20AccountContext(uuid, currency, accountName, balance, this::updateAccountContext,
                        contractAddress, symbol, unitExponent, ethAccountId, archived, blockHeight, nonce)
            })
            .executeAsList()

    override fun loadAccountContext(accountId: UUID) = erc20Queries.selectERC20ContextByUUID(accountId,
            mapper = { uuid: UUID,
                       currency: CryptoCurrency,
                       accountName: String,
                       archived: Boolean,
                       balance: Balance,
                       blockHeight: Int,
                       nonce: BigInteger,
                       contractAddress: String,
                       unitExponent: Int,
                       symbol: String,
                       ethAccountId: UUID ->
                ERC20AccountContext(uuid, currency, accountName, balance, this::updateAccountContext,
                        contractAddress, symbol, unitExponent, ethAccountId, archived, blockHeight, nonce)
            })
            .executeAsOneOrNull()

    override fun createAccountContext(context: ERC20AccountContext) {
        generalBacking.createAccountContext(context)
        erc20Queries.insert(context.uuid, context.nonce, context.contractAddress, context.unitExponent, context.symbol, context.ethAccountId)
    }

    override fun updateAccountContext(context: ERC20AccountContext) {
        generalBacking.updateAccountContext(context)
        erc20Queries.update(context.nonce, context.uuid)
    }

    override fun deleteAccountContext(uuid: UUID) {
        generalBacking.deleteAccountContext(uuid)
    }
}