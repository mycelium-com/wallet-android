package com.mycelium.wallet.activity.send.model

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.mycelium.view.Denomination
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.NoneItem
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.SpinnerItem
import com.mycelium.wallet.activity.send.TransactionItem
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wallet.activity.util.AdaptiveDateFormat
import com.mycelium.wallet.activity.util.displaySymbol
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.util.get
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.ERC20Account.Companion.TOKEN_TRANSFER_GAS_LIMIT
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthTransactionData
import com.mycelium.wapi.wallet.eth.aa.AATransactionManager
import com.mycelium.wapi.wallet.eth.aa.FeeQuote
import com.mycelium.wapi.wallet.eth.aa.FeeTokenConfig
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.Address
import com.mycelium.wallet.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

/** How the gas of an ERC-20 transfer is paid. */
enum class EthFeeMode {
    /** From the parent ETH account (standard transaction). */
    ETH,
    /** In the token itself, through an ERC-4337 paymaster (UserOperation). */
    TOKEN
}

class SendEthModel(application: Application,
                   account: WalletAccount<*>,
                   intent: Intent)
    : SendCoinsModel(application, account, intent) {
    var txItems: List<SpinnerItem> = emptyList()
    val parentAccount: EthAccount? = (account as? ERC20Account)?.ethAcc
    val gasLimitStatus: MutableLiveData<GasLimitStatus> = MutableLiveData(GasLimitStatus.OK)
    val denomination: Denomination = mbwManager.getDenomination(Utils.getEthCoinType())

    val nonce: MutableLiveData<BigInteger?> = object : MutableLiveData<BigInteger?>() {
        override fun setValue(value: BigInteger?) {
            if (value != this.value) {
                super.setValue(value)
                val oldData = (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value = EthTransactionData(value, oldData.gasLimit, oldData.inputData, oldData.suggestedGasPrice,
                        oldData.maxPriorityFeePerGas, oldData.tokenFee)
            }
        }
    }

    val selectedTxItem: MutableLiveData<SpinnerItem> = object : MutableLiveData<SpinnerItem>() {
        override fun setValue(value: SpinnerItem) {
            if (value != this.value) {
                super.setValue(value)
                val oldData = (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                when (value) {
                    is NoneItem -> {
                        // the gas price field still shows the replacement's price — clear it with the replacement
                        if (replacementGasPrice != null && gasPrice.value == replacementGasPrice) {
                            presetGasPriceText.value = ""
                        }
                        replacementGasPrice = null
                        replacementTip = null
                        nonce.value = null
                        transactionData.value = EthTransactionData(null, oldData.gasLimit, oldData.inputData, null, tokenFee = oldData.tokenFee)
                        applyTxPrefill(null)
                    }
                    is TransactionItem -> {
                        val tx = value.tx as EthTransactionSummary
                        val oldGasPrice = when {
                            tx.gasPrice > BigInteger.ZERO -> tx.gasPrice
                            tx.fee != null && tx.gasUsed > BigInteger.ZERO -> tx.fee!!.value / tx.gasUsed
                            else -> null
                        }
                        // a replacement must raise both the cap and the tip by >= 10 %; the old tip is not
                        // stored, but it never exceeds the old cap, so a tip of oldCap * 1.1 always clears it
                        val bumped = oldGasPrice?.let { it * BigInteger.valueOf(110) / BigInteger.valueOf(100) }
                        val suggestedGasPrice = bumped?.let { feeEstimation.normal.value.max(it) }
                        replacementGasPrice = suggestedGasPrice
                        replacementTip = bumped
                        nonce.value = tx.nonce
                        transactionData.value = EthTransactionData(tx.nonce, oldData.gasLimit, oldData.inputData, suggestedGasPrice, bumped, oldData.tokenFee)
                        // show the bumped price in the Advanced gas price field (M-74); applying the
                        // field also fills the default gas limit and switches the fee display to it
                        suggestedGasPrice?.let {
                            presetGasPriceText.value = Convert.fromWei(BigDecimal(it), Convert.Unit.GWEI)
                                    .stripTrailingZeros().toPlainString()
                        }
                        // recipient and amount follow every selection, not only the preselected one
                        applyTxPrefill(tx)
                    }
                }
            }
        }
    }

    val gasLimit: MutableLiveData<BigInteger?> = object : MutableLiveData<BigInteger?>() {
        override fun setValue(value: BigInteger?) {
            if (value != this.value) {
                gasLimitStatus.value = if (value != null) {
                    when {
                        value < Transfer.GAS_LIMIT -> GasLimitStatus.ERROR
                        value > EthAccount.MAX_TX_GAS_LIMIT -> GasLimitStatus.TOO_HIGH
                        account is ERC20Account && value < BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT) -> GasLimitStatus.WARNING
                        else -> GasLimitStatus.OK
                    }
                } else {
                    GasLimitStatus.OK
                }

                super.setValue(value)
                val oldData =
                    (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value =
                    EthTransactionData(oldData.nonce, value, oldData.inputData, oldData.suggestedGasPrice,
                            oldData.maxPriorityFeePerGas, oldData.tokenFee)
            }
        }
    }

    /**
     * Bumped cap and tip of the tx selected for replacement, null without one. A cleared gas price
     * field (it is reset to "not set" on every keystroke) falls back to them, so the replacement
     * never goes out at the plain network price, and a typed price keeps the bumped tip.
     */
    private var replacementGasPrice: BigInteger? = null
    private var replacementTip: BigInteger? = null

    /**
     * Recipient and amount the replace spinner put in, null when none. Declared before `init`:
     * the preselection there sets them, and a later initializer would reset them.
     */
    private var prefilledAddress: Address? = null
    private var prefilledAmount: Value? = null

    /** Text the app puts into the gas price field (replacement prefill); null = leave the field alone. */
    val presetGasPriceText: MutableLiveData<String?> = MutableLiveData()

    val gasPrice: MutableLiveData<BigInteger?> = object : MutableLiveData<BigInteger?>() {
        override fun setValue(value: BigInteger?) {
            if (value != this.value) {
                super.setValue(value)
                val oldData =
                        (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value =
                        EthTransactionData(oldData.nonce, oldData.gasLimit, oldData.inputData, value ?: replacementGasPrice,
                                // the tip may not exceed the cap
                                if (value != null) (replacementTip ?: oldData.maxPriorityFeePerGas)?.min(value) else replacementTip,
                                oldData.tokenFee)
                if (value != null) {
                    feeUpdater.stop()
                } else {
                    feeUpdater.start()
                }
            }
        }
    }

    val inputData: MutableLiveData<String?> = object : MutableLiveData<String?>() {
        override fun setValue(value: String?) {
            if (value != this.value) {
                super.setValue(value)
                val oldData = (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value = EthTransactionData(oldData.nonce, oldData.gasLimit,
                        if (value == null || value.isEmpty()) null else value, oldData.suggestedGasPrice,
                        oldData.maxPriorityFeePerGas, oldData.tokenFee)
            }
        }
    }

    /**
     * An Advanced field (nonce, gas limit, gas price, input data) holds text that does not parse. The field
     * then falls back to "not set", which would let Send go out with values the user did not mean.
     */
    @Volatile
    private var advancedInputInvalid = false

    fun setAdvancedInputInvalid(invalid: Boolean) {
        if (advancedInputInvalid == invalid) return
        advancedInputInvalid = invalid
        txRebuildPublisher.onNext(Unit)
    }

    val estimatedFee: MutableLiveData<Value> = MutableLiveData()
    val totalFee: MutableLiveData<Value> = MutableLiveData()
    val feeUpdater = FeeUpdater(FEE_UPDATE_INTERVAL, { feeUpdateRoutine() })

    // ---- gas paid in the token (ERC-4337 paymaster + EIP-7702)
    private val aa: AATransactionManager? = mbwManager.aaTransactionManager
    /** Fee token config when this account's token can pay for its own gas, null otherwise. */
    private val feeToken: FeeTokenConfig? = (account as? ERC20Account)
        ?.takeIf { aa != null && it.canSpend() && it.chainId == aa.config.chainId }
        ?.let { aa?.feeToken((it.coinType as ERC20Token).contractAddress) }
    val feeMode: MutableLiveData<EthFeeMode> = MutableLiveData(EthFeeMode.ETH)
    val tokenFeeAvailable: MutableLiveData<Boolean> = MutableLiveData(false)
    val tokenFeeText: MutableLiveData<String> = MutableLiveData("")
    val tokenFeeMaxText: MutableLiveData<String> = MutableLiveData("")
    val tokenFeeLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val tokenFeeError: MutableLiveData<String> = MutableLiveData("")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var quoteJob: Job? = null
    // Read by the tx builder on a background thread — kept apart from the LiveData (postValue is async)
    @Volatile
    private var currentQuote: FeeQuote? = null
    @Volatile
    private var currentFeeMode = EthFeeMode.ETH
    /** Recipient [currentQuote] was fetched for. */
    @Volatile
    private var quotedFor: String? = null

    private val addressObserver = { _: Address? ->
        if (currentFeeMode == EthFeeMode.TOKEN) scheduleQuote(refresh = true)
    }

    init {
        populateTxItems()
        selectedTxItem.value = NoneItem()
        val presetNonce = intent.getSerializableExtra(SendCoinsActivity.TRANSACTION_NONCE) as? BigInteger
        presetNonce?.let {
            nonce.value = it
        }
        preselectReplacedTx(presetNonce)
        feeUpdater.start()
        // A replacement reuses an ETH-paid nonce slot: a UserOperation cannot replace it.
        if (feeToken != null && presetNonce == null) {
            receivingAddress.observeForever(addressObserver)
            checkTokenFeeAvailability()
        }
    }

    /**
     * Opened from a tx's "Replace transaction" menu: select that tx in the replace spinner (which
     * also sets its nonce, the bumped fee and its recipient and amount, see [applyTxPrefill]) (M-74).
     */
    private fun preselectReplacedTx(presetNonce: BigInteger?) {
        if (presetNonce == null) return
        val item = txItems.filterIsInstance<TransactionItem>()
            .firstOrNull { (it.tx as? EthTransactionSummary)?.nonce == presetNonce } ?: return
        selectedTxItem.value = item
    }

    /**
     * Prefills the recipient and amount of the tx selected for replacement ([tx] null = "none").
     * What a previous selection prefilled is taken back first, unless the user has changed it.
     * ETH account: only plain transfers and self-sends (cancellations, any gas limit) — a contract
     * call would need its calldata, which is not stored. ERC-20 account: its rows hold the token
     * recipient and amount. Zero-value txs (e.g. a cancellation) still get their recipient.
     */
    private fun applyTxPrefill(tx: EthTransactionSummary?) {
        prefilledAddress?.let {
            if (receivingAddress.value === it) {
                receivingLabel.value = ""
                receivingAddress.value = null
            }
        }
        prefilledAmount?.let {
            if (amount.value == it) amount.value = Value.zeroValue(account.coinType)
        }
        prefilledAddress = null
        prefilledAmount = null
        if (tx == null) return
        val prefill = when (account) {
            is ERC20Account -> true
            is EthAccount -> !tx.hasTokenTransfers && (tx.gasLimit <= Transfer.GAS_LIMIT ||
                    tx.receiver.addressString.equals(tx.sender.addressString, true))
            else -> false
        }
        if (!prefill || tx.value.type != account.coinType) return
        if (receivingAddress.value?.toString()?.equals(tx.receiver.toString(), true) != true) {
            // the label belongs to the previous recipient
            receivingLabel.value = ""
            receivingAddress.value = tx.receiver
            prefilledAddress = tx.receiver
        }
        if (tx.value.isPositive()) {
            amount.value = tx.value
            prefilledAmount = tx.value
        }
    }

    /** Spinner position of the preselected tx, 0 ("none") when there is none. */
    fun selectedTxPosition(): Int = selectedTxItem.value?.let { txItems.indexOf(it) }?.takeIf { it > 0 } ?: 0

    private fun checkTokenFeeAvailability() {
        val manager = aa ?: return
        scope.launch {
            // the bundler may be unreachable only for now (e.g. no network when the screen opened)
            while (!manager.isAvailable()) {
                delay(TOKEN_FEE_RECHECK_MS)
            }
            tokenFeeAvailable.postValue(true)
            // Paying in the token is what an account without ETH for gas needs — offer it upfront.
            val parent = (account as ERC20Account).ethAcc.accountBalance.spendable.value
            val ethFee = (selectedFee.value?.value ?: BigInteger.ZERO) * BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT)
            if (parent < ethFee || parent.signum() == 0) setFeeMode(EthFeeMode.TOKEN)
        }
    }

    fun setFeeMode(mode: EthFeeMode) {
        if (currentFeeMode == mode || (mode == EthFeeMode.TOKEN && feeToken == null)) return
        currentFeeMode = mode
        feeMode.postValue(mode)
        if (mode == EthFeeMode.TOKEN) {
            scheduleQuote()
        } else {
            quoteJob?.cancel()
            tokenFeeLoading.postValue(false)
            tokenFeeError.postValue("")
            setTokenFee(null)
        }
    }

    fun isTokenFeeMode() = feeToken != null && currentFeeMode == EthFeeMode.TOKEN

    /**
     * Fetches the token fee. The gas of `approve + transfer` follows from the recipient, not from
     * the amount, so a quote serves the whole entry until the recipient changes or the periodic
     * fee update asks for a fresh gas price ([refresh]).
     */
    private fun scheduleQuote(refresh: Boolean = false) {
        val manager = aa ?: return
        val token = feeToken ?: return
        val to = receivingAddress.value?.toString() ?: account.receiveAddress.toString()
        if (!refresh && quotedFor == to) {
            currentQuote?.let {
                applyQuote(it)
                return
            }
        }
        quoteJob?.cancel()
        quoteJob = scope.launch {
            delay(QUOTE_DEBOUNCE_MS)
            if (currentQuote == null) tokenFeeLoading.postValue(true)
            try {
                val quote = manager.estimateFee(account.receiveAddress.toString(), token, to,
                    quoteAmount(manager.tokenBalance(account.receiveAddress.toString(), token.contractAddress)))
                if (currentFeeMode != EthFeeMode.TOKEN) return@launch
                quotedFor = to
                tokenFeeError.postValue("")
                applyQuote(quote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentFeeMode != EthFeeMode.TOKEN) return@launch
                currentQuote = null
                quotedFor = null
                tokenFeeText.postValue("")
                tokenFeeMaxText.postValue("")
                tokenFeeError.postValue(context.getString(
                    if (isBalanceRevert(e)) R.string.eth_token_fee_insufficient else R.string.eth_token_fee_unavailable,
                    account.coinType.displaySymbol))
                txRebuildPublisher.onNext(Unit)
            } finally {
                tokenFeeLoading.postValue(false)
            }
        }
    }

    /**
     * Amount the quote simulates. Gas does not depend on it, but a transfer the chain cannot cover
     * reverts the simulation — and the local balance can be ahead of the chain (blockbook lag, a
     * "max" computed from it) — so anything beyond what either side holds is simulated as the
     * smallest unit. Non-zero on purpose: writing a non-zero recipient balance is what the real
     * transfer costs. Whether the entered amount fits is decided on send ([quoteForSend]).
     */
    private fun quoteAmount(onChainBalance: BigInteger?): BigInteger {
        val local = account.accountBalance.spendable.value
        val balance = onChainBalance?.min(local) ?: local
        val requested = enteredTokenAmount()?.value
        return if (requested == null || requested > balance) BigInteger.ONE else requested
    }

    /**
     * The entered amount in the token. It may have been typed in fiat — converted the same way
     * the tx builder converts it, otherwise a fiat-entered send is simply unpriceable.
     */
    private fun enteredTokenAmount(): Value? {
        val entered = amount.value ?: return null
        val inToken = if (entered.type == account.coinType) entered
            else mbwManager.exchangeRateManager.get(mbwManager.getWalletManager(false), entered, account.coinType)
        return inToken?.takeIf { it.type == account.coinType && it.isPositive() }
    }

    private fun applyQuote(quote: FeeQuote) {
        currentQuote = quote
        tokenFeeText.postValue(Value.valueOf(account.coinType, quote.tokenAmount).toStringWithUnit())
        tokenFeeMaxText.postValue(Value.valueOf(account.coinType, quote.maxTokenAmount).toStringWithUnit())
        setTokenFee(quote.maxTokenAmount)
    }

    /**
     * The fee travels in the tx data: it lifts the parent-ETH requirement and holds the quote's
     * maximum back from what can be sent — postOp collects the fee after the transfer ran.
     */
    private fun setTokenFee(tokenFee: BigInteger?) {
        val oldData = (transactionData.value as? EthTransactionData) ?: EthTransactionData()
        val data = EthTransactionData(oldData.nonce, oldData.gasLimit, oldData.inputData, oldData.suggestedGasPrice,
                oldData.maxPriorityFeePerGas, tokenFee)
        transactionData.postValue(data)
        // A "max" was computed against the previous fee (or the full balance) — refit it.
        if (amountIsMaxEntered) {
            val max = account.calculateMaxSpendableAmount(Value.zeroValue(account.basedOnCoinType), null, data)
            if (max.isPositive() && max != amount.value) amount.postValue(max)
        }
    }

    /** The simulated transfer reverted for lack of tokens (ERC-20 revert reason, hex-encoded). */
    private fun isBalanceRevert(e: Exception): Boolean {
        val m = e.message?.lowercase() ?: return false
        return "exceeds balance" in m || "transfer amount exceeds" in m ||
                // "ERC20: transfer amount exceeds balance" inside an ABI-encoded revert reason
                "7472616e7366657220616d6f756e7420657863656564732062616c616e6365" in m
    }

    /** Token fee shown to the user (the estimate), null without a quote. */
    fun tokenFeeValue(): Value? = currentQuote?.let { Value.valueOf(account.coinType, it.tokenAmount) }

    /** Upper bound of the fee the user was shown ("reserved up to"), null without a quote. */
    fun shownMaxTokenFee(): BigInteger? = currentQuote?.maxTokenAmount

    data class TokenFeeSend(val quote: FeeQuote, val token: FeeTokenConfig, val recipient: String, val amount: Value)

    /** Why the last [quoteForSend] returned null: the amount and fee do not fit (vs. no quote). */
    @Volatile
    var sendQuoteInsufficient = false
        private set

    /**
     * Quote to send with: fetched again for exactly this recipient and amount, since the one on
     * screen may be a fee update old. A "max" that no longer fits the re-priced fee is refitted;
     * a typed amount is the user's and is refused instead. Blocking (network) — call off the main
     * thread; null when the bundler cannot be reached or the amount and fee do not fit.
     */
    fun quoteForSend(): TokenFeeSend? {
        val manager = aa ?: return null
        val token = feeToken ?: return null
        val to = receivingAddress.value?.toString() ?: return null
        var toSend = enteredTokenAmount() ?: return null
        sendQuoteInsufficient = false
        // The local balance can be behind the chain (blockbook lag, an AA send it has not indexed
        // yet) — what the op will actually find is the lower of the two.
        val onChain = manager.tokenBalance(account.receiveAddress.toString(), token.contractAddress)
        val balance = account.accountBalance.spendable.let { local ->
            if (onChain != null && onChain < local.value) Value.valueOf(account.coinType, onChain) else local
        }
        // A max computed from a stale balance would make the simulation itself revert — fit it to
        // the fee on screen first, then to the fresh quote below.
        currentQuote?.let { fitMax(toSend, balance, it)?.let { fitted -> toSend = fitted } }
        val quote = try {
            manager.estimateFee(account.receiveAddress.toString(), token, to, toSend.value)
        } catch (e: Exception) {
            sendQuoteInsufficient = isBalanceRevert(e)
            return null
        }
        if (toSend + Value.valueOf(account.coinType, quote.maxTokenAmount) > balance) {
            toSend = fitMax(toSend, balance, quote) ?: run {
                sendQuoteInsufficient = true
                return null
            }
        }
        if (toSend != amount.value) amount.postValue(toSend)
        return TokenFeeSend(quote, token, to, toSend)
    }

    /**
     * The largest amount [balance] leaves room for next to [quote]'s maximum fee, when the
     * amount is a "max" that exceeds it; null when it is a typed amount (the user's figure is not
     * changed behind their back) or nothing is left. Unchanged [toSend] when it already fits.
     */
    private fun fitMax(toSend: Value, balance: Value, quote: FeeQuote): Value? {
        val max = balance - Value.valueOf(account.coinType, quote.maxTokenAmount)
        return when {
            toSend <= max -> toSend
            amountIsMaxEntered && max.isPositive() -> max
            else -> null
        }
    }

    override fun validateExtra(toSend: Value): TransactionStatus? {
        if (advancedInputInvalid) return TransactionStatus.MISSING_ARGUMENTS
        if (!isTokenFeeMode()) return null
        val quote = currentQuote ?: return TransactionStatus.MISSING_ARGUMENTS
        if (toSend + Value.valueOf(account.coinType, quote.maxTokenAmount) > account.accountBalance.spendable) {
            return TransactionStatus.INSUFFICIENT_FUNDS
        }
        return null
    }

    override fun updateErrorMessage(transactionStatus: TransactionStatus) {
        when {
            // the invalid Advanced field carries its own "Not valid value" error; repeating it
            // under the Amount pointed at the wrong field (R5B-04)
            transactionStatus == TransactionStatus.MISSING_ARGUMENTS && advancedInputInvalid ->
                errorText.postValue("")
            transactionStatus != TransactionStatus.OK && tokenFeeShort() ->
                errorText.postValue(context.getString(R.string.eth_token_fee_insufficient, account.coinType.displaySymbol))
            else -> super.updateErrorMessage(transactionStatus)
        }
    }

    /** Token-fee mode: the balance cannot cover the entered amount (or nothing) plus the quoted maximum fee. */
    private fun tokenFeeShort(): Boolean {
        if (!isTokenFeeMode()) return false
        val quote = currentQuote ?: return false
        val toSend = enteredTokenAmount() ?: Value.zeroValue(account.coinType)
        return toSend + Value.valueOf(account.coinType, quote.maxTokenAmount) > account.accountBalance.spendable
    }

    fun release() {
        feeUpdater.stop()
        receivingAddress.removeObserver(addressObserver)
        quoteJob?.cancel()
        scope.cancel()
    }

    private suspend fun feeUpdateRoutine() {
        withContext(Dispatchers.Main) {
            transactionStatus.value = TransactionStatus.BUILDING
        }
        delay(1000) // for more visible update effect
        mbwManager.getFeeProvider(account.basedOnCoinType).updateFeeEstimationsAsync()
        feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation
        feeUpdatePublisher.onNext(Unit)
        txRebuildPublisher.onNext(Unit)
        // the token fee follows the gas price too
        if (isTokenFeeMode()) scheduleQuote(refresh = true)
    }

    private fun populateTxItems() {
        // Only txs that can still be replaced: PENDING (not REPLACED/CANCELED/DROPPED), with a
        // nonce, and per nonce only the latest broadcast — an earlier one is superseded (M-74).
        val outgoingUnconfirmedTransactions = (account as AbstractEthERC20Account).getLivePendingTransactions().filter {
            it.nonce != null && it.sender.addressString.equals(account.receivingAddress.addressString, true)
        }.sortedByDescending { it.timestamp }.distinctBy { it.nonce }
        val items: MutableList<SpinnerItem> = mutableListOf()

        items.add(NoneItem()) // fallback item

        val dateFormat = AdaptiveDateFormat(context)
        items.addAll(outgoingUnconfirmedTransactions.map {
            val date = dateFormat.format(Date(it.timestamp * 1000L))
            val amount = it.transferred.abs().toStringWithUnit(
                    mbwManager.getDenomination(mbwManager.selectedAccount.coinType))

            TransactionItem(it, date, amount)
        })

        txItems = items
    }

    override fun handlePaymentRequest(toSend: Value): TransactionStatus {
        throw IllegalStateException("Ethereum does not support payment requests")
    }

    override fun getFeeLvlItems(): List<FeeLvlItem> {
        return MinerFee.values()
                .map { fee ->
                    val blocks = when (fee) {
                        MinerFee.LOWPRIO -> 120
                        MinerFee.ECONOMIC -> 20
                        MinerFee.NORMAL -> 8
                        MinerFee.PRIORITY -> 2
                    }
                    val duration = Utils.formatBlockcountAsApproxDuration(mbwManager, blocks, EthCoin.BLOCK_TIME_IN_SECONDS)
                    FeeLvlItem(fee, "~$duration", SelectableRecyclerView.SRVAdapter.VIEW_TYPE_ITEM)
                }
    }

    override fun estimateTxSize(): Int {
        val gl = (transactionData.value as? EthTransactionData)?.gasLimit

        return transaction?.estimatedTransactionSize
            ?: if (gl != null && gasLimitStatus.value?.isValid != false) {
                gl.toInt()
            } else if (account is EthAccount) {
                EthAccount.defaultGasLimit((transactionData.value as? EthTransactionData)?.inputData).toInt()
            } else {
                account.typicalEstimatedTransactionSize
            }
    }

    override fun getRequestedAmountFormatted(): String =
        if (amount.value == null) {
            ""
        } else if (transactionStatus.value == TransactionStatus.OUTPUT_TOO_SMALL
            || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS
            || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS_FOR_FEE
        ) {
            getValueInAccountCurrency().toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            formatValue(amount.value)
        }

    override fun formatValue(value: Value?): String =
        if (value == null) {
            ""
        } else {
            if (value.type == account.coinType) {
                value.toStringWithUnit(mbwManager.getDenomination(account.coinType))
            } else {
                "~ ${value.toStringWithUnit()}"
            }
        }
    
    /** [isValid] = false: the entered limit is not used for fee estimation (TOO_HIGH would also overflow Int). */
    enum class GasLimitStatus(val isValid: Boolean) {
        OK(true), WARNING(true), ERROR(false), TOO_HIGH(false)
    }

    companion object {
        val FEE_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(30)
        /** Lets a recipient being typed settle before the bundler is asked about it. */
        private const val QUOTE_DEBOUNCE_MS = 500L
        /** How often an unreachable bundler is asked again while the Send screen is open. */
        private const val TOKEN_FEE_RECHECK_MS = 15_000L
    }
}

class FeeUpdater(
    private val feeUpdateIntervalMs: Long,
    private val feeUpdateRoutine: suspend () -> Unit,
    private val onStop: (() -> Unit)? = null
) {
    /**
     * Seconds until the next fee refresh. Its observers are the screen's data binding, so it is
     * active only while the Send screen is visible: updates pause in the background (M-60) and
     * restart with an immediate refresh when the screen comes back.
     */
    val secondsUntilNextUpdate: MutableLiveData<Long> = object : MutableLiveData<Long>(feeUpdateIntervalMs / 1000L) {
        override fun onActive() = setVisible(true)
        override fun onInactive() = setVisible(false)
    }
    private var updateSecondsTimer: Job? = null
    private var feeUpdateScheduledTask: Timer? = null
    /** Requested by the model ([start]/[stop]), e.g. off while a manual gas price is set. */
    private var wanted = false
    private var visible = true
    /** Bumped on every halt: an in-flight update of an older run must not restart the countdown. */
    private var generation = 0

    @Volatile
    var hasStarted = false
        private set

    @Synchronized
    fun start() {
        wanted = true
        applyState()
    }

    @Synchronized
    fun stop() {
        wanted = false
        applyState()
    }

    @Synchronized
    private fun setVisible(isVisible: Boolean) {
        visible = isVisible
        applyState()
    }

    private fun applyState() {
        if (wanted && visible) startRunning() else halt()
    }

    private fun startRunning() {
        if (hasStarted) return
        hasStarted = true
        val gen = ++generation
        feeUpdateScheduledTask = Timer().apply {
            // Initial delay 0: fetch fees immediately on entry rather than only
            // after the first interval. Otherwise the cached estimate is shown
            // for the first interval and — with EIP-1559 — the fee snapshot is
            // empty, so a send in that window silently falls back to legacy.
            scheduleAtFixedRate(timerTask {
                synchronized(this@FeeUpdater) {
                    if (gen != generation) return@timerTask
                    stopUpdateSecondsTimer()
                }
                try {
                    runBlocking { feeUpdateRoutine() }
                } catch (e: Throwable) {
                    // keep the schedule alive (an exception would kill the Timer thread); the next tick retries
                }
                synchronized(this@FeeUpdater) {
                    if (gen == generation && hasStarted) startUpdateSecondsTimer()
                }
            }, 0L, feeUpdateIntervalMs)
        }
    }

    private fun halt() {
        if (!hasStarted) return
        hasStarted = false
        generation++
        feeUpdateScheduledTask?.cancel()
        feeUpdateScheduledTask = null
        stopUpdateSecondsTimer()
        onStop?.invoke()
    }

    private fun startUpdateSecondsTimer() {
        updateSecondsTimer?.cancel()
        val total = feeUpdateIntervalMs / 1000L
        // counts from the start of this countdown, not from the LiveData value (postValue is async)
        updateSecondsTimer = startCoroutineTimer(CoroutineScope(Dispatchers.Default + SupervisorJob()), 0, 1000) { counter ->
            secondsUntilNextUpdate.postValue(maxOf(0L, total - counter))
        }
    }

    private fun stopUpdateSecondsTimer() {
        updateSecondsTimer?.cancel()
        updateSecondsTimer = null
        secondsUntilNextUpdate.postValue(feeUpdateIntervalMs / 1000L)
    }
}
