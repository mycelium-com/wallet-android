package com.mycelium.wallet.external.changelly2

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
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
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.AesKeyCipher
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
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.util.concurrent.TimeUnit


class ExchangeFragment : Fragment() {

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
        viewModel.toAccount.value = getToAccount()
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
            binding?.sellLayout?.coinValue?.startCursor()
            binding?.buyLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.maxDecimals =
                    viewModel.fromCurrency.value?.unitExponent ?: 0
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.sellLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
            binding?.layoutValueKeyboard?.numericKeyboard?.maxValue =
                    viewModel.exchangeInfo.value?.maxFrom
            binding?.layoutValueKeyboard?.numericKeyboard?.minValue =
                    viewModel.exchangeInfo.value?.minFrom
            binding?.layoutValueKeyboard?.numericKeyboard?.spendableValue =
                    viewModel.fromAccount.value?.accountBalance?.spendable?.valueAsBigDecimal
            binding?.layoutValueKeyboard?.numericKeyboard?.setEntry(viewModel.sellValue.value ?: "")
        }
        binding?.sellLayout?.coinSymbol?.setOnClickListener {
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_SELL)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_SELL)
        }
        binding?.buyLayout?.root?.setOnClickListener {
            binding?.buyLayout?.coinValue?.startCursor()
            binding?.sellLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.maxDecimals =
                    viewModel.toCurrency.value?.unitExponent ?: 0
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.buyLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
            binding?.layoutValueKeyboard?.numericKeyboard?.maxValue =
                    viewModel.exchangeInfo.value?.maxTo
            binding?.layoutValueKeyboard?.numericKeyboard?.minValue =
                    viewModel.exchangeInfo.value?.minTo
            binding?.layoutValueKeyboard?.numericKeyboard?.spendableValue =
                    viewModel.toAccount.value?.accountBalance?.spendable?.valueAsBigDecimal
            binding?.layoutValueKeyboard?.numericKeyboard?.setEntry(viewModel.buyValue.value ?: "")
        }
        binding?.buyLayout?.coinSymbol?.setOnClickListener {
            SelectAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(SelectAccountFragment.KEY_TYPE, SelectAccountFragment.VALUE_BUY)
                }
            }.show(parentFragmentManager, TAG_SELECT_ACCOUNT_BUY)
        }
        binding?.sellLayout?.coinValue?.doAfterTextChanged {
            viewModel.sellValue.value = binding?.sellLayout?.coinValue?.text?.toString()
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.sellLayout?.coinValue) {
                viewModel.error.value = ""
                try {
                    val amount = binding?.sellLayout?.coinValue?.text?.toString()?.toBigDecimal()!!
                    binding?.buyLayout?.coinValue?.text =
                            (amount * viewModel.exchangeInfo.value?.result!!)
                                    .setScale(viewModel.toCurrency.value?.unitExponent!!, RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString()
                } catch (e: NumberFormatException) {
                    binding?.buyLayout?.coinValue?.text = "N/A"
                }
            }
            binding?.sellLayout?.coinValue?.resizeTextView()
        }
        binding?.buyLayout?.coinValue?.doAfterTextChanged {
            viewModel.buyValue.value = binding?.buyLayout?.coinValue?.text?.toString()
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.buyLayout?.coinValue) {
                viewModel.error.value = ""
                try {
                    val amount = binding?.buyLayout?.coinValue?.text?.toString()?.toBigDecimal()
                    binding?.sellLayout?.coinValue?.text =
                            amount?.div(viewModel.exchangeInfo.value?.result!!)
                                    ?.setScale(viewModel.fromCurrency.value?.unitExponent!!, RoundingMode.HALF_UP)
                                    ?.stripTrailingZeros()
                                    ?.toPlainString() ?: "N/A"
                } catch (e: NumberFormatException) {
                    binding?.sellLayout?.coinValue?.text = "N/A"
                }
            }
            binding?.buyLayout?.coinValue?.resizeTextView()
        }
        binding?.swapAccount?.setOnClickListener {
            binding?.layoutValueKeyboard?.numericKeyboard?.done()
            val newFrom = viewModel.fromAccount.value
            val newTo = viewModel.toAccount.value
            viewModel.fromAccount.value = newTo
            viewModel.toAccount.value = newFrom
            binding?.sellLayout?.coinValue?.text = viewModel.buyValue.value
        }
        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            inputListener = object : ValueKeyboard.SimpleInputListener() {
                override fun done() {
                    binding?.sellLayout?.coinValue?.stopCursor()
                    binding?.buyLayout?.coinValue?.stopCursor()
                }
            }
            errorMaxListener = {
                viewModel.error.value = "The amount is more than the exchange maximum of ${viewModel.exchangeInfo.value?.maxFrom} ${viewModel.exchangeInfo.value?.from}"
            }
            errorMinListener = {
                viewModel.error.value = "The amount is lower than the exchange minimum of ${viewModel.exchangeInfo.value?.minFrom} ${viewModel.exchangeInfo.value?.from}"
            }
            setMaxText(getString(R.string.max), 14f)
            setPasteVisibility(View.GONE)
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
                            AlertDialog.Builder(requireContext())
                                    .setTitle("Exchange")
                                    .setMessage("You send: ${result.result?.amountExpectedFrom} ${result.result?.currencyFrom}\n" +
                                            "You get: ${result.result?.amountTo} ${result.result?.currencyTo}\n" +
                                            "Miners fee: ${viewModel.feeEstimation.value!!.normal.toStringFriendlyWithUnit()}")
                                    .setPositiveButton(R.string.button_ok) { _, _ ->
                                        sendTx(result.result!!.id!!, result.result!!.payinAddress!!, result.result!!.amountExpectedFrom!!)
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                        } else {
                            viewModel.error.value = result?.error?.message
                        }
                    },
                    { _, msg ->
                        viewModel.error.value = msg
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
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_TERMS)))
            } catch (e: ActivityNotFoundException) {
                Toaster(this).toast("cant open $LINK_TERMS", true)
            }
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
                    val createTx = account.createTx(address,
                            viewModel.fromAccount.value!!.coinType.value(amount),
                            FeePerKbFee(viewModel.feeEstimation.value!!.normal),
                            null
                    )
                    account.signTx(createTx, AesKeyCipher.defaultKeyCipher())
                    val broadcastResult = account.broadcastTx(createTx)
                    if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
                        val history = pref.getStringSet(KEY_HISTORY, null) ?: setOf()
                        pref.edit().putStringSet(KEY_HISTORY, history + txId).apply()
                    }
                    launch(Dispatchers.Main) {
                        loader(false)
                        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
                            ExchangeResultFragment().apply {
                                arguments = Bundle().apply {
                                    putString(ExchangeResultFragment.KEY_TX_ID, txId)
                                }
                            }.show(parentFragmentManager, "exchange_result")
                        } else {
                            //TODO handle broadcast error
                        }
                    }
                }
            }
        }
    }

    private fun updateExchangeRate() {
        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
            Changelly2Repository.fixRate(lifecycleScope,
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
                            viewModel.error.value = ""
                        } else {
                            viewModel.error.value = result?.error?.message ?: ""
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

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        if (viewModel.mbwManager.selectedAccount.canSpend()) {
            viewModel.fromAccount.value = viewModel.mbwManager.selectedAccount
        }
        if (viewModel.toAccount == viewModel.fromAccount) {
            viewModel.toAccount.value = getToAccount()
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

    fun getToAccount() = viewModel.mbwManager.getWalletManager(false)
            .getAllActiveAccounts()
            .firstOrNull() {
                it.coinType != viewModel.fromAccount.value?.coinType
                        && viewModel.currencies.contains(Util.trimTestnetSymbolDecoration(it.coinType.symbol).toLowerCase())
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