package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
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
            parentViewModel!!.address.value = accounts[position].receiveAddress.toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountList.adapter = adapter
        accountList.registerOnPageChangeCallback(onPageChangeCallback)
        TabLayoutMediator(accountListTab, accountList) { tab, _ ->
        }.attach()

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<SelectAccountFragment.AccountData>(SelectAccountFragment.ACCOUNT_KEY)?.observe(viewLifecycleOwner, Observer {
            val account = it
            val selectedAccount = mbwManager.getWalletManager(false).getAllActiveAccounts().find { it.label == account?.label }
            val pageToSelect = accounts.indexOf(selectedAccount)
            if (accountList.currentItem != pageToSelect) {
                accountList.currentItem = pageToSelect
            }
        })
        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer { coinSymbol ->
            adapter.submitList(accounts)
        })

        selectAccountMore.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount(parentViewModel?.currency?.value))
        }

    }

    override fun onDestroyView() {
        accountList.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }
}