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
import com.megiontechnologies.Bitcoins
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.withdraw.adapter.WithdrawFragmentAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantWithdrawBinding
import kotlinx.android.synthetic.main.fragment_bequant_withdraw.*
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
                ?:"btc"))
        tabs.setupWithViewPager(pager)
        pager.offscreenPageLimit = 2
        send.setOnClickListener { withdraw() }
    }

    private fun getSupportedByMycelium(currency: String): Boolean {
        return currency.toLowerCase(Locale.getDefault()) in listOf("eth", "btc")
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
            viewModel.castodialBalance.value = Bitcoins.valueOf(balance?.available).toString()
        }, { _, message ->
            ErrorHandler(requireContext()).handle(message)
        }, {
            loader(false)
        })
    }
}