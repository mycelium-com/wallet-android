//package com.mycelium.wapi.wallet.fio
//
//import com.mycelium.generated.wallet.database.AccountContext
//import com.mycelium.generated.wallet.database.WalletDB
//import com.mycelium.wapi.wallet.coins.Balance
//import com.mycelium.wapi.wallet.coins.CryptoCurrency
//import com.mycelium.wapi.wallet.genericdb.Backing
//import java.math.BigInteger
//import java.util.*
//
//open class FioBacking(walletDB: WalletDB, private val generalBacking: Backing<AccountContext>)
//    : Backing<FioAccountContext> {
//    private val fioQueries = walletDB.fioContextQueries
//
//    override fun loadAccountContexts() = fioQueries.selectAll(
//            mapper = { uuid: UUID,
//                       currency: CryptoCurrency,
//                       accountName: String,
//                       archived: Boolean,
//                       balance: Balance,
//                       blockHeight: Int,
//                       nonce: BigInteger,
//                       enabledTokens: List<String>?,
//                       accountIndex: Int ->
//                FioAccountContext(uuid, currency, accountName, balance, this::updateAccountContext,
//                        accountIndex, enabledTokens, archived, blockHeight, nonce)
//            })
//            .executeAsList()
//
//    override fun loadAccountContext(accountId: UUID) = fioQueries.selectByUUID(accountId,
//            mapper = { uuid: UUID,
//                       currency: CryptoCurrency,
//                       accountName: String,
//                       archived: Boolean,
//                       balance: Balance,
//                       blockHeight: Int,
//                       nonce: BigInteger,
//                       enabledTokens: List<String>?,
//                       accountIndex: Int ->
//                FioAccountContext(uuid, currency, accountName, balance, this::updateAccountContext,
//                        accountIndex, enabledTokens, archived, blockHeight, nonce)
//            })
//            .executeAsOneOrNull()
//
//    override fun createAccountContext(context: FioAccountContext) {
//        generalBacking.createAccountContext(context)
//        fioQueries.insert(context.uuid, context.nonce, context.enabledTokens, context.accountIndex)
//    }
//
//    override fun updateAccountContext(context: FioAccountContext) {
//        generalBacking.updateAccountContext(context)
//        fioQueries.update(context.nonce, context.enabledTokens, context.uuid)
//    }
//
//    override fun deleteAccountContext(uuid: UUID) {
//        generalBacking.deleteAccountContext(uuid)
//    }
//}