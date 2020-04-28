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
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.bequant.receive.viewmodel.FromMyceliumViewModel
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.databinding.FragmentBequantReceiveFromMyceliumBinding
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_bequant_receive_from_mycelium.*


class FromMyceliumFragment : Fragment() {

    lateinit var viewModel: FromMyceliumViewModel
    val adapter = AccountPagerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(FromMyceliumViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantReceiveFromMyceliumBinding>(inflater, R.layout.fragment_bequant_receive_from_mycelium, container, false)
                    .apply {
                        viewModel = this@FromMyceliumFragment.viewModel
                        lifecycleOwner = this@FromMyceliumFragment
                    }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mbwManager = MbwManager.getInstance(requireContext())
        adapter.submitList(mbwManager.getWalletManager(false).getSpendingAccountsWithBalance())
        fromAccounts.adapter = adapter
        TabLayoutMediator(fromAccountsTab, fromAccounts) { tab, _ ->
        }.attach()


        if (mbwManager.hasFiatCurrency()) {
            val coin = Utils.getBtcCoinType()
            val value = mbwManager.exchangeRateManager.get(coin.oneCoin(), mbwManager.getFiatCurrency(coin))
            if (value == null) {
                viewModel.oneCoinFiatRate.value = getString(R.string.exchange_source_not_available
                        , mbwManager.exchangeRateManager.getCurrentExchangeSourceName(coin.symbol))
            } else {
                oneCoinFiatRate.text = resources.getString(R.string.balance_rate
                        , coin.symbol, mbwManager.getFiatCurrency(coin).symbol, value.toString())
            }
        }

        viewModel.castodialBalance.value = "0"
        viewModel.coin.value = "ETH"
        viewModel.castodialLabel.value = "Custodial wallet (${viewModel.coin.value})"
        val coinAdapter = ArrayAdapter(requireContext(),
                R.layout.item_bequant_coin_selector, R.id.text,
                mbwManager.getWalletManager(false).getCryptocurrenciesSymbols())
        coinSelector.adapter = coinAdapter
        coinSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                viewModel.coin.value = coinAdapter.getItem(position)
                viewModel.castodialLabel.value = "Custodial wallet (${viewModel.coin.value})"
            }
        }
        confirm.setOnClickListener {
            val account = adapter.getItem(fromAccounts.currentItem)
            val address = mbwManager.getWalletManager(false).parseAddress(viewModel.address.value!!)
            val value = Value.parse(Utils.getBtcCoinType(), viewModel.amount.value!!)
            SendCoinTask(parentFragmentManager, account, address[0], value,
                    FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0")))
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        requestDepositAddress(viewModel.coin.value!!)
    }

    class SendCoinTask(val fragmentManager: FragmentManager,
                       val account: WalletAccount<*>,
                       val address: GenericAddress,
                       val value: Value,
                       val fee: GenericFee) : AsyncTask<Void, Int, GenericTransaction>() {
        val loader = LoaderFragment()

        override fun onPreExecute() {
            super.onPreExecute()
            loader.show(fragmentManager, Constants.LOADER_TAG)
        }

        override fun doInBackground(vararg p0: Void?): GenericTransaction {
            val fee = FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0"))
            val tx = account.createTx(address, value, fee)
            account.signTx(tx, AesKeyCipher.defaultKeyCipher())
            return tx
        }

        override fun onPostExecute(result: GenericTransaction?) {
            super.onPostExecute(result)
            BroadcastDialog.create(account, false, result!!)
        }
    }

    fun requestDepositAddress(currency: String) {
        val loader = LoaderFragment()
        loader.show(parentFragmentManager, Constants.LOADER_TAG)
        ApiRepository.repository.depositAddress(currency, {
            viewModel.address.value = it.address
        }, { code, message ->
            ErrorHandler(requireContext()).handle(message)
        })
    }
}