package com.mycelium.bequant.market

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BQExchangeRateManager
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.BequantConstants.REQUEST_CODE_EXCHANGE_COINS
import com.mycelium.bequant.common.*
import com.mycelium.bequant.exchange.SelectCoinActivity
import com.mycelium.bequant.kyc.BequantKycActivity
import com.mycelium.bequant.market.viewmodel.ExchangeViewModel
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Symbol
import com.mycelium.bequant.remote.trading.model.Transaction
import com.mycelium.bequant.sign.SignActivity
import com.mycelium.bequant.signup.TwoFactorActivity
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.DialogBequantExchangeSummaryBinding
import com.mycelium.wallet.databinding.FragmentBequantExchangeBinding
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.squareup.otto.Subscribe
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.pow


class ExchangeFragment : Fragment() {
    private val viewModel: ExchangeViewModel by viewModels()
    private var isDemo = false
    var getViewActive = false
    var binding: FragmentBequantExchangeBinding? = null

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            Api.publicRepository.publicCurrencyGet(viewLifecycleOwner.lifecycle.coroutineScope, null, {
                val currencies = it?.toList() ?: listOf()
                currencies.find { it.id == intent?.getStringExtra("from") }?.assetInfoById()?.let {
                    viewModel.youSend.value = Value.zeroValue(it)
                }
                currencies.find { it.id == intent?.getStringExtra("to") }?.assetInfoById()?.let {
                    viewModel.youGet.value = Value.zeroValue(it)
                }
                updateAvailable()
                // check RadioButton with default "100%" value
                binding?.sendPercent?.check(binding!!.sendPercent.findViewWithTag<RadioButton>(100).id)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BQExchangeRateManager.requestOptionalRefresh()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, IntentFilter(BequantConstants.ACTION_EXCHANGE))
        MbwManager.getEventBus().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantExchangeBinding.inflate(inflater,  container, false)
                    .apply {
                        binding = this
                        viewModel = this@ExchangeFragment.viewModel
                        lifecycleOwner = this@ExchangeFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDemo = activity?.intent?.getBooleanExtra(BequantMarketActivity.IS_DEMO_KEY, false)!!
        createPercentageRadioButtons()
        binding?.sendView?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                getViewActive = false
            }
        }
        binding?.getView?.setOnFocusChangeListener { _, hasFocus ->
            getViewActive = hasFocus
        }
        viewModel.youSend.observe(viewLifecycleOwner, Observer {
            try {
                if (!equals(it, viewModel.youSendText.value)) {
                    viewModel.youSendText.value = it.toString(Denomination.UNIT)
                }
            } catch (e: NumberFormatException) {
                viewModel.youSendText.value = it.toString(Denomination.UNIT)
            }
            // calculate receive value if receive view not active
            if (!getViewActive) {
                calculateReceiveValue()
            }
            recalculateDestinationPrice()
            viewModel.isEnoughFundsIncludingFees.value = isEnoughFundsIncludingFees()
            updateExchangeEnabledFlag()
        })
        viewModel.youSendText.observe(viewLifecycleOwner, Observer {
            try {
                if (!equals(viewModel.youSend.value, it)) {
                    viewModel.youSend.value = viewModel.youSend.value?.type?.value(it)
                }
            } catch (e: NumberFormatException) {
            }
        })
        viewModel.youGet.observe(viewLifecycleOwner, Observer {
            try {
                if (!equals(it, viewModel.youGetText.value)) {
                    viewModel.youGetText.value = it.toString(Denomination.UNIT)
                }
            } catch (e: NumberFormatException) {
                viewModel.youGetText.value = it?.toString(Denomination.UNIT)
            }
            // calculate send value only if receive view active
            if (getViewActive) {
                calculateSendValue()
            }
            recalculateDestinationPrice()
            viewModel.isEnoughFundsIncludingFees.value = isEnoughFundsIncludingFees()
            updateExchangeEnabledFlag()
        })
        viewModel.youGetText.observe(viewLifecycleOwner, Observer {
            try {
                if (!equals(viewModel.youGet.value, it)) {
                    viewModel.youGet.value = viewModel.youGet.value?.type?.value(it)
                }
            } catch (e: NumberFormatException) {
            }
        })
        viewModel.accountBalances.observe(viewLifecycleOwner, Observer {
            updateAvailable()
        })
        viewModel.tradingBalances.observe(viewLifecycleOwner, Observer {
            updateAvailable()
        })
        viewModel.available.observe(viewLifecycleOwner, Observer {
            updateYouSend(100)
        })
        binding?.sendSymbolLayout?.setOnClickListener {
            val youSendYouGetPair = Pair(viewModel.youSend.value!!.type, viewModel.youGet.value!!.type)
            startActivityForResult(Intent(requireContext(), SelectCoinActivity::class.java)
                    .putExtra(PARENT, YOU_SEND)
                    .putExtra(YOU_SEND_YOU_GET_PAIR, youSendYouGetPair), REQUEST_CODE_EXCHANGE_COINS)
        }
        binding?.getSymbolLayout?.setOnClickListener {
            val youSendYouGetPair = Pair(viewModel.youSend.value!!.type, viewModel.youGet.value!!.type)
            startActivityForResult(Intent(requireContext(), SelectCoinActivity::class.java)
                    .putExtra(PARENT, YOU_GET)
                    .putExtra(YOU_SEND_YOU_GET_PAIR, youSendYouGetPair), REQUEST_CODE_EXCHANGE_COINS)
        }
        binding?.exchange?.setOnClickListener {
            if (isDemo) {
                startActivity(Intent(requireActivity(), SignActivity::class.java))
            } else if (!BequantPreference.hasKeys()) {
                ModalDialog(getString(R.string.bequant_turn_2fa),
                        getString(R.string.bequant_recommend_enable_2fa),
                        getString(R.string.secure_your_account)) {
                    startActivity(Intent(requireActivity(), TwoFactorActivity::class.java))
                }.show(childFragmentManager, "modal_dialog")
            } else if (BequantPreference.getKYCStatus() != KYCStatus.VERIFIED) {
                askDoKyc()
            } else {
                makeExchange()
            }
        }

