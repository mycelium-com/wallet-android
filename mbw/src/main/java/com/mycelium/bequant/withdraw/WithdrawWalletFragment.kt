package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.mrd.bitlib.model.Address
import com.mycelium.bequant.InvestmentAccount
import com.mycelium.bequant.receive.SelectAccountFragment
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_withdraw_pager_accounts.*

class WithdrawWalletFragment : Fragment(R.layout.fragment_bequant_withdraw_mycelium_wallet) {
    var parentViewModel: WithdrawViewModel? = null
    val adapter = AccountPagerAdapter()
    val mbwManager by lazy{ MbwManager.getInstance(requireContext()) }
    val accounts by lazy {
        mbwManager.getWalletManager(false).getAllActiveAccounts()
                .filter { it !is InvestmentAccount }
                .filter { it.coinType.symbol == parentViewModel?.currency?.value }
    }
    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        }

        override fun onPageSelected(position: Int) {
            parentViewModel!!.address.value = Address(accounts[position].receiveAddress.getBytes()).toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountList.adapter = adapter
        TabLayoutMediator(accountListTab, accountList) { tab, _ ->
        }.attach()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<SelectAccountFragment.AccountData>(SelectAccountFragment.ACCOUNT_KEY)?.observe(viewLifecycleOwner, Observer {
            val account = it
            val selectedAccount = mbwManager.getWalletManager(false).getAllActiveAccounts().find { it.label == account?.label }
            Handler(Looper.getMainLooper()).post {
                adapter.submitList(listOf(selectedAccount))
            }
        })
        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer { coinSymbol ->
            adapter.submitList(accounts)
            accountList.registerOnPageChangeCallback(onPageChangeCallback)
        })

        selectAccountMore.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount())
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        accountList.unregisterOnPageChangeCallback(onPageChangeCallback)
    }
}