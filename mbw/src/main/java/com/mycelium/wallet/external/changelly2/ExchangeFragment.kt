package com.mycelium.wallet.external.changelly2

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.activity.send.event.BroadcastResultListener
import com.mycelium.wallet.activity.util.resizeTextView
import com.mycelium.wallet.activity.util.startCursor
import com.mycelium.wallet.activity.util.stopCursor
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.ExchangeSourceChanged
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit


class ExchangeFragment : Fragment(), BroadcastResultListener {

    var binding: FragmentChangelly2ExchangeBinding? = null
    val viewModel: ExchangeViewModel by activityViewModels()
    val pref by lazy { requireContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val manager = MbwManager.getInstance(requireContext())
        viewModel.currencies = pref.getStringSet(KEY_SUPPORT_COINS, null) ?: setOf("btc", "eth")
        viewModel.fromAccount.value = if (viewModel.currencies.contains(Util.trimTestnetSymbolDecoration(manager.selectedAccount.coinType.symbol).toLowerCase())) {
            manager.selectedAccount
        } else {
            manager.getWalletManager(false)
                    .getAllActiveAccounts()
                    .firstOrNull {
                        it.canSpend()
                                && viewModel.currencies.contains(Util.trimTestnetSymbolDecoration(it.coinType.symbol).toLowerCase())
                    }
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
                spendableValue = viewModel.fromAccount.value?.accountBalance?.spendable?.valueAsBigDecimal
                setEntry(viewModel.sellValue.value ?: "")
                maxDecimals = viewModel.fromCurrency.value?.unitExponent ?: 0
                visibility = View.VISIBLE
            }
        }
        binding?.sellLayout?.coinSymbol?.setOnClickListener {
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_SELL)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_SELL)
        }
        binding?.buyLayout?.root?.setOnClickListener {
            binding?.sellLayout?.coinValue?.stopCursor()
            binding?.buyLayout?.coinValue?.startCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.run {
                inputTextView = binding?.buyLayout?.coinValue
                maxValue = viewModel.exchangeInfo.value?.maxTo
                minValue = viewModel.exchangeInfo.value?.minTo
                spendableValue = viewModel.toAccount.value?.accountBalance?.spendable?.valueAsBigDecimal
                setEntry(viewModel.buyValue.value ?: "")
                maxDecimals = viewModel.toCurrency.value?.unitExponent ?: 0
                visibility = View.VISIBLE
            }
        }
        binding?.buyLayout?.coinSymbol?.setOnClickListener {
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_BUY)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_BUY)
        }
        viewModel.sellValue.observe(viewLifecycleOwner) {
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView != binding?.buyLayout?.coinValue) {
                try {
                    val amount = binding?.sellLayout?.coinValue?.text?.toString()?.toBigDecimal()!!
                    viewModel.buyValue.value =
                            (amount * viewModel.exchangeInfo.value?.result!!)
                                    .setScale(viewModel.toCurrency.value?.unitExponent!!, RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString()
                } catch (e: NumberFormatException) {
                    viewModel.buyValue.value = "N/A"
                }
            }
            binding?.sellLayout?.coinValue?.resizeTextView()
        }
        viewModel.buyValue.observe(viewLifecycleOwner) {
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.buyLayout?.coinValue) {
                try {
                    val amount = binding?.buyLayout?.coinValue?.text?.toString()?.toBigDecimal()
                    viewModel.sellValue.value =
                            amount?.setScale(viewModel.fromCurrency.value?.unitExponent!!, RoundingMode.HALF_UP)
                                    ?.div(viewModel.exchangeInfo.value?.result!!)
                                    ?.stripTrailingZeros()
                                    ?.toPlainString() ?: "N/A"
                } catch (e: NumberFormatException) {
                    viewModel.sellValue.value = "N/A"
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
                    viewModel.errorKeyboard.value = "The amount is more than the exchange maximum of ${viewModel.exchangeInfo.value?.maxFrom} ${viewModel.exchangeInfo.value?.from}"
                }

                override fun minError(minValue: BigDecimal) {
                    viewModel.errorKeyboard.value = "The amount is lower than the exchange minimum of ${viewModel.exchangeInfo.value?.minFrom} ${viewModel.exchangeInfo.value?.from}"
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
                            val feeEstimation = viewModel.mbwManager.getFeeProvider(viewModel.fromAccount.value!!.basedOnCoinType).estimation
                            AlertDialog.Builder(requireContext())
                                    .setTitle("Exchange")
                                    .setMessage("You send: ${result.result?.amountExpectedFrom} ${result.result?.currencyFrom?.toUpperCase()}\n" +
                                            "You get: ${result.result?.amountTo} ${result.result?.currencyTo?.toUpperCase()}\n" +
                                            "Miners fee: ${feeEstimation.normal.toStringFriendlyWithUnit()}")
                                    .setPositiveButton(R.string.button_ok) { _, _ ->
                                        sendTx(result.result!!.id!!, result.result!!.payinAddress!!, result.result!!.amountExpectedFrom!!)
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                        } else {
                            AlertDialog.Builder(requireContext())
                                    .setMessage(result?.error?.message)
                                    .setPositiveButton(R.string.button_ok, null)
                                    .show()
                        }
                    },
                    { _, msg ->
                        AlertDialog.Builder(requireContext())
                                .setMessage(msg)
                                .setPositiveButton(R.string.button_ok, null)
                                .show()
                    },
                    {
                        loader(false)
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
        startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.MINUTES.toMillis(2)) {
            updateExchangeRate()
        }
    }

    private fun sendTx(txId: String, addressTo: String, amount: String) {
        viewModel.mbwManager.runPinProtectedFunction(requireActivity()) {
            val address = when (viewModel.fromAccount.value) {
                is EthAccount, is ERC20Account -> {
                    EthAddress(Utils.getEthCoinType(), addressTo)
                }
                is AbstractBtcAccount -> {
                    BtcAddress(Utils.getBtcCoinType(), BitcoinAddress.fromString(addressTo))
                }
                else -> TODO("Account not supported yet")
            }
            loader(true)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                viewModel.fromAccount.value?.let { account ->
                    val feeEstimation = viewModel.mbwManager.getFeeProvider(account.basedOnCoinType).estimation
                    val createTx = account.createTx(address,
                            viewModel.fromAccount.value!!.coinType.value(amount),
                            FeePerKbFee(feeEstimation.normal),
                            null
                    )
                    account.signTx(createTx, AesKeyCipher.defaultKeyCipher())
                    loader(false)
                    viewModel.changellyTx = txId
                    viewModel.chainTx = createTx
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
            rateJob = Changelly2Repository.fixRate(lifecycleScope,
                    Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
                    Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
                    { result ->
                        if (result?.result != null) {
                            viewModel.exchangeInfo.value = result.result
                            if (binding?.sellLayout?.coinValue?.text?.isEmpty() != false) {
                                binding?.sellLayout?.coinValue?.text = result.result?.minFrom
                                        ?.stripTrailingZeros()
                                        ?.toPlainString()
                            }
                            viewModel.errorRemote.value = ""
                        } else {
                            viewModel.errorRemote.value = result?.error?.message ?: ""
                        }
                    },
                    { _, _ ->

                    })
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
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun broadcastResult(broadcastResult: BroadcastResult) {
        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            val fromAccountId = viewModel.fromAccount.value?.id
            val toAccountId = viewModel.toAccount.value?.id
            val history = pref.getStringSet(KEY_HISTORY, null) ?: setOf()
            val txId = viewModel.changellyTx
            val createTx = viewModel.chainTx
            pref.edit().putStringSet(KEY_HISTORY, history + txId)
                    .putString("tx_id_${txId}", HexUtils.toHex(createTx?.id))
                    .putString("account_from_id_${txId}", fromAccountId?.toString())
                    .putString("account_to_id_${txId}", toAccountId?.toString())
                    .apply()

            ExchangeResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ExchangeResultFragment.KEY_TX_ID, txId)
                    putString(ExchangeResultFragment.KEY_CHAIN_TX, HexUtils.toHex(createTx?.id))
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_FROM_ID, fromAccountId)
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_TO_ID, toAccountId)
                }
            }.show(parentFragmentManager, "exchange_result")
        }
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        if (viewModel.mbwManager.selectedAccount.canSpend()) {
            viewModel.fromAccount.value = viewModel.mbwManager.selectedAccount
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
                "https://web-api.changelly.com/api/coins/${Util.trimTestnetSymbolDecoration(coin.symbol).toLowerCase()}.png"
    }
}