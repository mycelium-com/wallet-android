package com.mycelium.wallet.activity.fio.mapaccount

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.*
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.FIOMapPubAddressViewModel
import com.mycelium.wallet.activity.fio.registerdomain.RegisterFIODomainActivity
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import com.mycelium.wapi.wallet.fio.getFioAccounts
import kotlinx.android.synthetic.main.fragment_fio_name_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FIONameListFragment : Fragment(R.layout.fragment_fio_name_details) {
    val adapter = AccountNamesAdapter()
    private val viewModel: FIOMapPubAddressViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments?.containsKey("fioName") == true) {
            findNavController().navigate(FIONameListFragmentDirections.actionName(requireArguments().getSerializable("fioName") as RegisteredFIOName))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.my_fio_names)
        }
        list.adapter = adapter
        list.itemAnimator = null
        list.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        when (viewModel.mode.value) {
            Mode.NEED_FIO_NAME_MAPPING -> {
                registeredOn.text = getString(R.string.fio_manage_name_need_mapping_s, viewModel.extraAccount.value?.label)
            }
            else -> {
                registeredOn.text = getString(R.string.fio_manage_name_and_domain)
            }
        }
        adapter.fioNameClickListener = {
            findNavController().navigate(FIONameListFragmentDirections.actionName(it))
        }
        adapter.domainClickListener = {
            findNavController().navigate(FIONameListFragmentDirections.actionDomain(it))
        }
        val preference = requireContext().getSharedPreferences("fio_name_details_preference", Context.MODE_PRIVATE)
        adapter.switchGroupVisibilityListener = {
            preference.edit().putBoolean("isClosed${it}", !preference.getBoolean("isClosed${it}", true)).apply()
            updateList(preference, walletManager)
        }
        adapter.registerFIONameListener = {
            startActivity(Intent(requireActivity(), RegisterFioNameActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
        adapter.registerFIODomainListener = {
            startActivity(Intent(requireActivity(), RegisterFIODomainActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
        updateList(preference, walletManager)
    }

    private fun updateList(preference: SharedPreferences, walletManager: WalletManager) {
        CoroutineScope(Dispatchers.IO).launch {
            adapter.submitList(mutableListOf<Item>().apply {
                val accounts =
                        if (viewModel.accountList.value != null) viewModel.accountList.value!! else walletManager.getFioAccounts()
                accounts.forEach {
                    val isClosed = preference.getBoolean("isClosed${it.label}", true)
                    add(AccountItem(it, isClosed))
                    if (isClosed) {
                        if(viewModel.mode.value != Mode.NEED_FIO_NAME_MAPPING) {
                            add(RegisterFIONameItem(it))
                            add(RegisterFIODomainItem(it))
                            addAll(it.registeredFIODomains.map { FIODomainItem(it) })
                        }
                        addAll(it.registeredFIONames.map { FIONameItem(it) })
                    }
                }
            })
        }
    }
}