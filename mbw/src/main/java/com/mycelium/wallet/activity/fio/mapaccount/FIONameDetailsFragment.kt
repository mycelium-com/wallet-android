package com.mycelium.wallet.activity.fio.mapaccount

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountItem
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountNamesAdapter
import com.mycelium.wallet.activity.fio.mapaccount.adapter.FIONameItem
import com.mycelium.wallet.activity.fio.mapaccount.adapter.Item
import com.mycelium.wallet.activity.fio.registerdomain.RegisterFIODomainActivity
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.fragment_fio_name_details.*


class FIONameDetailsFragment : Fragment(R.layout.fragment_fio_name_details) {
    val adapter = AccountNamesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.my_fio_names)
        }
        list.adapter = adapter
        list.itemAnimator = null
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        registeredOn.text = getString(R.string.following_fio_names_registered_on_s, MbwManager.getInstance(requireContext()).selectedAccount.label)
        val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        adapter.fioNameClickListener = {
            findNavController().navigate(R.id.actionNext)
        }
        val preference = requireContext().getSharedPreferences("fio_name_details_preference", Context.MODE_PRIVATE)
        adapter.switchGroupVisibilityListener = { fioName ->
            preference.edit().putBoolean("isClosed${fioName}", !preference.getBoolean("isClosed${fioName}", true)).apply()
            updateList(fioModule, preference, walletManager)
        }
        updateList(fioModule, preference, walletManager)
        addFIOName.setOnClickListener {
            startActivity(Intent(requireActivity(), RegisterFioNameActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
        addFIODomain.setOnClickListener {
            startActivity(Intent(requireActivity(), RegisterFIODomainActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
    }

    private fun updateList(fioModule: FioModule, preference: SharedPreferences, walletManager: WalletManager) {
        adapter.submitList(mutableListOf<Item>().apply {
            fioModule.getAllRegisteredFioNames().forEach { fioName ->
                val isClosed = preference.getBoolean("isClosed${fioName}", true)
                add(FIONameItem(fioName.name, 1, isClosed))
                if (isClosed) {
                    fioModule.getFioAccountByFioName(fioName.name)?.let {
                        walletManager.getAccount(it)?.let { account ->
                            add(AccountItem(account, "asasas"))
                        }
                    }
                }
            }
        })
    }
}