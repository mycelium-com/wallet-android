package com.mycelium.wallet.external.changelly2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mrd.bitlib.TransactionUtils
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toFriendlyString
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.external.changelly.model.FixRate
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wallet.external.changelly2.remote.changellyTicker
import com.mycelium.wallet.external.changelly2.remote.importSymbol
import com.mycelium.wapi.wallet.trc20.TRC20Account
import com.mycelium.wapi.wallet.tron.energy.EnergyFeeQuote
import com.mycelium.wapi.wallet.tron.energy.EnergyServiceException
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal


class ExchangeViewModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    var currencies = setOf("BTC", "ETH")
    val fromAccount = MutableLiveData<WalletAccount<*>>()
    val exchangeInfo = MutableLiveData<FixRate>()
    val sellValue = object : MutableLiveData<String>() {
        override fun setValue(value: String?) {
            if (this.value != value) {
                super.setValue(value)
            }
        }
    }
    val buyValue = MutableLiveData<String>()
    val errorKeyboard = MutableLiveData("")
    val errorTransaction = MutableLiveData("")
    val errorRemote = MutableLiveData("")
    val keyboardActive = MutableLiveData(false)
    val rateLoading = MutableLiveData(false)
    var changellyTx: String? = null
    var swapDirection = 0

    val toAccount = MediatorLiveData<WalletAccount<*>>().apply {
        addSource(fromAccount) {
            if (value?.coinType == it?.coinType) {
                viewModelScope.launch(Dispatchers.IO) { postValue(getToAccountForInit()) }
            }
        }
    }
    val swapEnableDelay = MutableLiveData(false)
    val swapEnabled = MediatorLiveData<Boolean>().apply {
        value = false
        fun update() {
            value = toAccount.value?.canSpend() ?: false
                    && rateLoading.value == false
                    && swapEnableDelay.value == false
        }
        addSource(toAccount) {
            update()
        }
        addSource(rateLoading) {
            update()
        }
        addSource(swapEnableDelay) {
            update()
        }
    }

    val error = MediatorLiveData<String>().apply {
        value = ""
        fun error() =
                when {
                    keyboardActive.value == true -> ""
                    errorKeyboard.value?.isNotEmpty() == true -> errorKeyboard.value
                    errorTransaction.value?.isNotEmpty() == true -> errorTransaction.value
                    errorRemote.value?.isNotEmpty() == true -> errorRemote.value
                    else -> ""
                }
        addSource(errorKeyboard) {
            value = error()
        }
        addSource(errorTransaction) {
            value = error()
        }
        addSource(keyboardActive) {
            value = error()
        }
        addSource(fromAccount) {
            errorKeyboard.value = ""
            errorTransaction.value = ""
            errorRemote.value = ""
        }
        addSource(toAccount) {
            errorKeyboard.value = ""
            errorTransaction.value = ""
            errorRemote.value = ""
        }
    }


    val fromCurrency = fromAccount.map {
        it?.coinType
    }
    val fromAddress = fromAccount.map {
        it?.receiveAddress?.toString()
    }
    val fromChain = fromAccount.map {
        if (it?.basedOnCoinType != it?.coinType) it?.basedOnCoinType?.name else ""
    }
    val fromFiatBalance = fromAccount.map {
        it?.let {
            mbwManager.exchangeRateManager
                .get(it.accountBalance.spendable, mbwManager.getFiatCurrency(it.coinType))
                ?.toStringFriendlyWithUnit()
        }
    }
    val fromSendable = fromAccount.map {
        it?.let {
            it.accountBalance.spendable.toStringFriendlyWithUnit()
        }
    }

    val toCurrency = toAccount.map {
        it?.coinType ?: Utils.getBtcCoinType()
    }
    val toAddress = toAccount.map {
        it?.receiveAddress?.toString()
    }
    val toChain = toAccount.map {
        if (it?.basedOnCoinType != it?.coinType) it?.basedOnCoinType?.name else ""
    }
    val toBalance = toAccount.map {
        it?.accountBalance?.spendable?.toStringFriendlyWithUnit()
    }
    val toFiatBalance = toAccount.map {
        it?.accountBalance?.spendable?.let { value ->
            mbwManager.exchangeRateManager
                    .get(value, mbwManager.getFiatCurrency(it.coinType))
                    ?.toStringFriendlyWithUnit()
        }
    }
