package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.mycelium.wallet.databinding.FragmentBequantWithdrawMyceliumWalletBinding

class WithdrawWalletFragment : Fragment(R.layout.fragment_bequant_withdraw_mycelium_wallet) {
    var parentViewModel: WithdrawViewModel? = null
    val adapter = AccountPagerAdapter()
    val mbwManager by lazy { MbwManager.getInstance(requireContext()) }
    val accounts by lazy {
        mbwManager.getWalletManager(false).getAllActiveAccounts()
                .filter { it !is InvestmentAccount }
                .filter { it.coinType.symbol == parentViewModel?.currency?.value }
    }
    private var binding: FragmentBequantWithdrawMyceliumWalletBinding? = null
    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            // not needed
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // not needed
        }

        override fun onPageSelected(position: Int) {
            parentViewModel!!.address.value = accounts[position].receiveAddress.toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentBequantWithdrawMyceliumWalletBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.layout?.accountList?.adapter = adapter
        binding?.layout?.accountList?.registerOnPageChangeCallback(onPageChangeCallback)
        TabLayoutMediator(binding?.layout?.accountListTab!!, binding?.layout?.accountList!!) { _, _ ->
        }.attach()

        adapter.submitList(accounts)

        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer {
            if (adapter.currentList != accounts) {
                adapter.submitList(accounts)
            }
        })

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<SelectAccountFragment.AccountData>(SelectAccountFragment.ACCOUNT_KEY)
            ?.observe(viewLifecycleOwner, Observer {
                val account = it
                val selectedAccount = mbwManager.getWalletManager(false).getAllActiveAccounts()
                    .find { it.label == account?.label }
                val pageToSelect = accounts.indexOf(selectedAccount)
                if (binding?.layout?.accountList?.currentItem != pageToSelect) {
                    Handler(Looper.getMainLooper()).post {
                        binding?.layout?.accountList?.setCurrentItem(pageToSelect, true)
                    }
                }
            })

        binding?.layout?.selectAccountMore?.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount(parentViewModel?.currency?.value))
        }
    }

    override fun onDestroyView() {
        binding?.layout?.accountList?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding = null
        super.onDestroyView()
    }
}