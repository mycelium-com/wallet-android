package com.mycelium.bequant.withdraw

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.InvestmentAccount
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.withdraw.adapter.WithdrawFragmentAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.FragmentBequantWithdrawBinding
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericFee
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_bequant_withdraw.*
import com.mycelium.wallet.activity.util.toString


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
        pager.adapter = WithdrawFragmentAdapter(this, viewModel)
        tabs.setupWithViewPager(pager)
        pager.offscreenPageLimit = 2
        viewModel.castodialBalance.value = BequantPreference.getMockCastodialBalance().toString(Denomination.UNIT)
        val mbwManager = MbwManager.getInstance(requireContext())
        send.setOnClickListener {
            if (viewModel.amount.value != null) {
                val value = Value.parse(Utils.getBtcCoinType(), viewModel.amount.value!!)
                val account = InvestmentAccount()
                val address = mbwManager.getWalletManager(false)
                        .parseAddress(if (mbwManager.network.isProdnet) viewModel.address.value!! else Constants.TEST_ADDRESS)
                SendCoinTask(this, account, address[0], value,
                        FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0.00000001")))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
    }

    class SendCoinTask(val fragment: Fragment,
                       val account: WalletAccount<*>,
                       val address: GenericAddress,
                       val value: Value,
                       val fee: GenericFee) : AsyncTask<Void, Int, GenericTransaction?>() {


        override fun onPreExecute() {
            super.onPreExecute()
            fragment.loader(true)
        }

        override fun doInBackground(vararg p0: Void?): GenericTransaction? {
            val fee = FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0"))
//            val tx = account.createTx(address, value, fee)
//            account.signTx(tx, AesKeyCipher.defaultKeyCipher())
            return null
        }

        override fun onPostExecute(result: GenericTransaction?) {
            super.onPostExecute(result)
            fragment.loader(false)
//            BroadcastDialog.create(account, false, result!!)
            BequantPreference.setMockCastodialBalance(BequantPreference.getMockCastodialBalance().minus(value))
            fragment.findNavController().popBackStack()
        }
    }
}