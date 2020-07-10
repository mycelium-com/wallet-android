package com.mycelium.bequant

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.bequant.common.assetInfoById
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.*
import kotlin.math.pow


class InvestmentAccount : WalletAccount<BtcAddress> {
    private val id = UUID.nameUUIDFromBytes("bequant_account".toByteArray())

    private var cachedBalance = Balance(BequantPreference.getLastKnownBalance(),
            Value.zeroValue(Utils.getBtcCoinType()),
            Value.zeroValue(Utils.getBtcCoinType()),
            Value.zeroValue(Utils.getBtcCoinType()))

    @Volatile
    protected var syncing = false


    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?, data: GenericTransactionData?): GenericTransaction {
        TODO("Not yet implemented")
    }

    override fun canSign(): Boolean {
        TODO("Not yet implemented")
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        TODO("Not yet implemented")
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        TODO("Not yet implemented")
    }

    override fun getReceiveAddress(): GenericAddress? = null

    override fun getCoinType(): CryptoCurrency = Utils.getBtcCoinType()

    override fun getBasedOnCoinType(): CryptoCurrency = Utils.getBtcCoinType()

    override fun getAccountBalance(): Balance = cachedBalance

    override fun isMineAddress(address: GenericAddress?): Boolean = false

    override fun isExchangeable(): Boolean = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("Not yet implemented")
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<GenericTransactionSummary> {
        TODO("Not yet implemented")
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> {
        TODO("Not yet implemented")
    }

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("Not yet implemented")
    }

    override fun getLabel(): String = "Investment Account"

    override fun setLabel(label: String?) {
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("Not yet implemented")
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        syncing = true
        val totalBalances = mutableListOf<com.mycelium.bequant.remote.trading.model.Balance>()
        runBlocking {
            Api.accountRepository.accountBalanceGet(this, { data ->
                totalBalances.addAll(data?.toList() ?: emptyList())
            }, { code, msg -> }, {})
            Api.tradingRepository.tradingBalanceGet(this, { data ->
                totalBalances.addAll(data?.toList() ?: emptyList())
            }, { code, msg -> }, {})
        }
        var btcTotal = BigDecimal.ZERO
        for ((currency, balances) in totalBalances.groupBy { it.currency }) {
            val currencySum = balances.map {
                BigDecimal(it.available) + BigDecimal(it.reserved)
            }.reduceRight { bigDecimal, acc -> acc.plus(bigDecimal) }

            if (currencySum == BigDecimal.ZERO) continue
            if (currency!!.toUpperCase() == "BTC") {
                btcTotal += currencySum
                continue
            }

            lateinit var currencyInfo: com.mycelium.bequant.remote.trading.model.Currency
            runBlocking {
                Api.publicRepository.publicCurrencyCurrencyGet(this, currency, { response ->
                    currencyInfo = response!!
                }, { code, msg -> }, {})
            }

            val currencySumValue = Value.valueOf(currencyInfo.assetInfoById(),
                    (currencySum * 10.0.pow(currencyInfo.precisionPayout).toBigDecimal()).toBigInteger())
            val converted = BQExchangeRateManager.get(currencySumValue, Utils.getBtcCoinType())?.valueAsBigDecimal
            btcTotal += converted ?: BigDecimal.ZERO
        }
        val balance = Value.valueOf(Utils.getBtcCoinType(), (btcTotal * 10.0.pow(Utils.getBtcCoinType().unitExponent).toBigDecimal()).toBigInteger())
        BequantPreference.setLastKnownBalance(balance)
        cachedBalance = Balance(balance,
                Value.zeroValue(Utils.getBtcCoinType()),
                Value.zeroValue(Utils.getBtcCoinType()),
                Value.zeroValue(Utils.getBtcCoinType()))
        syncing = false
        return true
    }

    override fun getBlockChainHeight(): Int = 0

    override fun canSpend(): Boolean = true

    override fun isSyncing(): Boolean = false

    override fun isArchived(): Boolean = false

    override fun isActive(): Boolean = true

    override fun archiveAccount() {
    }

    override fun activateAccount() {
    }

    override fun dropCachedData() {
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = false

    override fun getId(): UUID = id

    override fun broadcastOutgoingTransactions(): Boolean = false

    override fun removeAllQueuedTransactions() {
        TODO("Not yet implemented")
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: BtcAddress?): Value {
        TODO("Not yet implemented")
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun getTypicalEstimatedTransactionSize(): Int {
        TODO("Not yet implemented")
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): BtcAddress {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(subType: String?): BtcAddress {
        TODO("Not yet implemented")
    }

    override fun getDependentAccounts(): MutableList<WalletAccount<GenericAddress>> {
        TODO("Not yet implemented")
    }

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("Not yet implemented")
    }
}