package com.mycelium.wallet.activity.fio.mapaccount

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.mycelium.wallet.databinding.FragmentFioNameDetailsBinding
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import com.mycelium.wapi.wallet.fio.getFioAccounts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FIONameListFragment : Fragment(R.layout.fragment_fio_name_details) {
    val adapter = AccountNamesAdapter()
    private val viewModel: FIOMapPubAddressViewModel by activityViewModels()
    private lateinit var preference: SharedPreferences
    private lateinit var walletManager: WalletManager
    var binding: FragmentFioNameDetailsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preference = requireContext().getSharedPreferences("fio_name_details_preference", Context.MODE_PRIVATE)
        walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        if (arguments?.containsKey("fioName") == true) {
            findNavController().navigate(FIONameListFragmentDirections.actionName(requireArguments().getSerializable("fioName") as RegisteredFIOName))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentFioNameDetailsBinding.inflate(inflater, container, false).apply {
        binding = this
    }.root

    override fun onResume() {
        updateList(preference, walletManager)
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.my_fio_names)
        }
        binding?.list?.adapter = adapter
        binding?.list?.itemAnimator = null
        binding?.list?.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        when (viewModel.mode.value) {
            Mode.NEED_FIO_NAME_MAPPING -> {
                binding?.registeredOn?.text = getString(R.string.fio_manage_name_need_mapping_s, viewModel.extraAccount.value?.label)
            }
            else -> {
                binding?.registeredOn?.text = getString(R.string.fio_manage_name_and_domain)
            }
        }
        adapter.fioNameClickListener = {
            findNavController().navigate(FIONameListFragmentDirections.actionName(it))
        }
        adapter.domainClickListener = {
            findNavController().navigate(FIONameListFragmentDirections.actionDomain(it))
        }
        adapter.switchGroupVisibilityListener = {
            preference.edit().putBoolean("isClosed${it}", !preference.getBoolean("isClosed${it}", true)).apply()
            updateList(preference, walletManager)
        }
        adapter.registerFIONameListener = {
            RegisterFioNameActivity.start(requireContext(),
                    MbwManager.getInstance(requireContext()).selectedAccount.id)
        }
        adapter.registerFIODomainListener = {
            RegisterFIODomainActivity.start(requireContext(),
                    MbwManager.getInstance(requireContext()).selectedAccount.id)
        }
        updateList(preference, walletManager)
    }

    private fun updateList(preference: SharedPreferences, walletManager: WalletManager) {
        CoroutineScope(Dispatchers.IO).launch {
            adapter.submitList(mutableListOf<Item>().apply {
                val accounts =
                        if (viewModel.accountList.value != null) viewModel.accountList.value!! else walletManager.getFioAccounts()
                accounts.forEach { account ->
                    val isClosed = preference.getBoolean("isClosed${account.label}", true)
                    add(AccountItem(account, isClosed))
                    if (isClosed) {
                        if (viewModel.mode.value != Mode.NEED_FIO_NAME_MAPPING) {
                            if (account.canSpend()) {
                                add(RegisterFIONameItem(account))
                                add(RegisterFIODomainItem(account))
                            }
                            addAll(account.registeredFIODomains.map { FIODomainItem(it) })
                        }
                        addAll(account.registeredFIONames.map { FIONameItem(it) })
                    }
                }
            })
        }
    }
}