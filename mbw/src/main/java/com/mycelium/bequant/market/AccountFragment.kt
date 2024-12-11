package com.mycelium.bequant.market

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.BequantConstants.HIDE_VALUE
import com.mycelium.bequant.BequantConstants.TYPE_ITEM
import com.mycelium.bequant.common.ModalDialog
import com.mycelium.bequant.kyc.BequantKycActivity
import com.mycelium.bequant.market.adapter.AccountItem
import com.mycelium.bequant.market.adapter.BequantAccountAdapter
import com.mycelium.bequant.market.viewmodel.AccountViewModel
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.bequant.sign.SignActivity
import com.mycelium.bequant.signup.TwoFactorActivity
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentBequantAccountBinding
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import com.squareup.otto.Subscribe
import java.math.BigDecimal

class AccountFragment : Fragment() {
    private lateinit var mbwManager: MbwManager
    val adapter = BequantAccountAdapter()
    private var balancesData = mutableListOf<Balance>()
    val viewModel: AccountViewModel by viewModels()
    private var binding: FragmentBequantAccountBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mbwManager = MbwManager.getInstance(requireContext())
        MbwManager.getEventBus().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantAccountBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@AccountFragment.viewModel
                        lifecycleOwner = this@AccountFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isDemo = activity?.intent?.getBooleanExtra(BequantMarketActivity.IS_DEMO_KEY, false)!!
        fun askEnable2Fa(@StringRes title: Int) {
            ModalDialog(getString(title),
                    getString(R.string.bequant_enable_2fa),
                    getString(R.string.secure_your_account)) {
                startActivity(Intent(requireActivity(), TwoFactorActivity::class.java))
            }.show(childFragmentManager, "modal_dialog")
        }

        fun askDoKyc() {
            ModalDialog(getString(R.string.bequant_kyc_verify_title),
                    getString(R.string.bequant_kyc_verify_message),
                    getString(R.string.bequant_kyc_verify_button)) {
                startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
            }.show(childFragmentManager, "modal_dialog")
        }
        binding?.deposit?.setOnClickListener {
            if (isDemo) {
                startActivity(Intent(requireActivity(), SignActivity::class.java))
            } else if (!BequantPreference.hasKeys()) {
                askEnable2Fa(R.string.bequant_turn_2fa_deposit)
            } else if (BequantPreference.getKYCStatus() != KYCStatus.VERIFIED) {
                askDoKyc()
            } else {
                findNavController().navigate(MarketFragmentDirections.actionSelectCoin("deposit"))
            }
        }
        binding?.withdraw?.setOnClickListener {
            if (isDemo) {
                startActivity(Intent(requireActivity(), SignActivity::class.java))
            } else if (!BequantPreference.hasKeys()) {
                askEnable2Fa(R.string.bequant_turn_2fa_withdraw)
            } else if (BequantPreference.getKYCStatus() != KYCStatus.VERIFIED) {
                askDoKyc()
            } else {
                findNavController().navigate(MarketFragmentDirections.actionSelectCoin("withdraw"))
            }
        }

        binding?.estBalanceCurrency?.text = BequantPreference.getLastKnownBalance().currencySymbol
        binding?.hideZeroBalance?.isChecked = BequantPreference.hideZeroBalance()
        binding?.hideZeroBalance?.setOnCheckedChangeListener { _, checked ->
            BequantPreference.setHideZeroBalance(checked)
            updateList()
        }
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        binding?.list?.adapter = adapter
        adapter.addCoinListener = {
            if (BequantPreference.getKYCStatus() != KYCStatus.VERIFIED) {
                askDoKyc()
            } else {
                when (it) {
                    "EURB", "USDB", "GBPB" -> {
                        AlertDialog.Builder(requireContext())
                                .setMessage(getString(R.string.bequant_fiat_tx_not_supported))
                                .setPositiveButton(R.string.button_ok) { _, _ ->
                                }.show()
                    }
                    else -> findNavController().navigate(MarketFragmentDirections.actionDeposit(it))
                }
            }
        }
        viewModel.privateMode.observe(viewLifecycleOwner, Observer {
            binding?.privateModeButton?.setImageDrawable(ContextCompat.getDrawable(requireContext(),
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

        binding?.privateModeButton?.setOnClickListener {
            viewModel.privateMode.value = !(viewModel.privateMode.value ?: false)
        }
        binding?.searchBar?.search?.doOnTextChanged { text, _, _, _ ->
            updateList(text?.toString() ?: "")
        }
        binding?.searchBar?.clear?.setOnClickListener {
            viewModel.searchMode.value = false
            binding?.searchBar?.search?.text = null
            updateList()
        }
        binding?.searchButton?.setOnClickListener {
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
            val usdRate = mbwManager.exchangeRateManager.getExchangeRate(currency, "USD")
            btcTotal = balances.map { BigDecimal(it.available) }.reduceRight { bigDecimal, acc -> acc.plus(bigDecimal) }
            fiatTotal = btcTotal.multiply(BigDecimal.valueOf(usdRate?.price!!))
        }

        viewModel.totalBalance.value = btcTotal.toPlainString()
        viewModel.totalBalanceFiat.value = fiatTotal.setScale(2, BigDecimal.ROUND_CEILING).toPlainString() + " USD"
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
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
                    AccountItem(TYPE_ITEM, it.currency!!, it.currency!!,
                            if (viewModel.privateMode.value == true) HIDE_VALUE else it.available!!)
                }
                .filter { it.name.contains(filter, true) || it.symbol.contains(filter, true) })
    }
}