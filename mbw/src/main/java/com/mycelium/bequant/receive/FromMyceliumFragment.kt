package com.mycelium.bequant.receive

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.bequant.receive.viewmodel.FromMyceliumViewModel
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel
import com.mycelium.bequant.withdraw.WithdrawFragmentDirections
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.send.SendInitializationActivity
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.databinding.FragmentBequantReceiveFromMyceliumBinding
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.content.eth.EthUri
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import java.math.BigDecimal

class FromMyceliumFragment : Fragment() {
    val viewModel: FromMyceliumViewModel by viewModels()
    var parentViewModel: ReceiveCommonViewModel? = null
    val adapter = AccountPagerAdapter()

    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    var binding :FragmentBequantReceiveFromMyceliumBinding?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantReceiveFromMyceliumBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@FromMyceliumFragment.viewModel
                        parentViewModel = this@FromMyceliumFragment.parentViewModel
                        lifecycleOwner = this@FromMyceliumFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findNavController().currentBackStackEntry?.savedStateHandle
                ?.getLiveData<SelectAccountFragment.AccountData>(SelectAccountFragment.ACCOUNT_KEY)
                ?.observe(viewLifecycleOwner, Observer { account ->
            val selectedAccount = mbwManager.getWalletManager(false)
                    .getAllActiveAccounts().find { it.label == account?.label }
            Handler(Looper.getMainLooper()).post {
                adapter.submitList(listOf(selectedAccount))
            }
        })
        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer { coinSymbol ->
            val accounts = mbwManager.getWalletManager(false)
                    .getActiveSpendingAccounts()
                    .filter { it.coinType.symbol == coinSymbol }
            adapter.submitList(accounts)

            if (mbwManager.hasFiatCurrency() && accounts.isNotEmpty()) {
                val coin = accounts[0].coinType
                val value = mbwManager.exchangeRateManager.get(coin.oneCoin(), mbwManager.getFiatCurrency(coin))
                if (value == null) {
                    viewModel.oneCoinFiatRate.value = getString(R.string.exchange_source_not_available
                            , mbwManager.exchangeRateManager.getCurrentExchangeSourceName(coin.symbol))
                } else {
                    viewModel.oneCoinFiatRate.value = resources.getString(R.string.balance_rate
                            , coin.symbol, mbwManager.getFiatCurrency(coin).symbol, value.toString())
                }
            }
        })
        binding?.withdraw?.accountList?.adapter = adapter
        TabLayoutMediator(binding?.withdraw?.accountListTab!!, binding?.withdraw?.accountList!!) { _, _ ->
        }.attach()

        val selectorItems = viewModel.getCryptocurrenciesSymbols()
        val coinAdapter = ArrayAdapter(requireContext(),
                R.layout.item_bequant_coin, R.id.text, selectorItems)
        coinAdapter.setDropDownViewResource(R.layout.item_bequant_coin_selector)
        binding?.coinSelector?.adapter = coinAdapter

        binding?.coinSelector?.setSelection(selectorItems.indexOf(parentViewModel?.currency?.value))
        binding?.coinSelector?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                // ignore
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                if (parentViewModel?.currency?.value != coinAdapter.getItem(position)) {
                    parentViewModel?.currency?.value = coinAdapter.getItem(position)
                }
            }
        }
        binding?.confirm?.setOnClickListener {
            val account = adapter.getItem(binding?.withdraw?.accountList?.currentItem ?: 0)

            val addressString = parentViewModel?.address?.value ?: ""
            val uri: AssetUri = when (account) {
                is AbstractBtcAccount -> {
                    val type = Utils.getBtcCoinType()
                    BitcoinUri.from(BtcAddress(type, BitcoinAddress.fromString(addressString)),
                            Value.parse(type, viewModel.amount.value!!),
                            null, null)
                }
                is EthAccount -> {
                    val type = Utils.getEthCoinType()
                    EthUri(EthAddress(type, addressString),
                            Value.parse(type, viewModel.amount.value!!), null)
                }
                else -> TODO("Not supported account: $it")
            }

            SendInitializationActivity.callMe(requireActivity(), account.id, uri, false);
        }
//        loader(true)
//        viewModel.loadBalance("") {
//            loader(false)
//        }
        binding?.withdraw?.selectAccountMore?.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount(parentViewModel?.currency?.value))
        }
        viewModel.custodialBalance.value = BequantPreference.getLastKnownBalance().toString(Denomination.UNIT)

        viewModel.amount.observe(viewLifecycleOwner) {
            updateAmount(it)
        }
    }

    private fun updateAmount(amountAsString: String) {
        val account = adapter.currentList.first()
        val amount = amountAsString.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val enoughAmount = amount < account.accountBalance.confirmed.valueAsBigDecimal && amount > 0.toBigDecimal()
        binding?.edAmount?.error = if (enoughAmount) null else getString(R.string.insufficient_funds)
        binding?.confirm?.isEnabled = enoughAmount
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (this.isVisible) {
            if (!isVisibleToUser) {
                binding?.edAmount?.error = null
            } else {
                updateAmount(viewModel.amount.value ?: "")
            }
        }
    }

    override fun onDestroyView() {
        binding?.withdraw?.accountList?.adapter = null
        binding = null
        super.onDestroyView()
    }
}