        binding?.icExchange?.setOnClickListener {
            val tempValue = viewModel.youSend.value
            viewModel.youSend.value = viewModel.youGet.value
            viewModel.youGet.value = tempValue
            updateAvailable()
        }
        binding?.deposit?.setOnClickListener {
            if (isDemo) {
                startActivity(Intent(requireActivity(), SignActivity::class.java))
            } else if (!BequantPreference.hasKeys()) {
                askEnable2Fa()
            } else if (BequantPreference.getKYCStatus() != KYCStatus.VERIFIED) {
                askDoKyc()
            } else {
                findNavController().navigate(ChoseCoinFragmentDirections.actionDeposit(viewModel.available.value!!.currencySymbol))
            }
        }
        binding?.btContactSupport?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BequantConstants.LINK_SUPPORT_CENTER)))
        }
    }

    private fun askDoKyc() {
        ModalDialog(getString(R.string.bequant_kyc_verify_title),
                getString(R.string.bequant_kyc_verify_message),
                getString(R.string.bequant_kyc_verify_button)) {
            startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
        }.show(childFragmentManager, "modal_dialog")
    }

    private fun askEnable2Fa() {
        ModalDialog(getString(R.string.bequant_turn_2fa_deposit),
                getString(R.string.bequant_enable_2fa),
                getString(R.string.secure_your_account)) {
            startActivity(Intent(requireActivity(), TwoFactorActivity::class.java))
        }.show(childFragmentManager, "modal_dialog")
    }

    private fun View.hideKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun onPause() {
        binding?.sendView?.clearFocus()
        binding?.sendView?.hideKeyboard()
        binding?.getView?.clearFocus()
        binding?.getView?.hideKeyboard()
        super.onPause()
    }

    private fun createPercentageRadioButtons() {
        val params: RadioGroup.LayoutParams = RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // convert dp to pixels as setMargins() works with pixels
        val dpValue = 8
        val dpRatio = requireContext().resources.displayMetrics.density
        val pixelForDp = dpValue * dpRatio.toInt()
        params.setMargins(0, 0, pixelForDp, 0)
        for (i in 25..100 step 25) {
            val rb = RadioButton(requireContext()).apply {
                text = "$i%"
                tag = i
                setOnClickListener {
                    updateYouSend((it.tag as Int))
                }
            }
            rb.layoutParams = params
            binding?.sendPercent?.addView(rb)
        }
        binding?.sendPercent?.apply {
            (getChildAt(childCount - 1) as RadioButton).isChecked = true
        }
    }

    @Subscribe
    fun onNewTradingBalance(balance: TradingBalance) {
        viewModel.tradingBalances.value = balance.balances
    }

    @Subscribe
    fun onNewAccountBalance(balance: AccountBalance) {
        viewModel.accountBalances.value = balance.balances
    }

    private fun updateExchangeEnabledFlag() {
        val rateExists = viewModel.rate.value!!.isNotBlank();
        val moreThanZero = viewModel.youSend.value?.isPositive() ?: false
        viewModel.isExchangeEnabled.value = rateExists && isEnoughFundsIncludingFees() && moreThanZero;
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        MbwManager.getEventBus().unregister(this)
        super.onDestroy()
    }

    private fun updateAvailable() {
        val youSend = viewModel.youSend.value!!

        val accountBalanceString = viewModel.accountBalances.value?.find { it.currency == youSend.type.symbol }?.available
        val accountBalance = if (accountBalanceString == null) {
            BigInteger.ZERO
        } else {
            (BigDecimal(accountBalanceString) * 10.0.pow(youSend.type.unitExponent).toBigDecimal()).toBigInteger()
        }
        val tradingBalanceString = viewModel.tradingBalances.value?.find { it.currency == youSend.type.symbol }?.available
        val tradingBalance = if (tradingBalanceString == null) {
            BigInteger.ZERO
        } else {
            (BigDecimal(tradingBalanceString) * 10.0.pow(youSend.type.unitExponent).toBigDecimal()).toBigInteger()
        }
        viewModel.available.value = Value.valueOf(youSend.type, tradingBalance + accountBalance)
    }

    private fun requestBalances() {
        MbwManager.getEventBus().post(RequestBalance())
    }

    private fun makeExchange() {
        val youSend = viewModel.youSend.value ?: return
        val youGet = viewModel.youGet.value ?: return

        binding?.clOrderRejected?.visibility = View.GONE

        val currency = youSend.currencySymbol
        BQExchangeRateManager.findSymbol(youGet.currencySymbol,
                youSend.currencySymbol) { symbol ->
            val isBuy = youGet.currencySymbol == symbol!!.baseCurrency
            val amount = youSend.valueAsBigDecimal
            val tradingBalance = BigDecimal(viewModel.tradingBalances.value?.find { it.currency == currency }?.available
                    ?: "0")
            val quantity = if (isBuy) youGet.toPlainString() else youSend.toPlainString()
            if (tradingBalance < amount) {
                val lackAmount = (amount - tradingBalance).setScale(youSend.type.unitExponent, BigDecimal.ROUND_UP)
                val lackAmountString = lackAmount.toString()
                loader(true)
                Api.accountRepository.accountTransferPost(viewLifecycleOwner.lifecycle.coroutineScope, currency, lackAmountString,
                        Transaction.Type.bankToExchange.value, {
                    requestBalances()
                    // wait for balance update
                    Timer("name", false).schedule(2000) {
                        // place market order
                        Api.tradingRepository.orderPost(viewLifecycleOwner.lifecycle.coroutineScope, symbol.id,
                                if (isBuy) "buy" else "sell", quantity, "", "market", "", "",
                                "", null, false, false, {
                            loader(false)
                            requireActivity().runOnUiThread {
                                showSummary()
                                requestBalances()
                            }
                        }, { _, error ->
                            ErrorHandler(requireContext()).handle(error)
                            requireActivity().runOnUiThread {
                                binding?.clOrderRejected?.visibility = View.VISIBLE
                            }
                        }, {})
                    }
                }, { _, error ->
                    ErrorHandler(requireContext()).handle(error)
                }, {
                    loader(false)
                })
            } else {
                Api.tradingRepository.orderPost(viewLifecycleOwner.lifecycle.coroutineScope, symbol.id,
                        if (isBuy) "buy" else "sell", quantity, "", "market", "", "",
                        "", null, false, false, {
                    requireActivity().runOnUiThread {
                        showSummary()
                        requestBalances()
                    }
                }, { _, error ->
                    ErrorHandler(requireContext()).handle(error)
                    requireActivity().runOnUiThread {
                        binding?.clOrderRejected?.visibility = View.VISIBLE
                    }
                }, {
                    loader(false)
                })
            }
        }
    }

    private fun showSummary() {
        val youSend = viewModel.youSend.value!!
        val youGet = viewModel.youGet.value!!
        val available = viewModel.available.value!!
        val oldAmountGetBigDecimal = BigDecimal(viewModel.tradingBalances.value?.find { it.currency == youGet.currencySymbol }?.available
                ?: "0") + BigDecimal(viewModel.accountBalances.value?.find { it.currency == youGet.currencySymbol }?.available
                ?: "0")
        val oldAmountGetValue = Value.valueOf(youGet.type, (oldAmountGetBigDecimal * 10.0.pow(youGet.type.unitExponent).toBigDecimal()).toBigInteger())
        val singleCoin = Value.valueOf(viewModel.youSend.value!!.type, 1, 0)
        val destPrice = BQExchangeRateManager.get(singleCoin, youGet.type)

        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.Theme_D1NoTitleDim)
        val dialogBinding = DialogBequantExchangeSummaryBinding.inflate(layoutInflater).apply {
            amountSend.text = youSend.toStringWithUnit()
            oldAmountSend.text = available.toStringWithUnit()
            oldAmountSend.paintFlags = oldAmountSend.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            newAmountSend.text = (available - youSend).toStringWithUnit()
            amountGet.text = "+ ${youGet.toStringWithUnit()}"
            oldAmountGet.text = oldAmountGetValue.toStringWithUnit()
            oldAmountGet.paintFlags = oldAmountGet.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            newAmountGet.text = (oldAmountGetValue + youGet).toStringWithUnit()
            exchangeRate.text = destPrice?.let {
                "${singleCoin.toStringWithUnit(Denomination.UNIT)} ~ ${it.toStringWithUnit(Denomination.UNIT)}"
            } ?: ""
            dialogBuilder.setView(root)
            val youGetEstimated = getString(R.string.bequant_exchange_summary_you_get_estimated)
            val youGetEstimatedSpanned = SpannableString(youGetEstimated).apply {
                setSpan(ForegroundColorSpan(resources.getColor(R.color.bequant_gray_6)),
                        youGetEstimated.indexOf("("), youGetEstimated.indexOf(")") + 1, 0)
            }
            tvYouGetEstimated.text = youGetEstimatedSpanned
        }

        val dialog = dialogBuilder.create()
        val blurredBg = BitmapDrawable(resources, BlurBuilder.blur(requireActivity()))
        dialog.window!!.setBackgroundDrawable(blurredBg)
        dialog.show()
        dialogBinding.btDone.setOnClickListener { dialog.cancel() }
    }

    private fun equals(value: Value?, text: String?) = ((value?.valueAsBigDecimal
            ?: BigDecimal.ZERO) - BigDecimal(text ?: "")).unscaledValue() == BigInteger.ZERO

    private fun recalculateDestinationPrice() {
        viewModel.youGet.value?.let { youGetValue ->
            val singleCoin = Value.valueOf(viewModel.youSend.value!!.type, 1, 0)
            val destPrice = BQExchangeRateManager.get(singleCoin, youGetValue.type)
            viewModel.rate.value = destPrice?.let { "${singleCoin.toStringWithUnit(Denomination.UNIT)} ~ ${it.toStringWithUnit(Denomination.UNIT)}" }
                    ?: ""
            updateExchangeEnabledFlag()
        }
    }

    private fun getFee(symbol: Symbol): BigDecimal {
        return BigDecimal(symbol.takeLiquidityRate)
    }

    private fun calculateSendValue() {
        viewModel.youGet.value?.let {
            val youSend = viewModel.youSend.value!!
            val youGet = viewModel.youGet.value!!
            BQExchangeRateManager.findSymbol(youGet.currencySymbol, youSend.currencySymbol) { symbol ->
                if (symbol != null) {
                    Api.publicRepository.publicTickerSymbolGet(viewLifecycleOwner.lifecycle.coroutineScope, symbol.id, { ticker ->
                        ticker?.let {
                            if (symbol.baseCurrency == youGet.currencySymbol) { // BUY base currency
                                val rate = BigDecimal(ticker.ask)
                                if (!youGet.isZero()) {
                                    val youSendDecimal = youGet.valueAsBigDecimal.multiply(rate.multiply(BigDecimal.valueOf(1L).plus(getFee(symbol))))
                                    viewModel.youSend.value = Value.parse(youSend.type, youSendDecimal)
                                }
                            } else { //SELL base currency
                                val rate = BigDecimal(ticker.bid)
                                val youSendDecimal = youGet.valueAsBigDecimal.divide(rate.multiply(BigDecimal.valueOf(1L).minus(getFee(symbol))), RoundingMode.HALF_DOWN)
                                viewModel.youSend.value = Value.parse(youSend.type, youSendDecimal.setScale(symbol.quantityIncrement.toBigDecimal().scale(), RoundingMode.DOWN))
                            }
                        }
                    }, { _, msg ->
                        ErrorHandler(requireContext()).handle(msg)
                    })
                }
            }
        }
    }

    private fun getAmountWithLimitsCorrection(available: BigDecimal, toSend: BigDecimal): BigDecimal {
        val maximumWExchangedAmount = available.multiply(BigDecimal(1 - RESERVED_PERCENT_WHEN_FULL_AMOUNT_EXCHANGED))
        return toSend.min(maximumWExchangedAmount)
    }

    private fun calculateReceiveValue() {
        viewModel.youSend.value?.let {
            val youSend = viewModel.youSend.value!!
            val youGet = viewModel.youGet.value!!
            BQExchangeRateManager.findSymbol(youGet.currencySymbol, youSend.currencySymbol) { symbol ->
                if (symbol != null) {
                    Api.publicRepository.publicTickerSymbolGet(viewLifecycleOwner.lifecycle.coroutineScope, symbol.id, { ticker ->
                        ticker?.let {
                            if (symbol.baseCurrency == youGet.currencySymbol) { // BUY base currency
                                val rate = BigDecimal(ticker.ask)
                                if (!youSend.isZero()) {
                                    val amountToSend = if (!isDemo) getAmountWithLimitsCorrection(viewModel.available.value?.valueAsBigDecimal!!, youSend.valueAsBigDecimal) else youSend.valueAsBigDecimal
                                    val youGetDecimal = amountToSend.divide(rate.multiply(BigDecimal.valueOf(1L).plus(getFee(symbol))), RoundingMode.HALF_DOWN)
                                    viewModel.youGet.value = Value.parse(youGet.type, youGetDecimal.setScale(symbol.quantityIncrement.toBigDecimal().scale(), RoundingMode.DOWN))
                                }
                            } else { //SELL base currency
                                val rate = BigDecimal(ticker.bid)
                                val youGetDecimal = youSend.valueAsBigDecimal.multiply(rate.multiply(BigDecimal.valueOf(1L).minus(getFee(symbol))))
                                viewModel.youGet.value = Value.parse(youGet.type, youGetDecimal)
                            }
                        }
                    }, { _, msg ->
                        ErrorHandler(requireContext()).handle(msg)
                    })
                }
            }
        }
    }

    private fun updateYouSend(rate: Int) {
        viewModel.available.value?.also { available ->
            val result = available.value.toDouble() * (rate.toDouble() / 100)
            viewModel.youSend.value = Value.valueOf(available.type,
                    result.toBigDecimal().toBigInteger())
        }
    }

    private fun isEnoughFundsIncludingFees(): Boolean {
        // We don't need a warning about funds availability in case of demo
        if (isDemo) {
            return true
        }
        val available = viewModel.available.value ?: return false
        val youSend = viewModel.youSend.value ?: return false
        return available.valueAsBigDecimal >= youSend.valueAsBigDecimal
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EXCHANGE_COINS && resultCode == Activity.RESULT_OK) {
            val youSendYouGetPair = data?.getSerializableExtra(YOU_SEND_YOU_GET_PAIR) as Pair<AssetInfo, AssetInfo>
            viewModel.youSend.value = Value.zeroValue(youSendYouGetPair.first)
            viewModel.youGet.value = Value.zeroValue(youSendYouGetPair.second)
        }
    }

    companion object {
        const val PARENT = "parent"
        const val YOU_SEND_YOU_GET_PAIR = "youSendYouGetPair"
        const val YOU_SEND = 0
        const val YOU_GET = 1
        private const val RESERVED_PERCENT_WHEN_FULL_AMOUNT_EXCHANGED = 0.1
    }
}

@BindingAdapter("isRedColor")
fun setRedTextColor(target: ConstraintLayout, isRedColor: Boolean) {
    target.setBackgroundResource(if (isRedColor) {
        R.drawable.bg_bequant_text_with_stroke_red
    } else {
        R.drawable.bg_bequant_text_with_stroke
    })
}