//    val exchangeRate = Transformations.map(exchangeInfo) {
//        "1 ${it.from.toUpperCase()} = ${it.result} ${it.to.toUpperCase()}"
//    }

    val exchangeRateFrom = exchangeInfo.map {
        fromDisplaySymbol()?.let { "1 $it" }
    }

    /**
     * Symbol shown for the sold asset: the wallet's own (e.g. "USDT" for TRC20 USDT) rather than
     * the backend's network-suffixed ticker ("USDTTRX") echoed in [exchangeInfo].
     */
    fun fromDisplaySymbol(): String? = fromCurrency.value?.symbol ?: exchangeInfo.value?.from?.uppercase()

    val exchangeRateToValue = exchangeInfo.map {
        if (it?.result != null && it.result != BigDecimal.ZERO)
            " = " + it.result.stripTrailingZeros().toPlainString()
        else
            " ~ " + (mbwManager.exchangeRateManager.get(
                fromCurrency.value?.value("1"),
                toCurrency.value
            )?.toFriendlyString(Denomination.UNIT)
                ?: N_A)
    }

    val exchangeRateToCurrency = exchangeInfo.map {
        toCurrency.value?.symbol ?: it?.to?.uppercase()
    }

    val fiatSellValue = sellValue.map {
        if (it?.isNotEmpty() == true) {
            try {
                mbwManager.exchangeRateManager
                        .get(fromCurrency.value?.value(it), mbwManager.getFiatCurrency(fromCurrency.value))
                        ?.toStringFriendlyWithUnit()?.let { "≈$it" }
            } catch (e: NumberFormatException) {
                N_A
            }
        } else {
            ""
        }
    }
    val fiatBuyValue = buyValue.map {
        if (it?.isNotEmpty() == true) {
            try {
                mbwManager.exchangeRateManager
                        .get(toCurrency.value?.value(it), mbwManager.getFiatCurrency(toCurrency.value))
                        ?.toStringFriendlyWithUnit()?.let { "≈$it" }
            } catch (e: NumberFormatException) {
                N_A
            }
        } else {
            ""
        }
    }

    val minerFee = MutableLiveData("")

    // ---- TRON energy service: pay a TRC20 send's fee in the token when the parent has no TRX
    /** Quote for the current from-account/amount, null when not fetched or not applicable. */
    val energyQuote = MutableLiveData<EnergyFeeQuote?>(null)
    val energyQuoteLoading = MutableLiveData(false)
    @Volatile
    private var energyQuoteKey: String? = null
    private var energyQuoteJob: kotlinx.coroutines.Job? = null

    val validateData = MediatorLiveData<Boolean>().apply {
        value = isValid()
        addSource(energyQuote) {
            value = isValid()
        }
        addSource(toAddress) {
            value = isValid()
        }
        addSource(fromAddress) {
            value = isValid()
        }
        addSource(sellValue) {
            value = isValid()
        }
        addSource(exchangeInfo) {
            value = isValid()
        }
        addSource(rateLoading) {
            value = isValid()
        }
    }

    fun isValid(): Boolean =
            try {
                errorTransaction.value = ""
                minerFee.value = ""
                val res = getApplication<WalletApplication>().resources
                val amount = sellValue.value?.toBigDecimal()
                when {
                    toAddress.value == null -> false
                    fromAddress.value == null -> false
                    rateLoading.value == true -> false
                    amount == null -> false
                    amount == BigDecimal.ZERO -> false
                    // no fix-rate quote (backend error, pair below minimum, …): nothing to exchange against
                    exchangeInfo.value?.result == null -> false
                    exchangeInfo.value?.minFrom != null && amount < exchangeInfo.value?.minFrom  -> {
                        errorTransaction.value = res.getString(R.string.exchange_min_msg,
                                exchangeInfo.value?.minFrom?.stripTrailingZeros()?.toPlainString(),
                                fromDisplaySymbol())
                        false
                    }
                    exchangeInfo.value?.maxFrom != null && amount > exchangeInfo.value?.maxFrom -> {
                        errorTransaction.value = res.getString(R.string.exchange_max_msg,
                                exchangeInfo.value?.maxFrom?.stripTrailingZeros()?.toPlainString(),
                                fromDisplaySymbol())
                        false
                    }
                    else -> checkValidTransaction() != null
                }
            } catch (e: java.lang.NumberFormatException) {
                false
            }

    fun checkValidTransaction(): Transaction? {
        val res = getApplication<WalletApplication>().resources
        val account = fromAccount.value!!
        val value = account.coinType.value(sellValue.value!!)
        if (value.equalZero()) {
            return null
        }
        val feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation
        try {
            return prepateTx(account.dummyAddress, sellValue.value!!)
                    ?.apply {
                        minerFee.value =
                                res.getString(R.string.miner_fee) + " " +
                                        this.totalFee().toStringFriendlyWithUnit() + " " +
                                        mbwManager.exchangeRateManager
                                                .get(this.totalFee(), mbwManager.getFiatCurrency(this.type))
                                                ?.toStringFriendlyWithUnit()?.let { "≈$it" }
                    }
        } catch (e: OutputTooSmallException) {
            errorTransaction.value = res.getString(R.string.amount_too_small_short,
                    Value.valueOf(account.coinType, TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit())
        } catch (e: InsufficientFundsForFeeException) {
            if (account is ERC20Account) {
                val fee = feeEstimation.normal.times(account.typicalEstimatedTransactionSize.toBigInteger())
                errorTransaction.value = res.getString(R.string.please_top_up_your_eth_account,
                        account.ethAcc.label, fee.toStringFriendlyWithUnit(), convert(fee)) + TAG_ETH_TOP_UP
            } else if (account is TRC20Account && mbwManager.energyServiceManager != null) {
                return energyServiceTx(account, value)
            } else {
                errorTransaction.value = res.getString(R.string.insufficient_funds_for_fee)
            }
        } catch (e: InsufficientFundsException) {
            errorTransaction.value = res.getString(R.string.insufficient_funds)
        } catch (e: BuildTransactionException) {
            mbwManager.reportIgnoredException("MinerFeeException", e)
            errorTransaction.value = res.getString(R.string.tx_build_error) + " " + e.message
        } catch (e: Exception) {
            errorTransaction.value = res.getString(R.string.tx_build_error) + " " + e.message
        }
        return null
    }

    /**
     * The parent TRON account cannot pay the fee in TRX: fall back to the energy service (fee in
     * the token). Returns a placeholder tx once a valid quote covers `amount + fee`, otherwise
     * fetches the quote and reports "not yet valid" — the quote arrival re-validates.
     */
    private fun energyServiceTx(account: TRC20Account, value: Value): Transaction? {
        val res = getApplication<WalletApplication>().resources
        val manager = mbwManager.energyServiceManager ?: return null
        val payin = fromAccount.value?.receiveAddress ?: return null // real recipient unknown until Changelly answers
        val key = "${account.id}|${value.value}"
        val quote = energyQuote.value
        if (quote == null || quote.isExpired() || energyQuoteKey != key) {
            requestEnergyQuote(account, value, key)
            minerFee.value = res.getString(R.string.tron_energy_service_loading)
            return null
        }
        val fee = manager.feeInAccountUnits(account, quote)
        if (value + fee > account.accountBalance.spendable) {
            errorTransaction.value = res.getString(R.string.error_energy_service_insufficient_balance, account.coinType.symbol)
            return null
        }
        minerFee.value = res.getString(R.string.miner_fee) + " " + fee.toStringFriendlyWithUnit() +
                " " + (mbwManager.exchangeRateManager.get(fee, mbwManager.getFiatCurrency(fee.type))?.toStringFriendlyWithUnit()?.let { "≈$it" } ?: "") +
                " (" + res.getString(R.string.tron_energy_service_title) + ")"
        return account.createEnergyServiceTransferTx(payin as com.mycelium.wapi.wallet.tron.TronAddress, value,
                java.math.BigInteger.valueOf(com.mycelium.wapi.wallet.tron.TronFeeEstimator.MIN_TRC20_FEE_LIMIT),
                com.mycelium.wapi.wallet.tron.energy.EnergyServiceManager.ENERGY_TX_EXPIRATION_MS)
    }

    private fun requestEnergyQuote(account: TRC20Account, value: Value, key: String) {
        val manager = mbwManager.energyServiceManager ?: return
        if (energyQuoteLoading.value == true && energyQuoteKey == key) return
        energyQuoteKey = key
        energyQuoteJob?.cancel()
        energyQuoteLoading.value = true
        energyQuoteJob = viewModelScope.launch(Dispatchers.IO) {
            val quote = try {
                if (!manager.isAvailableFor(account)) null
                else manager.getFeeQuote(account, account.receivingAddress, value)
            } catch (e: EnergyServiceException) {
                errorTransaction.postValue(if (e.isInsufficientBalance)
                    getApplication<WalletApplication>().getString(R.string.error_energy_service_insufficient_balance, account.coinType.symbol)
                else e.message)
                null
            } catch (e: Exception) {
                errorTransaction.postValue(getApplication<WalletApplication>().getString(R.string.error_energy_service_unavailable))
                null
            }
            energyQuoteLoading.postValue(false)
            if (quote == null && energyQuoteKey == key) {
                energyQuoteKey = null
                if (errorTransaction.value.isNullOrEmpty()) {
                    errorTransaction.postValue(getApplication<WalletApplication>().getString(R.string.insufficient_funds_for_fee))
                }
            }
            energyQuote.postValue(quote)
        }
    }

    /** True when the current from-account send will be settled through the energy service. */
    fun usesEnergyService(): Boolean {
        val account = fromAccount.value as? TRC20Account ?: return false
        val quote = energyQuote.value ?: return false
        if (quote.isExpired()) return false
        val amount = try { account.coinType.value(sellValue.value ?: return false) } catch (e: Exception) { return false }
        return energyQuoteKey == "${account.id}|${amount.value}"
    }

    /**
     * Refresh the from-account's fee estimation for ETH/ERC-20 before building
     * the actual exchange tx. The exchange flow otherwise only reads the cached
     * estimation, so with EIP-1559 the fee snapshot may be empty/stale and the
     * send would silently fall back to legacy signing. Gated to ETH-based
     * accounts so other coins don't pay an extra network round-trip; safe to
     * call from a coroutine (updateFeeEstimationsAsync swallows failures and
     * keeps the previous estimate).
     */
    suspend fun refreshEthFeesForSend() {
        val account = fromAccount.value ?: return
        if (account.basedOnCoinType is EthCoin) {
            mbwManager.getFeeProvider(account.basedOnCoinType).updateFeeEstimationsAsync()
        }
    }

    fun prepateTx(address: Address, amount: String): Transaction? {
        val res = getApplication<WalletApplication>().resources
        val account = fromAccount.value!!
        val value = account.coinType.value(amount)
        if (value.equalZero()) {
            return null
        }
        val feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation
        try {
            return account.createTx(
                    address,
                    value,
                    FeePerKbFee(feeEstimation.normal),
                    null
            )
        } catch (e: OutputTooSmallException) {
            errorTransaction.postValue(res.getString(R.string.amount_too_small_short,
                    Value.valueOf(account.coinType, TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit()))
        } catch (e: InsufficientFundsForFeeException) {
            if (account is ERC20Account) {
                val parentAccountBalance = account.ethAcc.accountBalance.spendable
                val topUpForFee = feeEstimation.normal.times(account.typicalEstimatedTransactionSize.toBigInteger()) - parentAccountBalance
                errorTransaction.postValue(res.getString(R.string.please_top_up_your_eth_account,
                        account.ethAcc.label, topUpForFee.toStringFriendlyWithUnit(), convert(topUpForFee)) + TAG_ETH_TOP_UP)
            } else {
                errorTransaction.postValue(res.getString(R.string.insufficient_funds_for_fee))
            }
        } catch (e: InsufficientFundsException) {
            errorTransaction.postValue(res.getString(R.string.insufficient_funds))
        } catch (e: BuildTransactionException) {
            mbwManager.reportIgnoredException("MinerFeeException", e)
            errorTransaction.postValue(res.getString(R.string.tx_build_error) + " " + e.message)
        } catch (e: Exception) {
            errorTransaction.postValue(res.getString(R.string.tx_build_error) + " " + e.message)
        }
        return null
    }

    fun convert(value: Value) =
            " ~${mbwManager.exchangeRateManager.get(value, mbwManager.getFiatCurrency(value.type))?.toStringFriendlyWithUnit() ?: ""}"


    fun getToAccountForInit() = Utils.sortAccounts(mbwManager.getWalletManager(false)
            .getAllActiveAccounts(), mbwManager.metadataStorage)
            .firstOrNull {
                it.coinType != fromAccount.value?.coinType
                        && isSupported(it.coinType)
            }

    fun isSupported(coinType: CryptoCurrency): Boolean {
        val ticker = changellyTicker(coinType)
        // TRC20 tokens travel under the plain ticker + network; the backend's currency list only
        // carries tickers, so a listed ticker is taken as supported (as the KMP wallet does).
        return currencies.any { it.equals(ticker, true) || it.equals(importSymbol(ticker), true) }
    }

    companion object {
        const val TAG_ETH_TOP_UP = "<hiden type=\"TAG_ETH_TOP_UP\"/>"
        const val N_A = "N/A"
    }

    fun reset() {
        sellValue.value = ""
        buyValue.value = ""
        errorTransaction.value = ""
        errorRemote.value = ""
        errorKeyboard.value = ""
        fromAccount.value = fromAccount.value
        toAccount.value = toAccount.value
    }
}