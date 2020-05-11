package com.mycelium.bequant.receive

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.bequant.receive.viewmodel.FromMyceliumViewModel
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel
import com.mycelium.bequant.withdraw.WithdrawFragmentDirections
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.databinding.FragmentBequantReceiveFromMyceliumBinding
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_bequant_receive_from_mycelium.*
import kotlinx.android.synthetic.main.item_bequant_withdraw_pager_accounts.*

class FromMyceliumFragment : Fragment() {

    lateinit var viewModel: FromMyceliumViewModel
    var parentViewModel: ReceiveCommonViewModel? = null
    val adapter = AccountPagerAdapter()

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
        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer { coinSymbol ->
            val accounts = mbwManager.getWalletManager(false).getSpendingAccountsWithBalance()
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

        val selectorItems = mbwManager.getWalletManager(false).getCryptocurrenciesSymbols()
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
            if (viewModel.amount.value != null) {
                val value = Value.parse(Utils.getBtcCoinType(), viewModel.amount.value!!)
                val account = adapter.getItem(accountList.currentItem)
                val address = mbwManager.getWalletManager(false)
                        .parseAddress(if (mbwManager.network.isProdnet) viewModel.address.value!! else Constants.TEST_ADDRESS)
                SendCoinTask(this, account, address[0], value,
                        FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0.00000001")))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
        selectAccountMore.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount())
        }
        viewModel.castodialBalance.value = BequantPreference.getMockCastodialBalance().valueAsBigDecimal.stripTrailingZeros().toString()
    }

    class SendCoinTask(val fragment: Fragment,
                       val account: WalletAccount<*>,
                       val address: GenericAddress,
                       val value: Value,
                       val fee: GenericFee) : AsyncTask<Void, Int, GenericTransaction>() {


        override fun onPreExecute() {
            super.onPreExecute()
            fragment.loader(true)
        }

        override fun doInBackground(vararg p0: Void?): GenericTransaction {
            val fee = FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0"))
            val tx = account.createTx(address, value, fee)
            account.signTx(tx, AesKeyCipher.defaultKeyCipher())
            return tx
        }

        override fun onPostExecute(result: GenericTransaction?) {
            super.onPostExecute(result)
            fragment.loader(false)
            BroadcastDialog.create(account, false, result!!)
            BequantPreference.setMockCastodialBalance(BequantPreference.getMockCastodialBalance().plus(value))
            fragment.findNavController().popBackStack()
        }
    }
}