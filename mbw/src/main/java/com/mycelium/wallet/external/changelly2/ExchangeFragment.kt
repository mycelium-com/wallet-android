package com.mycelium.wallet.external.changelly2

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel


class ExchangeFragment : Fragment() {

    var binding: FragmentChangelly2ExchangeBinding? = null
    val viewModel: ExchangeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = MbwManager.getInstance(requireContext())
        viewModel.fromAccount.value = manager.selectedAccount
        viewModel.toAccount.value = manager.selectedAccount
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2ExchangeBinding.inflate(inflater).apply {
                binding = this
                vm = viewModel
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.sellLayout?.root?.setOnClickListener {
            binding?.sellLayout?.coinValue?.startCursor()
            binding?.buyLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.sellLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
        }
        binding?.buyLayout?.root?.setOnClickListener {
            binding?.buyLayout?.coinValue?.startCursor()
            binding?.sellLayout?.coinValue?.stopCursor()
            binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView = binding?.buyLayout?.coinValue
            binding?.layoutValueKeyboard?.numericKeyboard?.visibility = View.VISIBLE;
        }

        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            setMaxDecimals(8)
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
            setMaxText(getString(R.string.use_all_funds), 14f)
            setPasteVisibility(View.GONE)
            visibility = View.GONE
        }
        requestExchangeRate("1")
    }

    private fun requestExchangeRate(amount: String) {
        val dblAmount = try {
            amount.toDouble()
        } catch (e: NumberFormatException) {
            Toaster(requireContext()).toast("Error parsing double values", true)
            return
        }
//        if (viewModel.fromCurrency.value?.symbol != null && viewModel.toCurrency.value?.symbol != null) {
            Changelly2Repository.exchangeAmount(lifecycleScope,
                    "BTC",//viewModel.fromCurrency.value?.symbol!!,
                    "ETH", //viewModel.toCurrency.value?.symbol!!,
                    dblAmount,
                    {
                        viewModel.exchangeRate.value = it?.result?.toString() ?: "!!!!"
                    },
                    { _, _ ->

                    },
                    {
                    })
//        }
    }


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