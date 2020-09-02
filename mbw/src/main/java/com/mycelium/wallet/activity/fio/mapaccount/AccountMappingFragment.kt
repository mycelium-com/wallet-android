package com.mycelium.wallet.activity.fio.mapaccount

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountMappingAdapter
import com.mycelium.wallet.activity.fio.mapaccount.adapter.Item
import com.mycelium.wallet.activity.fio.mapaccount.adapter.ItemAccount
import com.mycelium.wallet.activity.fio.mapaccount.adapter.ItemGroup
import com.mycelium.wallet.activity.util.getActiveBTCSingleAddressAccounts
import com.mycelium.wapi.wallet.btc.bip44.getActiveHDAccounts
import com.mycelium.wapi.wallet.eth.getActiveEthAccounts
import com.mycelium.wapi.wallet.fio.getActiveFioAccounts
import kotlinx.android.synthetic.main.fragment_fio_account_mapping.*


class AccountMappingFragment : Fragment(R.layout.fragment_fio_account_mapping) {
    val adapter = AccountMappingAdapter()
    val data = mutableListOf<Item>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = "Select Accounts to map to FIO Account"
        }
        val mbwManager = MbwManager.getInstance(requireContext())
        val walletManager = mbwManager.getWalletManager(false)
        data.clear()
        data.apply {
            walletManager.getActiveHDAccounts().map { ItemAccount(it.id, it.label, it.receiveAddress.toString()) }.apply {
                if (this.isNotEmpty()) {
                    add(ItemGroup(getString(R.string.active_hd_accounts_name)))
                    addAll(this)
                }
            }
            walletManager.getActiveBTCSingleAddressAccounts().map { ItemAccount(it.id, it.label) }.apply {
                if (this.isNotEmpty()) {
                    add(ItemGroup(getString(R.string.active_bitcoin_sa_group_name)))
                    addAll(this)
                }
            }
            walletManager.getActiveEthAccounts().map { ItemAccount(it.id, it.label) }.apply {
                if (this.isNotEmpty()) {
                    add(ItemGroup(getString(R.string.eth_accounts_name)))
                    addAll(this)
                }
            }
            walletManager.getActiveFioAccounts().map { ItemAccount(it.id, it.label) }.apply {
                if (this.isNotEmpty()) {
                    add(ItemGroup(getString(R.string.fio_accounts_name)))
                    addAll(this)
                }
            }
        }
        adapter.submitList(data)
        buttonContinue.setOnClickListener {
            findNavController().navigate(R.id.actionNext, Bundle().apply {
                putStringArray("accounts", data.filterIsInstance<ItemAccount>().filter { it.isEnabled }.map { it.accountId.toString() }.toTypedArray())
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_fio_map_account, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterItems(newText)
                return true
            }
        })
    }

    private fun filterItems(newText: String?) {
        if (newText != null && newText.isNotEmpty()) {
            adapter.submitList(data.filter { it is ItemAccount && it.label.contains(newText, true) })
        } else {
            adapter.submitList(data)
        }
    }
}