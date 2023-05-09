package com.mycelium.bequant

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.bequant.common.assetInfoById
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.*
import kotlin.math.pow
import com.mycelium.bequant.remote.trading.model.Balance as BequantBalance


class InvestmentAccount : WalletAccount<BtcAddress> {
    override val id
        get() = UUID.nameUUIDFromBytes("bequant_account".toByteArray())

    private val cachedBalance: Balance
        get() = Balance(BequantPreference.getLastKnownBalance(),
                Value.zeroValue(Utils.getBtcCoinType()),
                Value.zeroValue(Utils.getBtcCoinType()),
                Value.zeroValue(Utils.getBtcCoinType()))

    private var lastSyncInfo = SyncStatusInfo(SyncStatus.UNKNOWN)

    @Volatile
    private var syncing = false

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        TODO("Not yet implemented")
    }

    override fun canSign(): Boolean {
        TODO("Not yet implemented")
    }

    override fun signMessage(message: String, address: Address?): String {
        TODO("Not yet implemented")
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        TODO("Not yet implemented")
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        TODO("Not yet implemented")
    }

    override val receiveAddress: BtcAddress? = null

    override val coinType: CryptoCurrency
        get() = Utils.getBtcCoinType()

    override val basedOnCoinType: CryptoCurrency
        get() = Utils.getBtcCoinType()

    override val accountBalance: Balance
        get() = cachedBalance

    override fun isMineAddress(address: Address?): Boolean = false

    override fun isExchangeable(): Boolean = true

    override fun getTx(transactionId: ByteArray): Transaction? {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray): TransactionSummary? {
        TODO("Not yet implemented")
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<TransactionSummary> =
            mutableListOf()

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> =
            mutableListOf()

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> =
            mutableListOf()

    override var label: String = "Trading Account"

    override fun isSpendingUnconfirmed(tx: Transaction): Boolean = false

    override suspend fun synchronize(mode: SyncMode?): Boolean {
        syncing = true
        val totalBalances = mutableListOf<BequantBalance>()
        if(BequantPreference.hasKeys()) {
            runBlocking {
                Api.accountRepository.accountBalanceGet(this, { data ->
                    totalBalances.addAll(data?.toList() ?: emptyList())
                }, { _, _ -> }, {})
                Api.tradingRepository.tradingBalanceGet(this, { data ->
                    totalBalances.addAll(data?.toList() ?: emptyList())
                }, { _, _ -> }, {})
            }
        }
        if(totalBalances.isNotEmpty()) {
            var btcTotal = BigDecimal.ZERO
            for ((currency, balances) in totalBalances.groupBy { it.currency }) {
                val currencySum = balances.map {
                    BigDecimal(it.available) + BigDecimal(it.reserved)
                }.reduceRight { bigDecimal, acc -> acc.plus(bigDecimal) }

                if (currencySum == BigDecimal.ZERO) continue
                if (currency!!.toUpperCase(Locale.US) == "BTC") {
                    btcTotal += currencySum
                    continue
                }

                runBlocking {
                    Api.publicRepository.publicCurrencyCurrencyGet(this, currency, { currencyInfo ->
                        currencyInfo?.let {
                            val currencySumValue = Value.valueOf(currencyInfo.assetInfoById(),
                                    (currencySum * 10.0.pow(currencyInfo.precisionPayout).toBigDecimal()).toBigInteger())
                            val converted = BQExchangeRateManager.get(currencySumValue, Utils.getBtcCoinType())?.valueAsBigDecimal
                            btcTotal += converted ?: BigDecimal.ZERO
                        }
                    }, { _, _ -> }, {})
                }
            }
            val balance = Value.valueOf(Utils.getBtcCoinType(), (btcTotal * 10.0.pow(Utils.getBtcCoinType().unitExponent).toBigDecimal()).toBigInteger())
            BequantPreference.setLastKnownBalance(balance)
            lastSyncInfo = SyncStatusInfo(SyncStatus.SUCCESS)
        }
        syncing = false
        return true
    }

    override fun getBlockChainHeight(): Int = 0

    override fun canSpend(): Boolean = false

    override fun isSyncing(): Boolean = syncing

    override fun lastSyncStatus(): SyncStatusInfo = lastSyncInfo

    override fun setLastSyncStatus(syncStatusInfo: SyncStatusInfo) {
        lastSyncInfo = syncStatusInfo
    }

    override val isArchived: Boolean
        get() = false

    override val isActive: Boolean
        get() = SettingsPreference.isEnabled(BequantConstants.PARTNER_ID)

    override fun archiveAccount() {
    }

    override fun activateAccount() {
    }

    override fun dropCachedData() {
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = false

    override fun broadcastOutgoingTransactions(): Boolean = true

    override fun removeAllQueuedTransactions() {
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: BtcAddress?, txData: TransactionData?): Value {
        TODO("Not yet implemented")
    }

    override val syncTotalRetrievedTransactions: Int = 0

    override val typicalEstimatedTransactionSize: Int = 0

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override val dummyAddress: BtcAddress =
        BtcAddress(coinType, BitcoinAddress.getNullAddress(NetworkParameters.testNetwork))

    override fun getDummyAddress(subType: String): BtcAddress {
        TODO("Not yet implemented")
    }

    override val dependentAccounts: List<WalletAccount<Address>> = listOf()

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

    override fun interruptSync() {}
}