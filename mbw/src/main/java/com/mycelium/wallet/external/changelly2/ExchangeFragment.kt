package com.mycelium.wallet.external.changelly2

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.view.ValueKeyboard
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeBinding
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import java.math.BigDecimal
import java.util.concurrent.TimeUnit


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
            binding?.layoutValueKeyboard?.numericKeyboard?.setSpendableValue(viewModel.fromAccount.value?.accountBalance?.spendable?.valueAsBigDecimal)
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
            binding?.layoutValueKeyboard?.numericKeyboard?.setSpendableValue(viewModel.toAccount.value?.accountBalance?.spendable?.valueAsBigDecimal)
        }
        binding?.sellLayout?.coinValue?.doAfterTextChanged {
            viewModel.sellValue.value = binding?.sellLayout?.coinValue?.text?.toString()
            if (binding?.layoutValueKeyboard?.numericKeyboard?.inputTextView == binding?.sellLayout?.coinValue) {
                viewModel.error.value = ""
                try {
                    val amount = binding?.sellLayout?.coinValue?.text?.toString()?.toDouble()
                    binding?.buyLayout?.coinValue?.text = ((amount ?: 0.0) *
                            (viewModel.exchangeInfo.value?.result ?: 0.0)).toString()
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
                    val amount = binding?.buyLayout?.coinValue?.text?.toString()?.toDouble()
                    binding?.sellLayout?.coinValue?.text = ((amount ?: 0.0) /
                            (viewModel.exchangeInfo.value?.result ?: 1.0)).toString()
                } catch (e: NumberFormatException) {
                    binding?.sellLayout?.coinValue?.text = "N/A"
                }
            }
            binding?.buyLayout?.coinValue?.resizeTextView()
        }
        binding?.swapAccount?.setOnClickListener {
            val newFrom = viewModel.fromAccount.value
            val newTo = viewModel.toAccount.value
            viewModel.fromAccount.value = newTo
            viewModel.toAccount.value = newFrom
            binding?.sellLayout?.coinValue?.text = viewModel.buyValue.value
        }
        binding?.layoutValueKeyboard?.numericKeyboard?.apply {
            setInputListener(object : ValueKeyboard.SimpleInputListener() {
                override fun done() {
                    binding?.sellLayout?.coinValue?.stopCursor()
                    binding?.buyLayout?.coinValue?.stopCursor()
//                    fromValue.setHint(R.string.zero);
//                    toValue.setHint(R.string.zero);
//                    isValueForOfferOk(true);
                }
            })
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
                            viewModel.mbwManager.runPinProtectedFunction(requireActivity()) {
                                val address = when (viewModel.fromAccount.value) {
                                    is EthAccount, is ERC20Account -> {
                                        EthAddress(Utils.getEthCoinType(), result.result!!.payinAddress!!)
                                    }
                                    is AbstractBtcAccount -> {
                                        BtcAddress(
                                                Utils.getBtcCoinType(), BitcoinAddress.fromString(result.result!!.payinAddress!!)
                                        )
                                    }
                                    else -> TODO("Account not supported yet")
                                }

                                viewModel.fromAccount.value?.let { account ->
                                    val createTx = account.createTx(address,
                                            viewModel.fromAccount.value!!.coinType.value(result.result!!.amountExpectedFrom!!),
                                            FeePerKbFee(viewModel.feeEstimation.value!!.normal),
                                            null
                                    )
                                    account.signTx(createTx, AesKeyCipher.defaultKeyCipher())
                                    account.broadcastTx(createTx)
                                }
                            }
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
        viewModel.fromCurrency.observe(viewLifecycleOwner) {
            updateExchangeRate()
        }
        viewModel.toCurrency.observe(viewLifecycleOwner) {
            updateExchangeRate()
        }
        startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.MINUTES.toMillis(2)) {
            updateExchangeRate()
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
                                binding?.sellLayout?.coinValue?.text = result.result?.minFrom.toString()
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

    private fun TextView.resizeTextView() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (text.toString().length < 11) 36f else 22f)
    }
}