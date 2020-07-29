package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Transaction
import com.mycelium.bequant.withdraw.adapter.WithdrawFragmentAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantWithdrawBinding
import kotlinx.android.synthetic.main.fragment_bequant_withdraw.*
import kotlinx.android.synthetic.main.layout_bequant_amount.*
import java.math.BigDecimal
import java.util.*


class WithdrawFragment : Fragment() {

    lateinit var viewModel: WithdrawViewModel

    val args by navArgs<WithdrawFragmentArgs>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WithdrawViewModel::class.java)
        viewModel.currency.value = args.currency
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantWithdrawBinding>(inflater, R.layout.fragment_bequant_withdraw, container, false)
                    .apply {
                        viewModel = this@WithdrawFragment.viewModel
                        lifecycleOwner = this@WithdrawFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBalance()
        pager.adapter = WithdrawFragmentAdapter(this, viewModel, getSupportedByMycelium(args.currency
                ?: "btc"))
        tabs.setupWithViewPager(pager)
        pager.offscreenPageLimit = 2
        viewModel.amount.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val amount = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val custodialBalance = viewModel.castodialBalance.value?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO
            val enoughAmount = amount <= custodialBalance
            edAmount.error = if (enoughAmount) null else getString(R.string.insufficient_funds)
            send.isEnabled = enoughAmount && amount > 0.toBigDecimal()
        })
        send.setOnClickListener { withdraw() }
    }

    private fun getSupportedByMycelium(currency: String): Boolean {
        return currency.toLowerCase(Locale.getDefault()) in listOf("eth", "btc")
    }

    private fun withdraw() {
        val moneyToWithdraw = BigDecimal(viewModel.amount.value)
        if (moneyToWithdraw > 0.toBigDecimal()) {
            val total = viewModel.accountBalance.value as BigDecimal + viewModel.tradingBalance.value as BigDecimal
            if (moneyToWithdraw > total) {
                ErrorHandler(requireContext()).handle(getString(R.string.insufficient_funds))
                return
            }
            loader(true)
            if (moneyToWithdraw < viewModel.accountBalance.value) {
                usualWithraw()
            } else {
                val withdrawFromTrading = total - (viewModel.accountBalance.value
                        ?: BigDecimal.ZERO)
                Api.accountRepository.accountTransferPost(viewLifecycleOwner.lifecycle.coroutineScope, args.currency!!, withdrawFromTrading.toPlainString(),
                        Transaction.Type.exchangeToBank.value, {
                    usualWithraw()
                }, { int, message ->
                    ErrorHandler(requireContext()).handle(message)
                }, {
                    loader(false)
                })
            }
        }
    }

    private fun usualWithraw() {
        viewModel.withdraw({
            findNavController().popBackStack()
        }, { int, message ->
            ErrorHandler(requireContext()).handle(message)
        }, {
            loader(false)
        })
    }

    private fun loadBalance() {
        loader(true)
        viewModel.loadBalance({ accountBalance, tradingBalance ->
            val accountBalanceStr = accountBalance?.find { it.currency == args.currency }?.available
            val tradingBalanceStr = tradingBalance?.find { it.currency == args.currency }?.available

            viewModel.accountBalance.value = BigDecimal(accountBalanceStr ?: "0")
            viewModel.tradingBalance.value = BigDecimal(tradingBalanceStr ?: "0")
            val sum = (viewModel.accountBalance.value as BigDecimal) + (viewModel.tradingBalance.value as BigDecimal)
            viewModel.castodialBalance.value = sum.toPlainString()
        }, { _, message ->
            ErrorHandler(requireContext()).handle(message)
            viewModel.castodialBalance.value = "0.00"
        }, {
            loader(false)
        })
    }
}