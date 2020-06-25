package com.mycelium.bequant.market

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants.REQUEST_CODE_EXCHANGE_COINS
import com.mycelium.bequant.exchange.SelectCoinActivity
import com.mycelium.bequant.kyc.BequantKycActivity
import com.mycelium.bequant.market.viewmodel.ExchangeViewModel
import com.mycelium.view.Denomination
import com.mycelium.wallet.ExchangeRateManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.databinding.FragmentBequantExchangeBinding
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_bequant_exchange.*
import kotlinx.android.synthetic.main.layout_value_keyboard.*


class ExchangeFragment : Fragment() {

    private lateinit var viewModel: ExchangeViewModel
    private lateinit var exchangeRateManager: ExchangeRateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exchangeRateManager = MbwManager.getInstance(requireContext()).exchangeRateManager
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
            send_percent.addView(rb)
        }
        send_percent.apply {
            (getChildAt(childCount - 1) as RadioButton).isChecked = true
        }
        sendView.setOnClickListener {
            numeric_keyboard.setMaxDecimals(viewModel.youSend.value?.type?.unitExponent ?: 8)
            numeric_keyboard.visibility = View.VISIBLE
            numeric_keyboard.inputTextView = sendView
            numeric_keyboard.setEntry(sendView.text.toString())
        }
        getView.setOnClickListener {
            numeric_keyboard.setMaxDecimals(viewModel.youGet.value?.type?.unitExponent ?: 8)
            numeric_keyboard.visibility = View.VISIBLE
            numeric_keyboard.inputTextView = getView
            numeric_keyboard.setEntry(getView.text.toString())
        }
        viewModel.youSend.observe(viewLifecycleOwner, Observer {
            it.toString(Denomination.UNIT).let {
                if (it != viewModel.youSendText.value) {
                    viewModel.youSendText.value = it
                }
            }
        })
        viewModel.youSendText.observe(viewLifecycleOwner, Observer {
            viewModel.youSend.value?.type?.let { type ->
                Value.valueOf(type, it).let {
                    if (viewModel.youSend != it) {
                        viewModel.youSend.value = it
                    }
                }
            }
        })
        viewModel.youGet.observe(viewLifecycleOwner, Observer {
            it.toString(Denomination.UNIT).let {
                if (it != viewModel.youGetText.value) {
                    viewModel.youGetText.value = it
                }
            }
        })
        viewModel.youGetText.observe(viewLifecycleOwner, Observer {
            viewModel.youGet.value?.type?.let { type ->
                Value.valueOf(type, it).let {
                    if (viewModel.youGet != it) {
                        viewModel.youGet.value = it
                    }
                }
            }
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
            startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
        }
        icExchange.setOnClickListener {
            val tempValue = viewModel.youSend.value
            viewModel.youSend.value = viewModel.youGet.value
            viewModel.youGet.value = tempValue
        }
        deposit.setOnClickListener { findNavController().navigate(MarketFragmentDirections.actionSelectCoin("deposit")) }
        updateYouSend(100)
        recalculateDestinationPrice()
        recalculateReceivedValue()
    }

    private fun recalculateDestinationPrice() {
        val singleCoin = Value.valueOf(viewModel.youSend.value!!.type, 1, 0);
        viewModel.fullSourceUnitDestinationPrice.value = exchangeRateManager.get(singleCoin, viewModel.youGet.value!!.type)
    }

    private fun recalculateReceivedValue() {
        viewModel.youGet.value = exchangeRateManager.get(viewModel.youSend.value, viewModel.youGet.value!!.type)
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