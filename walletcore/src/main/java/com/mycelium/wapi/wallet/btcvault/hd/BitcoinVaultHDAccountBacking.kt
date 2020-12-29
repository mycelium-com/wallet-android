package com.mycelium.wapi.wallet.btcvault.hd

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.genericdb.Backing
import java.util.*

class BitcoinVaultHDAccountBacking(walletDB: WalletDB,
                                   private val generalBacking: Backing<AccountContext>)
    : Backing<BitcoinVaultHDAccountContext> {

    private val btcvQueries = walletDB.bTCVContextQueries

    override fun loadAccountContexts(): List<BitcoinVaultHDAccountContext> =
            btcvQueries.selectAllBTCVContexts { uuid, currency, accountName, archived, balance, blockHeight, accountIndex ->
                BitcoinVaultHDAccountContext(uuid, currency, accountIndex, archived, accountName, balance, this::updateAccountContext, blockHeight)
            }.executeAsList()

    override fun loadAccountContext(accountId: UUID): BitcoinVaultHDAccountContext? =
            btcvQueries.selectBTCVContextByUUID(accountId, mapper = { uuid, currency, accountName, archived, balance, blockHeight, accountIndex ->
                BitcoinVaultHDAccountContext(uuid, currency, accountIndex, archived, accountName, balance, this::updateAccountContext, blockHeight)
            }).executeAsOne()

    override fun createAccountContext(context: BitcoinVaultHDAccountContext) {
        generalBacking.createAccountContext(context)
        btcvQueries.insert(context.id, context.accountIndex)
    }

    override fun updateAccountContext(context: BitcoinVaultHDAccountContext) {
        generalBacking.updateAccountContext(context)
    }

    override fun deleteAccountContext(uuid: UUID) {
        generalBacking.deleteAccountContext(uuid)
    }
}