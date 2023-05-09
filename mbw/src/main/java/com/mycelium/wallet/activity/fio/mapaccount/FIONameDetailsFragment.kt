package com.mycelium.wallet.activity.fio.mapaccount

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.mapaccount.adapter.*
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.AccountMappingViewModel
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.FIOMapPubAddressViewModel
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.getActiveBTCSingleAddressAccounts
import com.mycelium.wallet.databinding.FragmentFioAccountMappingBinding
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.bip44.getActiveHDAccounts
import com.mycelium.wapi.wallet.erc20.getActiveERC20Accounts
import com.mycelium.wapi.wallet.eth.getActiveEthAccounts
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.fragment_fio_account_mapping.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FIONameDetailsFragment : Fragment() {
    private val globalViewModel: FIOMapPubAddressViewModel by activityViewModels()
    private val viewModel: AccountMappingViewModel by viewModels()

    val adapter = AccountMappingAdapter()
    val data = mutableListOf<Item>()
    private lateinit var fioAccount: FioAccount
    private lateinit var walletManager: WalletManager
    private lateinit var fioModule: FioModule

    val args: FIONameDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentFioAccountMappingBinding>(inflater, R.layout.fragment_fio_account_mapping, container, false)
                    .apply {
                        viewModel = this@FIONameDetailsFragment.viewModel
                        lifecycleOwner = this@FIONameDetailsFragment
                    }.root

    override fun onResume() {
        setFioName()
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
    }

    private fun setFioName() {
        viewModel.fioName.value = fioModule.getFIONameInfo(args.fioName.name)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.fio_name_details)
        }
        list.adapter = adapter
        list.itemAnimator = null
        setFioName()
        fioAccount = fioModule.getFioAccountByFioName(args.fioName.name)!!.let { accountId ->
            walletManager.getAccount(accountId) as FioAccount
        }
        viewModel.fioAccount.value = fioAccount
        adapter.selectChangeListener = { accountItem ->
            data.filterIsInstance<ItemAccount>()
                    .filter { it.coinType == accountItem.coinType }
                    .forEach {
                        data[data.indexOf(it)] = ItemAccount(it.accountId, it.label, it.summary,
                                it.icon, it.coinType,
                                it.accountId == accountItem.accountId && accountItem.isEnabled)
                    }
            val enabledList = data.filterIsInstance<ItemAccount>().filter { it.isEnabled }
            acknowledge?.visibility = if (enabledList.isEmpty()) INVISIBLE else VISIBLE
            viewModel.acknowledge.value = enabledList.isEmpty()
            adapter.submitList(data.toList())
        }
        val preference = requireContext().getSharedPreferences("fio_account_mapping_preference", Context.MODE_PRIVATE)
        adapter.groupClickListener = {
            preference.edit().putBoolean("isClosed$it", !preference.getBoolean("isClosed$it", false)).apply()
            updateList(walletManager, preference)
        }
        updateList(walletManager, preference)
        llBundled.setOnClickListener {
            Toast.makeText(requireContext(),
                    "TODO: add an explanation of how FIO bundled transactions work.",
                    Toast.LENGTH_SHORT).show()
        }
        renewFIOName.setOnClickListener {
            val fioName = viewModel.fioName.value!!.name
            RegisterFioNameActivity.startRenew(requireContext(), fioAccount.id, fioName)
        }
        copy.setOnClickListener {
            val text = viewModel.fioName.value!!.name
            Utils.setClipboardString(text, context)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        buttonContinue.setOnClickListener {
            val accounts = adapter.currentList
                    .filterIsInstance<ItemAccount>()
                    .filter { it.isEnabled }
                    .mapNotNull { walletManager.getAccount(it.accountId) }
            GlobalScope.launch(Dispatchers.IO) {
                fioModule.mapFioNameToAccounts(args.fioName.name, accounts)
                if (isResumed && globalViewModel.mode.value == Mode.NEED_FIO_NAME_MAPPING &&
                        accounts.contains(globalViewModel.extraAccount.value)) {
                    withContext(Dispatchers.Main) {
                        activity?.run {
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }
            }
            if (accounts.isNotEmpty()) {
                // TODO this toast would be shown even in case of an error
                Toaster(this).toast("accounts connected", false)
            } else {
                Toaster(this).toast("accounts disconnected", false)
            }
            viewModel.acknowledge.value = false
        }
        viewModel.fioName.observe(viewLifecycleOwner) {
            viewModel.update()
        }
        tvConnectAccountsDesc.text = if (viewModel.fioAccount.value!!.canSpend()) {
            getText(R.string.select_name_to_associate)
        } else {
            getText(R.string.select_name_to_associate_read_only_account)
        }
    }

    private fun updateList(walletManager: WalletManager, preference: SharedPreferences) {
        val mappedAccounts = fioModule.getConnectedAccounts(args.fioName.name)
        data.apply {
            clear()
            addBitcoinAccounts(walletManager, mappedAccounts, preference)
            addEthAccounts(walletManager, mappedAccounts, preference)
            addErc20Accounts(walletManager, mappedAccounts, preference)
        }
        adapter.submitList(data.toList())
    }

    private fun MutableList<Item>.addBitcoinAccounts(
            walletManager: WalletManager,
            mappedAccounts: List<WalletAccount<*>>,
            preference: SharedPreferences) {
        val btcHDAccounts = walletManager.getActiveHDAccounts().map {
            ItemAccount(it.id, it.label, "",
                    Utils.getDrawableForAccount(it, false, resources),
                    it.coinType, mappedAccounts.contains(it))
        }
        val btcSAAccounts = walletManager.getActiveBTCSingleAddressAccounts().map {
            ItemAccount(it.id, it.label, "",
                    Utils.getDrawableForAccount(it, false, resources),
                    it.coinType, mappedAccounts.contains(it))
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
    }

    private fun MutableList<Item>.addEthAccounts(
            walletManager: WalletManager,
            mappedAccounts: List<WalletAccount<*>>,
            preference: SharedPreferences) {
        walletManager.getActiveEthAccounts().map {
            ItemAccount(it.id, it.label, "",
                    Utils.getDrawableForAccount(it, false, resources),
                    it.coinType, mappedAccounts.contains(it))
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

    private fun MutableList<Item>.addErc20Accounts(
            walletManager: WalletManager,
            mappedAccounts: List<WalletAccount<*>>,
            preference: SharedPreferences) {
        walletManager.getActiveERC20Accounts().map {
            it.coinType.symbol
        }.toSet().forEach { symbol ->
            addErc20Accounts(symbol, walletManager, mappedAccounts, preference)
        }
    }

    private fun MutableList<Item>.addErc20Accounts(
            symbol: String,
            walletManager: WalletManager,
            mappedAccounts: List<WalletAccount<*>>,
            preference: SharedPreferences) {
        walletManager.getActiveERC20Accounts().filter { it.coinType.symbol == symbol }.map {
            ItemAccount(it.id, "${it.label} <font color=\"#9E9FA0\">(${it.ethAcc.label})</font>", "",
                    Utils.getDrawableForAccount(it, false, resources),
                    it.coinType, mappedAccounts.contains(it))
        }.apply {
            if (isNotEmpty()) {
                val isClosed = preference.getBoolean("isClosed$symbol", false)
                add(ItemGroup(symbol, this.size, isClosed))
                if (!isClosed) {
                    add(ItemDivider())
                    addAll(this)
                    add(ItemDivider(resources.getDimensionPixelOffset(R.dimen.fio_mapping_group_margin)))
                }
            }
        }
    }
}