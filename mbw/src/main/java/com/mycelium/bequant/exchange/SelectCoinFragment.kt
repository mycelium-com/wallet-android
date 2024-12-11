package com.mycelium.bequant.exchange

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.assetInfoById
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.bequant.market.ExchangeFragment
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Currency
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentBequantExchangeSelectCoinBinding

class SelectCoinFragment : Fragment(), CoinAdapter.ClickListener {
    private val role: String by lazy {
        requireArguments().getString(ROLE)!!
    }
    private val currencyList = mutableListOf<Currency>()
    private var binding: FragmentBequantExchangeSelectCoinBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantExchangeSelectCoinBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL)
                .apply { setFromItem(1) })
        val adapter = CoinAdapter(role, this, (requireActivity() as SelectCoinActivity).youSendYouGetPair)
        binding?.list?.adapter = adapter
        binding?.searchLayout?.search?.doOnTextChanged { filter, _, _, _ ->
            updateData(adapter, currencyList.filter {
                it.fullName.contains(filter ?: "", true) || it.id.contains(filter ?: "", true)
            })
        }
        binding?.searchLayout?.clear?.setOnClickListener {
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding?.searchLayout?.search?.applicationWindowToken, 0)
            binding?.searchLayout?.search?.text = null
            updateData(adapter, currencyList)
        }
        loader(true)
        Api.publicRepository.publicCurrencyGet(viewLifecycleOwner.lifecycle.coroutineScope, null, { list ->
            currencyList.clear()
            currencyList.addAll(list ?: arrayOf())
            updateData(adapter, currencyList)
        }, { _, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
            loader(false)
        })
        (requireActivity() as SelectCoinActivity).youSendYouGetPair.observe(viewLifecycleOwner, Observer {
            adapter.notifyDataSetChanged()
        })
    }

    private fun updateData(adapter: CoinAdapter, data: List<Currency>) {
        adapter.submitList(mutableListOf<CoinListItem>().apply {
            addAll(data.map { CoinListItem(CoinAdapter.TYPE_ITEM, it.assetInfoById()) })
            add(CoinListItem(CoinAdapter.TYPE_SPACE))
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(role: String): SelectCoinFragment {
            val f = SelectCoinFragment()
            val args = Bundle()

            args.putString(ROLE, role)
            f.arguments = args
            return f
        }

        const val ROLE = "role"
        const val SEND = "send"
        const val GET = "get"
    }

    override fun onClick() {
        val result = Intent()
        result.putExtra(ExchangeFragment.YOU_SEND_YOU_GET_PAIR, (requireActivity() as SelectCoinActivity).youSendYouGetPair.value)
        requireActivity().setResult(Activity.RESULT_OK, result)
        requireActivity().finish()
    }
}