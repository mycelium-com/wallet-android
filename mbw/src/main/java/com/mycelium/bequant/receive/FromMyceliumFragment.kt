package com.mycelium.bequant.receive

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.mrd.bitlib.model.Address
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
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.content.eth.EthUri
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import kotlinx.android.synthetic.main.fragment_bequant_receive_from_mycelium.*
import kotlinx.android.synthetic.main.item_bequant_withdraw_pager_accounts.*
import java.math.BigDecimal

class FromMyceliumFragment : Fragment() {

    lateinit var viewModel: FromMyceliumViewModel
    var parentViewModel: ReceiveCommonViewModel? = null
    val adapter = AccountPagerAdapter()

    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(FromMyceliumViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantReceiveFromMyceliumBinding>(inflater, R.layout.fragment_bequant_receive_from_mycelium, container, false)
                    .apply {
                        viewModel = this@FromMyceliumFragment.viewModel
                        parentViewModel = this@FromMyceliumFragment.parentViewModel
                        lifecycleOwner = this@FromMyceliumFragment
                    }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mbwManager = MbwManager.getInstance(requireContext())
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<SelectAccountFragment.AccountData>(SelectAccountFragment.ACCOUNT_KEY)?.observe(viewLifecycleOwner, Observer {
            val account = it
            val selectedAccount = mbwManager.getWalletManager(false).getAllActiveAccounts().find { it.label == account?.label }
            Handler(Looper.getMainLooper()).post {
                adapter.submitList(listOf(selectedAccount))
            }
        })
        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer { coinSymbol ->
            val accounts = mbwManager.getWalletManager(false).getActiveSpendingAccounts()
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
        accountList.adapter = adapter
        TabLayoutMediator(accountListTab, accountList) { tab, _ ->
        }.attach()

        val selectorItems = viewModel.getCryptocurrenciesSymbols()
        val coinAdapter = ArrayAdapter(requireContext(),
                R.layout.item_bequant_coin, R.id.text, selectorItems)
        coinAdapter.setDropDownViewResource(R.layout.item_bequant_coin_selector)
        coinSelector.adapter = coinAdapter

        coinSelector.setSelection(selectorItems.indexOf(parentViewModel?.currency?.value))
        coinSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                if (parentViewModel?.currency?.value != coinAdapter.getItem(position)) {
                    parentViewModel?.currency?.value = coinAdapter.getItem(position)
                }
            }
        }
        confirm.setOnClickListener {
            val account = adapter.getItem(accountList.currentItem)

            val addressString = parentViewModel?.address?.value ?: ""
            val uri: GenericAssetUri = when (account) {
                is AbstractBtcAccount -> {
                    val type = Utils.getBtcCoinType()
                    BitcoinUri.from(BtcAddress(type, Address.fromString(addressString)),
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

            SendInitializationActivity.callMe(activity, account.id, uri, false);
        }
//        loader(true)
//        viewModel.loadBalance("") {
//            loader(false)
//        }
        selectAccountMore.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount())
        }
        viewModel.castodialBalance.value = BequantPreference.getLastKnownBalance().toString(Denomination.UNIT)

        viewModel.amount.observe(viewLifecycleOwner) {
            val account = adapter.currentList.first()
            val amount = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val enoughAmount = amount < account.accountBalance.confirmed.valueAsBigDecimal && amount > 0.toBigDecimal()
            edAmount.error = if (enoughAmount) null else getString(R.string.insufficient_funds)
            confirm.isEnabled = enoughAmount
        }
    }

}