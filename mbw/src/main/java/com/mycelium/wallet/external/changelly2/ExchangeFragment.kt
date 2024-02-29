package com.mycelium.wallet.external.changelly2

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.view.RingDrawable
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.event.BackHandler
import com.mycelium.wallet.activity.modern.event.BackListener
import com.mycelium.wallet.activity.modern.event.SelectTab
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.activity.util.resizeTextView
import com.mycelium.wallet.activity.util.startCursor
import com.mycelium.wallet.activity.util.stopCursor
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding
import com.mycelium.wallet.event.*
import com.mycelium.wallet.external.changelly.model.ChangellyResponse
import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import com.squareup.otto.Subscribe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit


class ExchangeFragment : Fragment(), BackListener {

    var binding: FragmentChangelly2ExchangeBinding? = null
    val viewModel: ExchangeViewModel by activityViewModels()
    val pref by lazy { requireContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val manager = MbwManager.getInstance(requireContext())
        viewModel.currencies = pref.getStringSet(KEY_SUPPORT_COINS, null) ?: setOf("btc", "eth")
        viewModel.fromAccount.value = if (viewModel.isSupported(manager.selectedAccount.coinType)) {
            manager.selectedAccount
        } else {
            manager.getWalletManager(false)
                    .getAllActiveAccounts()
                    .firstOrNull { it.canSpend() && viewModel.isSupported(it.coinType) }
        }
        viewModel.toAccount.value = viewModel.getToAccountForInit()
        Changelly2Repository.supportCurrenciesFull(lifecycleScope, {
            it?.result?.first()
                    ?.filter { it.fixRateEnabled && it.enabled }
                    ?.map { it.ticker }
                    ?.toSet()?.let {
                        viewModel.currencies = it
                        pref.getStringSet(KEY_SUPPORT_COINS, it)
                    }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2ExchangeBinding.inflate(inflater).apply {
                binding = this
                vm = viewModel
                lifecycleOwner = this@ExchangeFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.sellLayout?.root?.setOnClickListener {
            binding?.buyLayout?.coinValue?.stopCursor()
            binding?.sellLayout?.coinValue?.startCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.run {
                inputTextView = binding?.sellLayout?.coinValue
                maxValue = viewModel.exchangeInfo.value?.maxFrom
                minValue = viewModel.exchangeInfo.value?.minFrom

                setEntry(viewModel.sellValue.value ?: "")
                maxDecimals = viewModel.fromCurrency.value?.friendlyDigits ?: 0
                visibility = View.VISIBLE

                lifecycleScope.launch(Dispatchers.IO) {
                    val feeEstimation = viewModel.mbwManager
                        .getFeeProvider(viewModel.fromAccount.value!!.basedOnCoinType).estimation
                    val maxSpendable = try {
                        viewModel.fromAccount.value
                            ?.calculateMaxSpendableAmount(feeEstimation.normal, null, null)
                    } catch (ignored: Exception) {
                        null
                    }

                    withContext(Dispatchers.Main) {
                        spendableValue = maxSpendable?.valueAsBigDecimal
                    }
                }
            }
            viewModel.keyboardActive.value = true
        }
        val selectSellAccount = { _: View ->
            binding?.layoutValueKeyboard?.numericKeyboard?.done()
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_SELL)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_SELL)
        }
        binding?.sellLayout?.coinSymbol?.setOnClickListener(selectSellAccount)
        binding?.sellLayout?.layoutAccount?.setOnClickListener(selectSellAccount)
        binding?.buyLayout?.root?.setOnClickListener {
            binding?.sellLayout?.coinValue?.stopCursor()
            binding?.buyLayout?.coinValue?.startCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.run {
                inputTextView = binding?.buyLayout?.coinValue
                maxValue = viewModel.exchangeInfo.value?.maxTo
                minValue = viewModel.exchangeInfo.value?.minTo
                spendableValue = null
                setEntry(viewModel.buyValue.value ?: "")
                maxDecimals = viewModel.toCurrency.value?.friendlyDigits ?: 0
                visibility = View.VISIBLE
            }
            viewModel.keyboardActive.value = true
        }
        val selectBuyAccount = { _: View ->
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_BUY)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_BUY)
        }
        binding?.buyLayout?.coinSymbol?.setOnClickListener(selectBuyAccount)
        binding?.buyLayout?.layoutAccount?.setOnClickListener(selectBuyAccount)
        viewModel.sellValue.observe(viewLifecycleOwner) { amount ->
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView != binding?.buyLayout?.coinValue) {
                updateAmountIfChanged()
                computeBuyValue()
            }
            if(binding?.sellLayout?.coinValue?.text?.toString() != amount) {
                binding?.sellLayout?.coinValue?.setText(amount)
            }
            binding?.sellLayout?.coinValue?.resizeTextView()
        }
        viewModel.buyValue.observe(viewLifecycleOwner) { amount ->
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.buyLayout?.coinValue) {
                viewModel.sellValue.value = if (amount?.isNotEmpty() == true) {
                    try {
                        val friendlyDigits = viewModel.fromCurrency.value?.friendlyDigits
                        val exchangeInfoResult = viewModel.exchangeInfo.value?.result
                        if (friendlyDigits == null || exchangeInfoResult == null) N_A
                        else amount.toBigDecimal().setScale(friendlyDigits, RoundingMode.HALF_UP)
                                ?.div(viewModel.exchangeInfo.value!!.getExpectedValue())
                                ?.stripTrailingZeros()
                                ?.toPlainString() ?: N_A
                    } catch (e: NumberFormatException) {
                        N_A
                    }
                } else {
                    null
                }
            }
            if(binding?.buyLayout?.coinValue?.text?.toString() != amount) {
                binding?.buyLayout?.coinValue?.setText(amount)
            }
            binding?.buyLayout?.coinValue?.resizeTextView()
        }
        binding?.swapAccount?.setOnClickListener {
            binding?.layoutValueKeyboard?.numericKeyboard?.done()
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = null
            val oldFrom = viewModel.fromAccount.value
            val oldTo = viewModel.toAccount.value
            val oldBuy = viewModel.buyValue.value
            viewModel.toAccount.value = null
            viewModel.fromAccount.value = oldTo
            viewModel.toAccount.value = oldFrom
            viewModel.sellValue.value = oldBuy
            viewModel.swapEnableDelay.value = true
            it.postDelayed({ viewModel.swapEnableDelay.value = false }, 1000) //avoid recalculation values gap
            val animation = RotateAnimation(0f, if ((viewModel.swapDirection++) % 2 == 0) 180f else -180f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                    .apply {
                        interpolator = OvershootInterpolator(1f)
                        duration = 500
                        fillAfter = true
                    }
            it.startAnimation(animation)
        }
        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            inputListener = object : ValueKeyboard.SimpleInputListener() {
                override fun done() {
                    binding?.sellLayout?.coinValue?.stopCursor()
                    binding?.buyLayout?.coinValue?.stopCursor()
                    viewModel.keyboardActive.value = false
                }
            }
            errorListener = object : ValueKeyboard.ErrorListener {
                override fun maxError(maxValue: BigDecimal) {
                    viewModel.errorKeyboard.value = resources.getString(R.string.exchange_max_msg,
                            viewModel.exchangeInfo.value?.maxFrom?.stripTrailingZeros()?.toPlainString(),
                            viewModel.exchangeInfo.value?.from?.toUpperCase())
                }

                override fun minError(minValue: BigDecimal) {
                    viewModel.errorKeyboard.value = resources.getString(R.string.exchange_min_msg,
                            viewModel.exchangeInfo.value?.minFrom?.stripTrailingZeros()?.toPlainString(),
                            viewModel.exchangeInfo.value?.from?.toUpperCase())
                }

                override fun formatError() {
                    viewModel.errorKeyboard.value = ""
                }

                override fun noError() {
                    viewModel.errorKeyboard.value = ""
                }
            }
            setMaxText(getString(R.string.max), 14f)
            setPasteVisibility(false)
            visibility = View.GONE
        }
        binding?.error?.setOnClickListener {
            val account = viewModel.fromAccount.value
            if (account is ERC20Account && viewModel.error.value?.contains(ExchangeViewModel.TAG_ETH_TOP_UP) == true) {
                viewModel.mbwManager.setSelectedAccount(account.ethAcc.id)
                MbwManager.getEventBus().post(SelectTab(ModernMain.TAB_BALANCE))
            }
        }
        binding?.exchangeButton?.setOnClickListener {
            loader(true)
            Changelly2Repository.createFixTransaction(lifecycleScope,
                    viewModel.exchangeInfo.value?.id!!,
                    Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
                    Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
                    viewModel.sellValue.value!!,
                    viewModel.toAddress.value!!,
                    viewModel.fromAddress.value!!,
                    { result ->
                        if (result?.result?.isNotEmpty() == true) {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                                val unsignedTx = prepareTx(
                                        if (BuildConfig.FLAVOR == "btctestnet")
                                            viewModel.fromAddress.value!!
                                        else
                                            result.result!!.first().payinAddress!!,
                                        result.result!!.first().amountExpectedFrom.toPlainString())
                                if(unsignedTx != null) {
                                    launch(Dispatchers.Main) {
                                        loader(false)
                                        acceptDialog(unsignedTx, result) {
                                            sendTx(result.result!!.first().id!!, unsignedTx)
                                        }
                                    }
                                }
                            }
                        } else {
                            loader(false)
                            AlertDialog.Builder(requireContext())
                                    .setMessage(if (result?.error?.message?.startsWith("rateId was expired") == true)
                                        getString(R.string.changelly_error_rate_expired)
                                    else result?.error?.message)
                                    .setPositiveButton(R.string.button_ok, null)
                                    .setOnDismissListener { updateAmount() }
                                    .show()
                        }
                    },
                    { _, msg ->
                        loader(false)
                        AlertDialog.Builder(requireContext())
                                .setMessage(msg)
                                .setPositiveButton(R.string.button_ok, null)
                                .setOnDismissListener { updateAmount() }
                                .show()
                    })
        }
        viewModel.fromCurrency.observe(viewLifecycleOwner) { coin ->
            binding?.sellLayout?.coinIcon?.let {
                Glide.with(it).clear(it)
                Glide.with(it)
                        .load(iconPath(coin))
                        .apply(RequestOptions().transforms(CircleCrop()))
                        .into(it)
            }
            updateExchangeRate()
        }
        viewModel.toCurrency.observe(viewLifecycleOwner) { coin ->
            binding?.buyLayout?.coinIcon?.let {
                Glide.with(it).clear(it)
                Glide.with(it)
                        .load(iconPath(coin))
                        .apply(RequestOptions().transforms(CircleCrop()))
                        .into(it)
            }
            updateExchangeRate()
        }
        updateAmount()
        viewModel.rateLoading.observe(viewLifecycleOwner) {
            if (it) {
                counterJob?.cancel()
                binding?.progress?.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_progress, null))
                binding?.progress?.startAnimation(RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                        .apply {
                            interpolator = LinearInterpolator()
                            repeatCount = Animation.INFINITE
                            duration = 700
                        })
            } else {
                binding?.progress?.setImageDrawable(null)
                binding?.progress?.clearAnimation()
            }
        }
        viewModel.exchangeInfo.observe(viewLifecycleOwner) {
            computeBuyValue()
        }
        binding?.buyLayout?.coinValue?.doOnTextChanged { text, start, before, count ->
            viewModel.buyValue.value = binding?.buyLayout?.coinValue?.text?.toString()
        }
        binding?.sellLayout?.coinValue?.doOnTextChanged { text, start, before, count ->
            viewModel.sellValue.value = binding?.sellLayout?.coinValue?.text?.toString()
        }
        binding?.policyTerms?.setOnClickListener {
            openLink(CHANGELLY_TERM_OF_USER)
        }
    }

    private fun computeBuyValue() {
        val amount = viewModel.sellValue.value
        viewModel.buyValue.value = if (amount?.isNotEmpty() == true
                && viewModel.exchangeInfo.value?.result != null) {
            try {
                (amount.toBigDecimal() * viewModel.exchangeInfo.value?.getExpectedValue()!!)
                        .setScale(viewModel.toCurrency.value?.friendlyDigits!!, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString()
            } catch (e: NumberFormatException) {
                "N/A"
            }
        } else {
            null
        }
    }

    private fun acceptDialog(unsignedTx: Transaction?, result: ChangellyResponse<ChangellyTransactionOffer>, action: () -> Unit) {
        if (!SettingsPreference.exchangeConfirmationEnabled) {
            viewModel.mbwManager.runPinProtectedFunction(activity) {
                action()
            }?.setOnDismissListener { updateAmount() }
        } else {
            AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.exchange_accept_dialog_title))
                    .setMessage(getString(R.string.exchange_accept_dialog_msg,
                            result.result?.first()?.amountExpectedFrom?.stripTrailingZeros()?.toPlainString(),
                            result.result?.first()?.currencyFrom?.toUpperCase(),
                            unsignedTx?.totalFee()?.toStringWithUnit(),
                            result.result?.first()?.amountTo?.stripTrailingZeros()?.toPlainString(),
                            result.result?.first()?.currencyTo?.toUpperCase()))
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                        viewModel.mbwManager.runPinProtectedFunction(activity) {
                            action()
                        }?.setOnDismissListener { updateAmount() }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener { updateAmount() }
                    .show()
        }
    }

    private fun prepareTx(addressTo: String, amount: String): Transaction? =
            viewModel.fromAccount.value?.let { account ->
                val address = when (account) {
                    is EthAccount, is ERC20Account -> {
                        EthAddress(Utils.getEthCoinType(), addressTo)
                    }
                    is AbstractBtcAccount -> {
                        BtcAddress(Utils.getBtcCoinType(), BitcoinAddress.fromString(addressTo))
                    }
                    else -> TODO("Account not supported yet")
                }
                viewModel.prepateTx(address, amount)
            }

    private fun sendTx(txId: String, createTx: Transaction) {
        loader(true)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            viewModel.fromAccount.value?.let { account ->
                account.signTx(createTx, AesKeyCipher.defaultKeyCipher())
                launch(Dispatchers.Main) {
                    loader(false)
                    viewModel.changellyTx = txId
                    BroadcastDialog.create(account, false, createTx)
                            .show(parentFragmentManager, "broadcast_tx")
                }
            }
        }
    }

    private var rateJob: Job? = null

    private fun updateExchangeRate() {
        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
            rateJob?.cancel()
            viewModel.rateLoading.value = true
            rateJob = Changelly2Repository.fixRate(lifecycleScope,
                    Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
                    Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
                    { result ->
                        if (result?.result != null) {
                            viewModel.exchangeInfo.value = result.result?.first()
                            viewModel.errorRemote.value = ""
                        } else {
                            viewModel.errorRemote.value = result?.error?.message ?: ""
                        }
                    },
                    { code, msg ->
                        if(code != 400) {
                            viewModel.errorRemote.value = msg
                        }
                    },
                    {
                        viewModel.rateLoading.value = false
                        refreshRateCounter()
                    })
        }
    }

    private var prevAmount: BigDecimal? = null

    private var amountJob: Job? = null

    private fun updateAmountIfChanged() {
        try {
            viewModel.sellValue.value?.toBigDecimal()?.let { fromAmount ->
                if (prevAmount != fromAmount && fromAmount > BigDecimal.ZERO) {
                    updateAmount()
                }
            }
        } catch (e: NumberFormatException) {
        }
    }

    private fun updateAmount() {
        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
            try {
                viewModel.sellValue.value?.toBigDecimal()?.let { fromAmount ->
                    if (fromAmount > BigDecimal.ZERO) {
                        amountJob?.cancel()
                        viewModel.rateLoading.value = true
                        amountJob = Changelly2Repository.exchangeAmount(lifecycleScope,
                                Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
                                Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
                                fromAmount,
                                { result ->
                                    result?.result?.let {
                                        viewModel.exchangeInfo.postValue(it.first())
                                        viewModel.errorRemote.value = ""
                                    } ?: run {
                                        viewModel.errorRemote.value = result?.error?.message ?: ""
                                    }
                                },
                                { code, msg ->
                                    if(code != 400) {
                                        viewModel.errorRemote.value = msg
                                    }
                                },
                                {
                                    viewModel.rateLoading.value = false
                                    refreshRateCounter()
                                })
                        prevAmount = fromAmount
                    } else {
                        updateExchangeRate()
                    }
                } ?: run {
                    updateExchangeRate()
                }
            } catch (e: NumberFormatException) {
                updateExchangeRate()
            }
        }
    }

    private var counterJob: Job? = null

    private fun refreshRateCounter() {
        counterJob?.cancel()
        counterJob = startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.SECONDS.toMillis(1)) { counter ->
            if (viewModel.rateLoading.value == false) {
                if(counter < 30) {
                    binding?.progress?.setImageDrawable(RingDrawable(counter * 360f / 30f, Color.parseColor("#777C80")))
                } else {
                    counterJob?.cancel()
                    updateAmount()
                }
            } else {
                counterJob?.cancel()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MbwManager.getEventBus().register(this)
        val pActivity = activity
        if (pActivity is BackHandler) {
            pActivity.addBackListener(this)
        }
    }

    override fun onStop() {
        val pActivity = activity
        if (pActivity is BackHandler) {
            pActivity.removeBackListener(this)
        }
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    @Subscribe
    fun broadcastResult(result: TransactionBroadcasted) {
        if (result.result.resultType == BroadcastResultType.SUCCESS && result.txid != null) {
            val fromAccountId = viewModel.fromAccount.value?.id
            val toAccountId = viewModel.toAccount.value?.id
            val history = pref.getStringSet(KEY_HISTORY, null) ?: setOf()
            val txId = viewModel.changellyTx
            val createTx = result.txid
            pref.edit().putStringSet(KEY_HISTORY, history + txId)
                    .putString("tx_id_${txId}", createTx)
                    .putString("account_from_id_${txId}", fromAccountId?.toString())
                    .putString("account_to_id_${txId}", toAccountId?.toString())
                    .apply()

            ExchangeResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ExchangeResultFragment.KEY_CHANGELLY_TX_ID, txId)
                    putString(ExchangeResultFragment.KEY_CHAIN_TX, createTx)
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_FROM_ID, fromAccountId)
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_TO_ID, toAccountId)
                }
            }.show(parentFragmentManager, "exchange_result")
            viewModel.mbwManager.getWalletManager(false).startSynchronization(viewModel.fromAccount.value?.id)
            viewModel.reset()
        }
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        if (viewModel.mbwManager.selectedAccount.canSpend()
                && viewModel.isSupported(viewModel.mbwManager.selectedAccount.coinType)) {
            viewModel.fromAccount.value = viewModel.mbwManager.selectedAccount
            viewModel.sellValue.value = ""
        }
    }

    @Subscribe
    fun exchangeRatesRefreshed(event: ExchangeRatesRefreshed) {
        viewModel.fromAccount.value = viewModel.fromAccount.value
        viewModel.toAccount.value = viewModel.toAccount.value
    }

    @Subscribe
    fun exchangeSourceChanged(event: ExchangeSourceChanged) {
        viewModel.fromAccount.value = viewModel.fromAccount.value
        viewModel.toAccount.value = viewModel.toAccount.value
    }

    @Subscribe
    fun onSelectedCurrencyChange(event: SelectedCurrencyChanged) {
        viewModel.fromAccount.value = viewModel.fromAccount.value
        viewModel.toAccount.value = viewModel.toAccount.value
    }

    @Subscribe
    fun pageSelectedEvent(event: PageSelectedEvent) {
        binding?.layoutValueKeyboard?.numericKeyboard?.done()
    }


    override fun onBackPressed(): Boolean =
            if (binding?.layoutValueKeyboard?.numericKeyboard?.visibility == View.VISIBLE) {
                binding?.layoutValueKeyboard?.numericKeyboard?.done()
                true
            } else {
                false
            }

    companion object {
        const val PREF_FILE = "changelly2"
        const val KEY_SUPPORT_COINS = "coin_support_list"
        const val KEY_HISTORY = "tx_history"
        const val TAG_SELECT_ACCOUNT_BUY = "select_account_for_buy"
        const val TAG_SELECT_ACCOUNT_SELL = "select_account_for_sell"
        const val TAG_HISTORY = "history"
        private const val N_A = "N/A"

        fun iconPath(coin: CryptoCurrency) =
                iconPath(Util.trimTestnetSymbolDecoration(coin.symbol))

        fun iconPath(coin: String) =
                Uri.parse("file:///android_asset/token-logos/" + coin.toLowerCase() + "_logo.png")

        const val CHANGELLY_TERM_OF_USER = "https://changelly.com/terms-of-use"
    }
}
