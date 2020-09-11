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
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.mapaccount.adapter.*
import com.mycelium.wallet.activity.util.getActiveBTCSingleAddressAccounts
import com.mycelium.wapi.wallet.btc.bip44.getActiveHDAccounts
import com.mycelium.wapi.wallet.eth.getActiveEthAccounts
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

        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = "Select accounts for mapping"
        }
        val mbwManager = MbwManager.getInstance(requireContext())
        val walletManager = mbwManager.getWalletManager(false)
        title.text = getString(R.string.select_account_to_accociate, mbwManager.selectedAccount.label)
        list.adapter = adapter
        list.itemAnimator = null
        adapter.selectChangeListener = { accountItem ->
            if (accountItem.isEnabled) {
                data.filterIsInstance<ItemAccount>().filter { it.coinType == accountItem.coinType }.forEach {
                    if (it.accountId != accountItem.accountId) {
                        data[data.indexOf(it)] = ItemAccount(it.accountId, it.label, it.summary,
                                it.icon, it.coinType, false)
                    }
                }
            }
            adapter.submitList(data.toList())
        }
        data.clear()
        data.apply {
            val btcHDAccounts = walletManager.getActiveHDAccounts().map {
                ItemAccount(it.id, it.label, "",
                        Utils.getDrawableForAccount(it, false, resources),
                        it.coinType)
            }
            val btcSAAccounts = walletManager.getActiveBTCSingleAddressAccounts().map {
                ItemAccount(it.id, it.label, "",
                        Utils.getDrawableForAccount(it, false, resources),
                        it.coinType)
            }
            if (btcHDAccounts.isNotEmpty() || btcSAAccounts.isNotEmpty()) {
                add(ItemGroup(getString(R.string.bitcoin_name)))
                if (btcHDAccounts.isNotEmpty()) {
                    add(ItemSubGroup(getString(R.string.active_hd_accounts_name)))
                    addAll(btcHDAccounts)
                }
                if (btcSAAccounts.isNotEmpty()) {
                    add(ItemSubGroup(getString(R.string.active_bitcoin_sa_group_name)))
                    addAll(btcSAAccounts)
                }
            }
            walletManager.getActiveEthAccounts().map {
                ItemAccount(it.id, it.label, "",
                        Utils.getDrawableForAccount(it, false, resources), it.coinType)
            }.apply {
                if (this.isNotEmpty()) {
                    add(ItemGroup(getString(R.string.eth_accounts_name)))
                    addAll(this)
                }
            }
            add(ItemSpace)
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

    private fun filterItems(text: String?) {
        if (text != null && text.isNotEmpty()) {
            adapter.submitList(data.filter { it is ItemAccount && it.label.contains(text, true) } + ItemSpace)
        } else {
            adapter.submitList(data)
        }
    }
}