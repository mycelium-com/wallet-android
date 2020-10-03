package com.mycelium.wallet.activity.fio.mapaccount

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.mapaccount.adapter.*
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.AccountMappingViewModel
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.util.getActiveBTCSingleAddressAccounts
import com.mycelium.wallet.databinding.FragmentFioAccountMappingBinding
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.bip44.getActiveHDAccounts
import com.mycelium.wapi.wallet.eth.getActiveEthAccounts
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.fragment_fio_account_mapping.*


class FIONameDetailsFragment : Fragment() {
    private val viewModel: AccountMappingViewModel by viewModels()

    val adapter = AccountMappingAdapter()
    val data = mutableListOf<Item>()

    val args: FIONameDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentFioAccountMappingBinding>(inflater, R.layout.fragment_fio_account_mapping, container, false)
                    .apply {
                        viewModel = this@FIONameDetailsFragment.viewModel
                        lifecycleOwner = this@FIONameDetailsFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.fio_name_details)
        }
        val mbwManager = MbwManager.getInstance(requireContext())
        val walletManager = mbwManager.getWalletManager(false)
        list.adapter = adapter
        list.itemAnimator = null
        viewModel.fioName.value = args.fioName
        (walletManager.getModuleById(FioModule.ID) as FioModule).getFioAccountByFioName(args.fioName.name)?.run {
            viewModel.fioAccount.value = walletManager.getAccount(this) as FioAccount
        }
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
        val preference = requireContext().getSharedPreferences("fio_account_mapping_preference", Context.MODE_PRIVATE)
        adapter.groupClickListener = {
            preference.edit().putBoolean("isClosed$it", !preference.getBoolean("isClosed$it", false)).apply()
            updateList(walletManager, preference)
        }
        updateList(walletManager, preference)
        buttonContinue.setOnClickListener {
//            TODO("account mapping not implemented")
            findNavController().popBackStack()
//            findNavController().navigate(R.id.actionNext, Bundle().apply {
//                putStringArray("accounts", data.filterIsInstance<ItemAccount>().filter { it.isEnabled }.map { it.accountId.toString() }.toTypedArray())
//            })
        }
        renewFIOName.setOnClickListener {
            startActivity(Intent(requireActivity(), RegisterFioNameActivity::class.java)
                    .putExtra("account", MbwManager.getInstance(requireContext()).selectedAccount.id))
        }
    }

    private fun updateList(walletManager: WalletManager, preference: SharedPreferences) {
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
                val title = getString(R.string.bitcoin_name)
                val isClosed = preference.getBoolean("isClosed$title", false)
                add(ItemGroup(title, btcHDAccounts.size + btcSAAccounts.size, isClosed))
                if (btcHDAccounts.isNotEmpty() && !isClosed) {
                    add(ItemSubGroup(getString(R.string.active_hd_accounts_name)))
                    add(ItemDivider())
                    addAll(btcHDAccounts)
                    add(ItemDivider(resources.getDimensionPixelOffset(R.dimen.fio_mapping_group_margin)))
                }
                if (btcSAAccounts.isNotEmpty() && !isClosed) {
                    add(ItemSubGroup(getString(R.string.active_bitcoin_sa_group_name)))
                    add(ItemDivider())
                    addAll(btcSAAccounts)
                    add(ItemDivider(resources.getDimensionPixelOffset(R.dimen.fio_mapping_group_margin)))
                }
            }
            walletManager.getActiveEthAccounts().map {
                ItemAccount(it.id, it.label, "",
                        Utils.getDrawableForAccount(it, false, resources), it.coinType)
            }.apply {
                if (this.isNotEmpty()) {
                    val title = getString(R.string.ethereum_name)
                    val isClosed = preference.getBoolean("isClosed$title", false)
                    add(ItemGroup(title, this.size, isClosed))
                    if (!isClosed) {
                        add(ItemDivider())
                        addAll(this)
                        add(ItemDivider(resources.getDimensionPixelOffset(R.dimen.fio_mapping_group_margin)))
                    }
                }
            }
        }
        adapter.submitList(data.toList())
    }
}