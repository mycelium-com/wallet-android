package com.mycelium.wallet.external.changelly2

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.math.BigDecimal


class ExchangeFragment : Fragment() {

    var binding: FragmentChangelly2ExchangeBinding? = null
    val viewModel: ExchangeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = MbwManager.getInstance(requireContext())
        viewModel.fromAccount.value = manager.selectedAccount
        viewModel.toAccount.value = manager.getWalletManager(false).getAllActiveAccounts().first { it.coinType != manager.selectedAccount.coinType }
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
            binding?.layoutValueKeyboard?.numericKeyboard?.setMaxDecimals(viewModel.fromCurrency.value?.unitExponent
                    ?: 0)
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.sellLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
            binding?.layoutValueKeyboard?.numericKeyboard?.setMaxValue(minOf(
                    viewModel.fromAccount.value?.accountBalance?.spendable?.valueAsBigDecimal
                            ?: BigDecimal.ZERO,
                    viewModel.exchangeInfo.value?.maxFrom?.toBigDecimal() ?: BigDecimal.ZERO))
        }
        binding?.buyLayout?.root?.setOnClickListener {
            binding?.buyLayout?.coinValue?.startCursor()
            binding?.sellLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.setMaxDecimals(viewModel.toCurrency.value?.unitExponent
                    ?: 0)
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.buyLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
            binding?.layoutValueKeyboard?.numericKeyboard?.setMaxValue(viewModel.exchangeInfo.value?.maxTo?.toBigDecimal()
                    ?: BigDecimal.ZERO)
        }
        binding?.sellLayout?.coinValue?.doAfterTextChanged { _ ->
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.sellLayout?.coinValue) {
                try {
                    val amount = binding?.sellLayout?.coinValue?.text?.toString()?.toDouble()
                    viewModel.buyValue.value = ((amount ?: 0.0) *
                            (viewModel.exchangeInfo.value?.result ?: 0.0)).toString()
                } catch (e: NumberFormatException) {
                }
            }
        }
        binding?.buyLayout?.coinValue?.doAfterTextChanged { _ ->
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.buyLayout?.coinValue) {
                try {
                    val amount = binding?.buyLayout?.coinValue?.text?.toString()?.toDouble()
                    viewModel.sellValue.value = ((amount ?: 0.0) /
                            (viewModel.exchangeInfo.value?.result ?: 1.0)).toString()
                } catch (e: NumberFormatException) {
                }
            }
        }

        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            setInputListener(object : ValueKeyboard.SimpleInputListener() {
                override fun done() {
                    binding?.sellLayout?.coinValue?.stopCursor()
                    binding?.buyLayout?.coinValue?.stopCursor()
//                    useAllFunds.setVisibility(View.VISIBLE);
//                    fromValue.setHint(R.string.zero);
//                    toValue.setHint(R.string.zero);
//                    isValueForOfferOk(true);
                }
            });
            setMaxText(getString(R.string.max), 14f)
            setPasteVisibility(View.GONE)
            visibility = View.GONE
        }
        viewModel.fromCurrency.observe(viewLifecycleOwner, Observer<CryptoCurrency> {
            updateExchangeRate()
        })
        viewModel.toCurrency.observe(viewLifecycleOwner, Observer<CryptoCurrency> {
            updateExchangeRate()
        })
        updateExchangeRate()
    }

    private fun updateExchangeRate() {
        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
            Changelly2Repository.fixRate(lifecycleScope,
                    Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
                    Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
                    { result ->
                        if (result?.result != null) {
                            viewModel.exchangeInfo.value = result.result
                            viewModel.sellValue.value = result.result?.minFrom.toString()
                            viewModel.error.value = ""
                        } else {
                            viewModel.error.value = result?.error?.message ?: ""
                        }
                    },
                    { _, _ ->

                    })
        }
    }

//    private fun requestExchangeRate(amount: String) {
//        val dblAmount = try {
//            amount.toDouble()
//        } catch (e: NumberFormatException) {
//            Toaster(requireContext()).toast("Error parsing double values", true)
//            return
//        }
//        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
//            Changelly2Repository.exchangeAmount(lifecycleScope,
//                    Util.trimTestnetSymbolDecoration(viewModel.fromCurrency.value?.symbol!!),
//                    Util.trimTestnetSymbolDecoration(viewModel.toCurrency.value?.symbol!!),
//                    dblAmount,
//                    { result ->
//                        if (result?.result != null) {
//                            viewModel.error.value = ""
//                            viewModel.buyValue.value = result.result?.amountTo?.toString()
//                        } else {
//                            viewModel.error.value = result?.error?.message ?: ""
//                        }
//                    },
//                    { _, _ ->
//
//                    },
//                    {
//                    })
//        }
//    }


    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun TextView.startCursor() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.input_cursor, 0)
        post {
            val animationDrawable = compoundDrawables[2] as AnimationDrawable
            if (!animationDrawable.isRunning) {
                animationDrawable.start()
            }
        }
    }

    private fun TextView.stopCursor() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}