package com.mycelium.wapi.wallet.fio

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.Backing
import java.math.BigInteger
import java.util.*

open class FioBacking(walletDB: WalletDB, private val generalBacking: Backing<AccountContext>)
    : Backing<FioAccountContext> {
    private val fioQueries = walletDB.fioContextQueries

    override fun loadAccountContexts() = fioQueries.selectAllFioContexts(
            mapper = { uuid: UUID,
                       currency: CryptoCurrency,
                       accountName: String,
                       archived: Boolean,
                       balance: Balance,
                       blockHeight: Int,
                       accountIndex: Int,
                       accountType: Int,
                       actionSequenceNumber: BigInteger,
                       registeredFIONames: List<RegisteredFIOName>?,
                       registeredFIODomains: List<FIODomain>? ->
                FioAccountContext(uuid, currency, accountName, balance,
                    { updateAccountContext(it.uuid, it) },
                        accountIndex, registeredFIONames, registeredFIODomains, archived, blockHeight, accountType, actionSequenceNumber)
            })
            .executeAsList()

    override fun loadAccountContext(accountId: UUID) = fioQueries.selectFioContextByUUID(accountId,
            mapper = { uuid: UUID,
                       currency: CryptoCurrency,
                       accountName: String,
                       archived: Boolean,
                       balance: Balance,
                       blockHeight: Int,
                       accountIndex: Int,
                       accountType: Int,
                       actionSequenceNumber: BigInteger,
                       registeredFIONames: List<RegisteredFIOName>?,
                       registeredFIODomains: List<FIODomain>? ->
                FioAccountContext(uuid, currency, accountName, balance,
                    { updateAccountContext(it.uuid, it) },
                        accountIndex, registeredFIONames, registeredFIODomains, archived, blockHeight, accountType, actionSequenceNumber)
            })
            .executeAsOneOrNull()

    override fun createAccountContext(accountId: UUID, context: FioAccountContext) {
        generalBacking.createAccountContext(accountId, context.accountContext())
        fioQueries.insert(context.uuid, context.accountIndex, context.accountType, context.actionSequenceNumber, context.registeredFIONames,
                context.registeredFIODomains)
    }

    override fun deleteAccountContext(uuid: UUID) {
        generalBacking.deleteAccountContext(uuid)
    }

    override fun updateAccountContext(accountId: UUID, context: FioAccountContext) {
        generalBacking.updateAccountContext(accountId, context.accountContext())
        fioQueries.update(context.actionSequenceNumber, context.registeredFIONames, context.registeredFIODomains, context.uuid)
    }
}