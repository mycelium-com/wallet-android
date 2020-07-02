package com.mycelium.bequant.market

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BQExchangeRateManager
import com.mycelium.bequant.Constants
import com.mycelium.bequant.Constants.REQUEST_CODE_EXCHANGE_COINS
import com.mycelium.bequant.common.BlurBuilder
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.exchange.SelectCoinActivity
import com.mycelium.bequant.market.viewmodel.ExchangeViewModel
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Transaction
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.databinding.FragmentBequantExchangeBinding
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.dialog_bequant_exchange_summary.*
import kotlinx.android.synthetic.main.fragment_bequant_exchange.*
import kotlinx.android.synthetic.main.layout_value_keyboard.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.math.pow


class ExchangeFragment : Fragment() {

    private lateinit var viewModel: ExchangeViewModel
    private lateinit var exchangeRateManager: BQExchangeRateManager
    var getViewActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exchangeRateManager = BQExchangeRateManager(requireContext())
        exchangeRateManager.requestOptionalRefresh()
        viewModel = ViewModelProviders.of(this).get(ExchangeViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantExchangeBinding>(inflater, R.layout.fragment_bequant_exchange, container, false)
                    .apply {
                        viewModel = this@ExchangeFragment.viewModel
                        lifecycleOwner = this@ExchangeFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in 25..100 step 25) {
            val rb = RadioButton(requireContext()).apply {
                text = "$i%"
                tag = i
                setOnClickListener {
                    updateYouSend((it.tag as Int))
                }
            }
            val params: RadioGroup.LayoutParams = RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // convert dp to pixels as setMargins() works with pixels
            val dpValue = 8
            val dpRatio = requireContext().resources.displayMetrics.density
            val pixelForDp = dpValue * dpRatio.toInt()
            params.setMargins(0, 0, pixelForDp, 0)
            rb.layoutParams = params
            send_percent.addView(rb)
        }
        send_percent.apply {
            (getChildAt(childCount - 1) as RadioButton).isChecked = true
        }
        numeric_keyboard.setInputListener(object : ValueKeyboard.SimpleInputListener() {
            override fun done() {
                getViewActive = false
                stopCursor(sendView)
                stopCursor(getView)
            }
        })
        clSendView.setOnClickListener {
            numeric_keyboard.setMaxDecimals(viewModel.youSend.value?.type?.unitExponent ?: 8)
            numeric_keyboard.visibility = View.VISIBLE
            numeric_keyboard.inputTextView = sendView
            numeric_keyboard.setEntry(sendView.text.toString())
            startCursor(sendView)
            stopCursor(getView)
            getViewActive = false

        }
        clGetView.setOnClickListener {
            numeric_keyboard.setMaxDecimals(viewModel.youGet.value?.type?.unitExponent ?: 8)
            numeric_keyboard.visibility = View.VISIBLE
            numeric_keyboard.inputTextView = getView
            numeric_keyboard.setEntry(getView.text.toString())
            startCursor(getView)
            stopCursor(sendView)
            getViewActive = true
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
        })
        viewModel.youSendText.observe(viewLifecycleOwner, Observer {
            try {
                if (!equals(viewModel.youGet.value, it)) {
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
        sendSymbolLayout.setOnClickListener {
            startActivityForResult(Intent(requireContext(), SelectCoinActivity::class.java)
                    .putExtra(PARENT, YOU_SEND), REQUEST_CODE_EXCHANGE_COINS)
        }
        getSymbolLayout.setOnClickListener {
            startActivityForResult(Intent(requireContext(), SelectCoinActivity::class.java)
                    .putExtra(PARENT, YOU_GET), REQUEST_CODE_EXCHANGE_COINS)
        }
        exchange.setOnClickListener {
            // TODO add check that KYC has been passed or show BequantKycActivity
            // startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
            makeExchange()
        }

        icExchange.setOnClickListener {
            val tempValue = viewModel.youSend.value
            viewModel.youSend.value = viewModel.youGet.value
            viewModel.youGet.value = tempValue
        }
        deposit.setOnClickListener { findNavController().navigate(MarketFragmentDirections.actionSelectCoin("deposit")) }
        btContactSupport.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_SUPPORT_CENTER)))
        }
        requestBalances()
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
        updateYouSend(100)
    }

    private fun requestBalances() {
        Api.tradingRepository.tradingBalanceGet(viewLifecycleOwner.lifecycle.coroutineScope, { arrayOfBalances ->
            viewModel.tradingBalances.value = arrayOfBalances
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
        })
        Api.accountRepository.accountBalanceGet(viewLifecycleOwner.lifecycle.coroutineScope, { arrayOfBalances ->
            viewModel.accountBalances.value = arrayOfBalances
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
        })
    }

    private fun makeExchange() {
        val youSend = viewModel.youSend.value ?: return
        val youGet = viewModel.youGet.value ?: return

        clOrderRejected.visibility = View.GONE

        val currency = youSend.currencySymbol
        val amount = youSend.value
        val tradingBalance = BigInteger(viewModel.tradingBalances.value?.find { it.currency == currency }?.available ?: "0")
        if (tradingBalance < amount) {
            val lackAmount = amount - tradingBalance
            val lackAmountString = BigDecimal(lackAmount, youSend.type.unitExponent).stripTrailingZeros().toString()
            loader(true)
            Api.accountRepository.accountTransferPost(viewLifecycleOwner.lifecycle.coroutineScope, currency, lackAmountString,
                    Transaction.Type.bankToExchange.value, {
                // update balance after exchange
                requestBalances()
                // place market order
                val symbol = youSend.currencySymbol + youGet.currencySymbol
                val quantity = youGet.value.toString() // TODO add check for compliance with quantityIncrement
                Api.tradingRepository.orderPost(viewLifecycleOwner.lifecycle.coroutineScope, symbol,
                        "buy", quantity, "", "market", "", "",
                        "", Date(), false, false, {
                    loader(false)
                    requireActivity().runOnUiThread {
                        showSummary()
                    }
                }, { code, error ->
                    ErrorHandler(requireContext()).handle(error)
                    requireActivity().runOnUiThread {
                        clOrderRejected.visibility = View.VISIBLE
                    }
                }, {})
            }, { code, error ->
                ErrorHandler(requireContext()).handle(error)
            }, {
                loader(false)
            })
        } else {
            val symbol = youSend.currencySymbol + youGet.currencySymbol
            val quantity = youGet.value.toString()
            Api.tradingRepository.orderPost(viewLifecycleOwner.lifecycle.coroutineScope, symbol,
                    "buy", quantity, "", "market", "", "",
                    "", Date(), true, false, {
                requireActivity().runOnUiThread {
                    showSummary()
                }
            }, { code, error ->
                ErrorHandler(requireContext()).handle(error)
                requireActivity().runOnUiThread {
                    clOrderRejected.visibility = View.VISIBLE
                }
            }, {
                loader(false)
            })
        }
    }

    private fun showSummary() {
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.Theme_D1NoTitleDim)
        val customLayout = layoutInflater.inflate(R.layout.dialog_bequant_exchange_summary, null)
        dialogBuilder.setView(customLayout)
        val dialog = dialogBuilder.create()
        val blurredBg = BitmapDrawable(resources, BlurBuilder.blur(requireActivity()))
        dialog.window.setBackgroundDrawable(blurredBg)
        dialog.show()
        dialog.btDone.setOnClickListener { dialog.cancel() }
    }

    private fun equals(value: Value?, text: String?) = ((value?.valueAsBigDecimal
            ?: BigDecimal.ZERO) - BigDecimal(text ?: "")).unscaledValue() == BigInteger.ZERO

    private fun startCursor(textView: TextView) {
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bequant_input_cursor, 0)
        textView.post {
            val animationDrawable = textView.compoundDrawables[2] as AnimationDrawable
            if (!animationDrawable.isRunning) {
                animationDrawable.start()
            }
        }
    }

    private fun stopCursor(textView: TextView) {
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }

    private fun recalculateDestinationPrice() {
        viewModel.youGet.value?.let { youGetValue ->
            val singleCoin = Value.valueOf(viewModel.youSend.value!!.type, 1, 0)
            val destPrice = exchangeRateManager.get(singleCoin, youGetValue.type)
            viewModel.rate.value = destPrice?.let { "${singleCoin.toStringWithUnit(Denomination.UNIT)} ~ ${it.toStringWithUnit(Denomination.UNIT)}" }
                    ?: ""
        }
    }

    private fun calculateSendValue() {
        viewModel.youGet.value?.let { youGetValue ->
            viewModel.youSend.value = exchangeRateManager.get(youGetValue, viewModel.youSend.value!!.type)
                    ?: Value.zeroValue(viewModel.youSend.value!!.type)
        }
    }

    private fun calculateReceiveValue() {
        viewModel.youSend.value?.let { youSendValue ->
            viewModel.youGet.value = exchangeRateManager.get(youSendValue, viewModel.youGet.value!!.type)
                    ?: Value.zeroValue(viewModel.youGet.value!!.type)
        }
    }

    private fun updateYouSend(rate: Int) {
        val available = viewModel.available.value
        if (available != null) {
            val result = available.value.toDouble() * (rate.toDouble() / 100)
            viewModel.youSend.value = Value.valueOf(available.type,
                    result.toBigDecimal().toBigInteger())
        }
    }

    companion object {
        const val PARENT = "parent"
        const val YOU_SEND = 0
        const val YOU_GET = 1
    }
}

@BindingAdapter("isRedColor")
fun setRedTextColor(target: ConstraintLayout, isRedColor: Boolean) {
    if (isRedColor) {
        target.setBackgroundResource(R.drawable.bg_bequant_text_with_stroke_red)
    } else {
        target.setBackgroundResource(R.drawable.bg_bequant_text_with_stroke)
    }
}