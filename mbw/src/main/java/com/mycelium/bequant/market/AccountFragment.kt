package com.mycelium.bequant.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants.HIDE_VALUE
import com.mycelium.bequant.Constants.TYPE_ITEM
import com.mycelium.bequant.market.adapter.AccountItem
import com.mycelium.bequant.market.adapter.BequantAccountAdapter
import com.mycelium.bequant.market.viewmodel.AccountViewModel
import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentBequantAccountBinding
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.fragment_bequant_account.*
import kotlinx.android.synthetic.main.item_bequant_search.*
import java.math.BigDecimal

class AccountFragment : Fragment() {
    private lateinit var mbwManager: MbwManager
    val adapter = BequantAccountAdapter()
    private var balancesData = mutableListOf<Balance>()
    lateinit var viewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mbwManager = MbwManager.getInstance(requireContext())
        viewModel = ViewModelProviders.of(this).get(AccountViewModel::class.java)
        MbwManager.getEventBus().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantAccountBinding>(inflater, R.layout.fragment_bequant_account, container, false)
                    .apply {
                        viewModel = this@AccountFragment.viewModel
                        lifecycleOwner = this@AccountFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deposit.setOnClickListener {
            findNavController().navigate(MarketFragmentDirections.actionSelectCoin("deposit"))
        }
        withdraw.setOnClickListener {
            findNavController().navigate(MarketFragmentDirections.actionSelectCoin("withdraw"))
        }

        estBalanceCurrency.text = BequantPreference.getLastKnownBalance().currencySymbol
        hideZeroBalance.isChecked = BequantPreference.hideZeroBalance()
        hideZeroBalance.setOnCheckedChangeListener { _, checked ->
            BequantPreference.setHideZeroBalance(checked)
            updateList()
        }
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        list.adapter = adapter
        adapter.addCoinListener = {
            findNavController().navigate(MarketFragmentDirections.actionDeposit(it))
        }
        viewModel.privateMode.observe(viewLifecycleOwner, Observer {
            privateModeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    if (it) R.drawable.ic_bequant_visibility_off
                    else R.drawable.ic_bequant_visibility))
            if (it) {
                viewModel.totalBalance.value = HIDE_VALUE
                viewModel.totalBalanceFiat.value = "~$HIDE_VALUE"
            } else {
                viewModel.totalBalance.value = BequantPreference.getLastKnownBalance().toString(Denomination.UNIT)
                viewModel.totalBalanceFiat.value = mbwManager.exchangeRateManager
                        .get(BequantPreference.getLastKnownBalance(), FiatType("USD"))?.toStringWithUnit(Denomination.UNIT)
            }
            updateList()
        })

        viewModel.tradingBalances.observe(viewLifecycleOwner, Observer<Array<Balance>> {
            updateBalanceData()
            updateBalances()
        })

        viewModel.accountBalances.observe(viewLifecycleOwner, Observer<Array<Balance>> {
            updateBalanceData()
            updateBalances()
        })

        privateModeButton.setOnClickListener {
            viewModel.privateMode.value = !(viewModel.privateMode.value ?: false)
        }
        search.doOnTextChanged { text, _, _, _ ->
            updateList(text?.toString() ?: "")
        }
        clear.setOnClickListener {
            viewModel.searchMode.value = false
            search.text = null
            updateList()
        }
        searchButton.setOnClickListener {
            viewModel.searchMode.value = true
            updateList()
        }
    }

    @Subscribe
    fun onNewTradingBalance(balance: TradingBalance) {
        viewModel.tradingBalances.value = balance.balances
    }

    @Subscribe
    fun onNewAccountBalance(balance: AccountBalance) {
        viewModel.accountBalances.value = balance.balances
    }

    private fun updateBalanceData() {
        val accountBalances = viewModel.accountBalances.value
        val tradingAccounts = viewModel.tradingBalances.value

        val totalBalances = mutableListOf<Balance>()
        totalBalances.addAll(accountBalances?.toList() ?: emptyList())
        totalBalances.addAll(tradingAccounts?.toList() ?: emptyList())

        val result = mutableListOf<Balance>()
        for ((currency, balances) in totalBalances.groupBy { it.currency }) {
            val currencySum = balances.map {
                BigDecimal(it.available) + BigDecimal(it.reserved)
            }.reduceRight { bigDecimal, acc -> acc.plus(bigDecimal) }
            result.add(Balance(currency, currencySum.toPlainString(), BigDecimal.ZERO.toPlainString()))
        }
        balancesData = result
        updateList()
    }

    private fun updateBalances() {
        val accountBalances = viewModel.accountBalances.value
        val tradingAccounts = viewModel.tradingBalances.value

        val totalBalances = mutableListOf<Balance>()
        totalBalances.addAll(accountBalances?.toList() ?: emptyList())
        totalBalances.addAll(tradingAccounts?.toList() ?: emptyList())

        var btcTotal = BigDecimal.ZERO
        var fiatTotal = BigDecimal.ZERO
        for ((currency, balances) in totalBalances.groupBy { it.currency }) {
            //for demo
            if (currency?.toUpperCase() != "BTC") {
                continue
            }
//            val btcRate = exchangeRateManager.getExchangeRate(currency!!, "BTC")
            val usdRate = mbwManager.exchangeRateManager.getExchangeRate(currency, "USD")
            btcTotal = balances.map { BigDecimal(it.available) }.reduceRight { bigDecimal, acc -> acc.plus(bigDecimal) }
            fiatTotal = btcTotal.multiply(BigDecimal.valueOf(usdRate?.price!!))
        }

        viewModel.totalBalance.value = btcTotal.toPlainString()
        viewModel.totalBalanceFiat.value = fiatTotal.setScale(2, BigDecimal.ROUND_CEILING).toPlainString() + " USD"
    }

    override fun onDestroyView() {
        list.adapter = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MbwManager.getEventBus().unregister(this)
        super.onDestroy()
    }

    private fun updateList(filter: String = "") {
        adapter.submitList(balancesData
                .filter { !BequantPreference.hideZeroBalance() || it.available != "0" }
                .map {
                    AccountItem(TYPE_ITEM, it.currency!!, it.currency,
                            if (viewModel.privateMode.value == true) HIDE_VALUE else it.available!!)
                }
                .filter { it.name.contains(filter, true) || it.symbol.contains(filter, true) })
    }
}