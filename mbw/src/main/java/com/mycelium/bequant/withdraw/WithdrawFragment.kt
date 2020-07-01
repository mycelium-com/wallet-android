package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.withdraw.adapter.WithdrawFragmentAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.databinding.FragmentBequantWithdrawBinding
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_bequant_withdraw.*
import java.math.BigInteger


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
        pager.adapter = WithdrawFragmentAdapter(this, viewModel)
        tabs.setupWithViewPager(pager)
        pager.offscreenPageLimit = 2
        send.setOnClickListener { withdraw() }
    }

    private fun getCryptoCurrency(): CryptoCurrency {
        return when (args.currency?.toLowerCase()) {
            "btc" -> Utils.getBtcCoinType()
            "eth" -> Utils.getEthCoinType()
            //TODO
            else -> Utils.getBtcCoinType()
        }
    }

    private fun withdraw() {
        if (viewModel.amount.value != null) {
            viewModel.address.value = Constants.TEST_ADDRESS
            loader(true)
            viewModel.withdraw({
                findNavController().popBackStack()
            }, { int, message ->
                ErrorHandler(requireContext()).handle(message)
            }, {
                loader(false)
            })
        }
    }

    private fun loadBalance() {
        loader(true)
        viewModel.loadBalance({
            val balance = it?.find { it.currency == args.currency }
            val balanceValue = Value.valueOf(getCryptoCurrency(), BigInteger(balance?.available?:"0"))
            viewModel.castodialBalance.value = balanceValue.toString(Denomination.UNIT)
        }, { _, message ->
            ErrorHandler(requireContext()).handle(message)
        }, {
            loader(false)
        })
    }
}