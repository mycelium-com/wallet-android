package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.getInvestmentAccounts
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Transaction
import com.mycelium.bequant.withdraw.adapter.WithdrawFragmentAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantWithdrawBinding
import com.mycelium.wapi.wallet.SyncMode
import java.math.BigDecimal
import java.util.*


class WithdrawFragment : Fragment() {
    val viewModel: WithdrawViewModel by viewModels()
    val args by navArgs<WithdrawFragmentArgs>()
    var binding: FragmentBequantWithdrawBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.currency.value = args.currency
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantWithdrawBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@WithdrawFragment.viewModel
                        lifecycleOwner = this@WithdrawFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBalance()
        binding?.pager?.adapter = WithdrawFragmentAdapter(this, viewModel, getSupportedByMycelium(args.currency
                ?: "btc"))
        binding?.tabs?.setupWithViewPager(binding?.pager)
        binding?.pager?.offscreenPageLimit = 2
        binding?.send?.isEnabled = false
        viewModel.amount.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val amount = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val custodialBalance = viewModel.custodialBalance.value?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO
            val hasSufficientFunds = amount <= custodialBalance
            binding?.layoutBequantAmount?.edAmount?.error = if (hasSufficientFunds) null else getString(R.string.insufficient_funds)
            binding?.send?.isEnabled = hasSufficientFunds && amount > 0.toBigDecimal()
        })
        binding?.send?.setOnClickListener { withdraw() }
    }

    private fun getSupportedByMycelium(currency: String): Boolean =
            currency.toLowerCase(Locale.US) in listOf("eth", "btc")

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
                usualWithdraw()
            } else {
                val withdrawFromTrading = total - (viewModel.accountBalance.value
                        ?: BigDecimal.ZERO)
                Api.accountRepository.accountTransferPost(
                        viewLifecycleOwner.lifecycle.coroutineScope,
                        args.currency,
                        withdrawFromTrading.toPlainString(),
                        Transaction.Type.exchangeToBank.value,
                        success = { usualWithdraw() },
                        error = { _, message -> ErrorHandler(requireContext()).handle(message) },
                        finally = { loader(false) })
            }
        }
    }

    private fun usualWithdraw() {
        viewModel.withdraw({
            startSyncInvestmentAccounts()
            findNavController().popBackStack()
        }, { _, message ->
            ErrorHandler(requireContext()).handle(message)
        }, {
            loader(false)
        })
    }

    private fun startSyncInvestmentAccounts() {
        MbwManager.getInstance(requireContext()).getWalletManager(false).let {
            it.startSynchronization(SyncMode.NORMAL_FORCED, it.getInvestmentAccounts())
        }
    }

    private fun loadBalance() {
        loader(true)
        viewModel.loadBalance({ accountBalance, tradingBalance ->
            val accountBalanceStr = accountBalance?.find { it.currency == args.currency }?.available
            val tradingBalanceStr = tradingBalance?.find { it.currency == args.currency }?.available

            viewModel.accountBalance.value = BigDecimal(accountBalanceStr ?: "0")
            viewModel.tradingBalance.value = BigDecimal(tradingBalanceStr ?: "0")
            val sum = (viewModel.accountBalance.value as BigDecimal) + (viewModel.tradingBalance.value as BigDecimal)
            viewModel.custodialBalance.value = sum.toPlainString()
        }, { _, message ->
            ErrorHandler(requireContext()).handle(message)
            viewModel.custodialBalance.value = "0.00"
        }, {
            loader(false)
        })
    }
}