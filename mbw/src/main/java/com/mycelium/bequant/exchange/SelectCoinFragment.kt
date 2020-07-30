package com.mycelium.bequant.exchange

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.assetInfoById
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import kotlinx.android.synthetic.main.fragment_bequant_exchange_select_coin.*

class SelectCoinFragment : Fragment(R.layout.fragment_bequant_exchange_select_coin) {
    private val role: String by lazy {
        requireArguments().getString(ROLE)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL)
                .apply { setFromItem(1) })
        val adapter = CoinAdapter(role, (requireActivity() as SelectCoinActivity).youSendYouGetPair)
        list.adapter = adapter
        loader(true)
        Api.publicRepository.publicCurrencyGet(viewLifecycleOwner.lifecycle.coroutineScope, null, { list ->
            adapter.submitList(mutableListOf<CoinListItem>().apply {
                add(CoinListItem(CoinAdapter.TYPE_SEARCH))
                addAll((list?.toList()
                        ?: emptyList()).map { CoinListItem(CoinAdapter.TYPE_ITEM, it.assetInfoById()) })
                add(CoinListItem(CoinAdapter.TYPE_SPACE))
            })
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
            loader(false)
        })
        (requireActivity() as SelectCoinActivity).youSendYouGetPair.observe(viewLifecycleOwner, Observer {
            adapter.notifyDataSetChanged()
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
}