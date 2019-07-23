package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.AccountContext
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import com.squareup.sqldelight.runtime.rx.asObservable
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import java.util.*

class EthAccount(val credentials: Credentials, db: WalletDB) : WalletAccount<EthAddress> {
    private val queries = db.accountContextQueries
    private val coinType = EthTest
    private val accountContext: AccountContext

    init {
        val uuid = credentials.ecKeyPair.toUUID()
        val accountContextInDB = queries.selectByUUID(uuid)
                .executeAsOneOrNull()
        accountContext = if (accountContextInDB != null) {
            AccountContextImpl(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    accountContextInDB.archived)
        } else {
            AccountContextImpl(
                    uuid,
                    coinType,
                    "abacaba",
                    Balance(Value.zeroValue(coinType),
                            Value.zeroValue(coinType),
                            Value.zeroValue(coinType),
                            Value.zeroValue(coinType)),
                    false)
        }
    }

    override fun getDefaultFeeEstimation(): FeeEstimationsGeneric {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createTx(addres: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress() = EthAddress(coinType, credentials.address)


    override fun getCoinType() = coinType

    override fun getBasedOnCoinType() = getCoinType()

    private val ethBalanceService = EthBalanceService(credentials.address)

    override fun getAccountBalance(): Balance {
        return accountContext.balance
    }

    override fun isMineAddress(address: GenericAddress) =
            address == EthAddress(coinType, "0x60c2A43Cc69658eC4b02a65A07623D7192166F4e")

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummaries(offset: Int, limit: Int) = emptyList<GenericTransactionSummary>()

    override fun getTransactionsSince(receivingSince: Long) = emptyList<GenericTransactionSummary>()

    override fun getTransactions(offset: Int, limit: Int) = emptyList<GenericTransaction>()

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel() = accountContext.accountName

    override fun setLabel(label: String?) {
        accountContext.accountName = label!!
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val succeed = ethBalanceService.updateBalanceCache()
        if (succeed) {
            val balance = Balance(Value.valueOf(EthTest, ethBalanceService.balance), Value(EthTest, 0),
                    Value(EthTest, 0), Value(EthTest, 0))
            queries.update(accountContext.accountName,
                    balance,
                    accountContext.archived,
                    accountContext.uuid)
            accountContext.balance = balance
        }
        return succeed
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = true

    override fun isSyncing() = false

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun activateAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dropCachedData() {
        val balance = Balance(Value.valueOf(EthTest, 0),
                Value(EthTest, 0),
                Value(EthTest, 0),
                Value(EthTest, 0))
        queries.update(accountContext.accountName,
                balance,
                accountContext.archived,
                accountContext.uuid)
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override fun getId() = credentials.ecKeyPair.toUUID()

    override fun isSynchronizing(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastOutgoingTransactions() = true

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Long, destinationAddress: EthAddress?): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSyncTotalRetrievedTransactions() = 0

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(): EthAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(subType: String?): EthAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun ECKeyPair.toUUID(): UUID = UUID(BitUtils.uint64ToLong(publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
        publicKey.toByteArray(), 16))
