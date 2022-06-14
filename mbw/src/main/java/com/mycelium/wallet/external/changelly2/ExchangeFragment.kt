package com.mycelium.wallet.external.changelly2

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.view.RingDrawable
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.modern.event.BackHandler
import com.mycelium.wallet.activity.modern.event.BackListener
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.activity.util.resizeTextView
import com.mycelium.wallet.activity.util.startCursor
import com.mycelium.wallet.activity.util.stopCursor
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding
import com.mycelium.wallet.event.*
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.FeePerKbFee
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
        viewModel.toAccount.value = viewModel.getToAccount()
        Changelly2Repository.supportCurrenciesFull(lifecycleScope, {
            it?.result
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
                    val feeEstimation = viewModel.mbwManager.getFeeProvider(viewModel.fromAccount.value!!.basedOnCoinType).estimation
                    val maxSpendable = viewModel.fromAccount.value?.calculateMaxSpendableAmount(feeEstimation.normal, null)
                    withContext(Dispatchers.Main) {
                        spendableValue = maxSpendable?.valueAsBigDecimal
                    }
                }
            }
        }
        val selectSellAccount = { view: View ->
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
        }
        val selectBuyAccount = { view:View ->
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_BUY)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_BUY)
        }
        binding?.buyLayout?.coinSymbol?.setOnClickListener(selectBuyAccount)
        binding?.buyLayout?.layoutAccount?.setOnClickListener(selectBuyAccount)
        viewModel.sellValue.observe(viewLifecycleOwner) {
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView != binding?.buyLayout?.coinValue) {
                updateAmount()
                viewModel.buyValue.value = try {
                    val amount = binding?.sellLayout?.coinValue?.text?.toString()?.toBigDecimal()!!
                    (amount * viewModel.exchangeInfo.value?.result!!)
                            .setScale(viewModel.toCurrency.value?.friendlyDigits!!, RoundingMode.HALF_UP)
                            .stripTrailingZeros()
                            .toPlainString()
                } catch (e: NumberFormatException) {
                    "N/A"
                }
            }
            binding?.sellLayout?.coinValue?.resizeTextView()
        }
        viewModel.buyValue.observe(viewLifecycleOwner) {
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.buyLayout?.coinValue) {
                viewModel.sellValue.value = try {
                    val amount = binding?.buyLayout?.coinValue?.text?.toString()?.toBigDecimal()
                    amount?.setScale(viewModel.fromCurrency.value?.friendlyDigits!!, RoundingMode.HALF_UP)
                            ?.div(viewModel.exchangeInfo.value?.result!!)
                            ?.stripTrailingZeros()
                            ?.toPlainString() ?: "N/A"
                } catch (e: NumberFormatException) {
                    "N/A"
                }
            }
            binding?.buyLayout?.coinValue?.resizeTextView()
        }
        binding?.swapAccount?.setOnClickListener {
            binding?.layoutValueKeyboard?.numericKeyboard?.done()
            val oldFrom = viewModel.fromAccount.value
            val oldTo = viewModel.toAccount.value
            val oldBuy = viewModel.buyValue.value
            viewModel.toAccount.value = null
            viewModel.fromAccount.value = oldTo
            viewModel.toAccount.value = oldFrom
            viewModel.sellValue.value = oldBuy
        }
        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            inputListener = object : ValueKeyboard.SimpleInputListener() {
                override fun done() {
                    binding?.sellLayout?.coinValue?.stopCursor()
                    binding?.buyLayout?.coinValue?.stopCursor()
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
                        if (result?.result != null) {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                                val unsignedTx = prepareTx(
                                        if (BuildConfig.FLAVOR == "btctestnet")
                                            viewModel.fromAddress.value!!
                                        else
                                            result.result!!.payinAddress!!,
                                        result.result!!.amountExpectedFrom!!)
                                launch(Dispatchers.Main) {
                                    loader(false)
                                    AlertDialog.Builder(requireContext())
                                            .setTitle("Exchange")
                                            .setMessage("You send: ${result.result?.amountExpectedFrom} ${result.result?.currencyFrom?.toUpperCase()}\n" +
                                                    "You get: ${result.result?.amountTo} ${result.result?.currencyTo?.toUpperCase()}\n" +
                                                    "Miners fee: ${unsignedTx?.totalFee()?.toStringWithUnit()}")
                                            .setPositiveButton(R.string.button_ok) { _, _ ->
                                                sendTx(result.result!!.id!!, unsignedTx!!)
                                            }
                                            .setNegativeButton(R.string.cancel, null)
                                            .show()
                                }
                            }
                        } else {
                            loader(false)
                            AlertDialog.Builder(requireContext())
                                    .setMessage(result?.error?.message)
                                    .setPositiveButton(R.string.button_ok, null)
                                    .show()
                        }
                    },
                    { _, msg ->
                        loader(false)
                        AlertDialog.Builder(requireContext())
                                .setMessage(msg)
                                .setPositiveButton(R.string.button_ok, null)
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
        binding?.policyTerms?.setOnClickListener {
            openLink(LINK_TERMS)
        }
        startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.SECONDS.toMillis(30)) {
            updateExchangeRate()
        }
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
                val feeEstimation = viewModel.mbwManager.getFeeProvider(account.basedOnCoinType).estimation
                account.createTx(address,
                        viewModel.fromAccount.value!!.coinType.value(amount),
                        FeePerKbFee(feeEstimation.normal),
                        null
                )
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
                            refreshRateCounter()
                            viewModel.exchangeInfo.value = result.result
                            if (viewModel.sellValue.value?.isEmpty() != false) {
                                viewModel.sellValue.value = result.result?.minFrom
                                        ?.stripTrailingZeros()
                                        ?.toPlainString()
                            }
                            viewModel.errorRemote.value = ""
                        } else {
                            viewModel.errorRemote.value = result?.error?.message ?: ""
                        }
                    },
                    { _, msg ->
                        viewModel.errorRemote.value = msg
                    },
                    {
                        viewModel.rateLoading.value = false
                    })
        }
    }

    private fun updateAmount() {
        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
            viewModel.sellValue.value?.toBigDecimal()?.let { fromAmount ->
                rateJob?.cancel()
                rateJob = Changelly2Repository.exchangeAmount(lifecycleScope,
                        Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
                        Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
                        fromAmount,
                        { result ->
                            if (result?.result != null) {
                                refreshRateCounter()
                                val info = viewModel.exchangeInfo.value
                                info?.result = result.result!!.rate
                                viewModel.exchangeInfo.postValue(info)
                                viewModel.errorRemote.value = ""
                            } else {
                                viewModel.errorRemote.value = result?.error?.message ?: ""
                            }
                        },
                        { _, msg ->
                            viewModel.errorRemote.value = msg
                        })
            }
        }
    }

    var counterJob:Job? = null

    private fun refreshRateCounter() {
        counterJob?.cancel()
        var counter = 0
        counterJob = startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.SECONDS.toMillis(1)) {
            if(viewModel.rateLoading.value == false ) {
                binding?.progress?.setImageDrawable(RingDrawable(counter++ * 360f / 30f, Color.parseColor("#777C80")))
            } else {
                counterJob?.cancel()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.exchange_changelly2, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.history -> {
                    HistoryFragment().show(parentFragmentManager, TAG_HISTORY)
                    true
                }
                else -> super.onOptionsItemSelected(item)
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
        if (result.result.resultType == BroadcastResultType.SUCCESS) {
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

        const val LINK_TERMS = "https://changelly.com/terms-of-use"
        const val LINK_AML = "https://changelly.com/aml-kyc"

        fun iconPath(coin: CryptoCurrency) =
                iconPath(Util.trimTestnetSymbolDecoration(coin.symbol))

        fun iconPath(coin: String) =
                Uri.parse("file:///android_asset/token-logos/" + coin.toLowerCase() + "_logo.png")
    }
}