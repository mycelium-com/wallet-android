package com.mycelium.wallet.activity.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.mycelium.net.ServerEndpointType
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.main.address.AddressFragment
import com.mycelium.wallet.activity.modern.UnspentOutputsActivity
import com.mycelium.wallet.activity.rmc.RMCAddressFragment
import com.mycelium.wallet.databinding.BalanceMasterFragmentBinding
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wallet.event.TorStateChanged
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.squareup.otto.Subscribe

class BalanceMasterFragment : Fragment() {

    private var binding: BalanceMasterFragmentBinding? = null
    private val menuProvider = MenuImpl()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BalanceMasterFragmentBinding.inflate(inflater).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentTransaction = childFragmentManager.beginTransaction()
        val account = MbwManager.getInstance(this.activity).selectedAccount
        binding?.run {
            defineAddressAccountView(fragmentTransaction, account)
            fragmentTransaction.replace(phFragmentBalance.id, BalanceFragment())
            fragmentTransaction.replace(phFragmentNotice.id, NoticeFragment())
            fragmentTransaction.replace(phFragmentBuySell.id, BuySellFragment())
        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if(binding == null) {
            return
        }
        if (menuVisible) {
            requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        } else {
            requireActivity().removeMenuProvider(menuProvider)
        }
    }

    private fun BalanceMasterFragmentBinding.defineAddressAccountView(
        fragmentTransaction: FragmentTransaction,
        account: WalletAccount<*>
    ) {
        fragmentTransaction.replace(
            phFragmentAddress.id,
            if ((account.coinType === RMCCoin || account.coinType === RMCCoinTest)) RMCAddressFragment() else AddressFragment()
        )
    }

    private fun showOutputs() {
        val account = MbwManager.getInstance(this.activity).selectedAccount
        account.interruptSync()
        startActivity(
            Intent(activity, UnspentOutputsActivity::class.java)
                .putExtra("account", account.id)
        )
    }

    override fun onStart() {
        // Set build version
        binding?.tvBuildText?.text = resources.getString(
                R.string.build_text,
                BuildConfig.VERSION_NAME
            )

        val mbwManager = MbwManager.getInstance(requireContext())
        if (mbwManager.torMode == ServerEndpointType.Types.ONLY_TOR && mbwManager.torManager != null) {
            binding?.tvTorState?.visibility = View.VISIBLE
            showTorState(mbwManager.torManager.initState)
        } else {
            binding?.tvTorState?.visibility = View.GONE
        }
        updateAddressView()
        MbwManager.getEventBus().register(this)
        super.onStart()
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    @Subscribe
    fun onTorState(torState: TorStateChanged) {
        showTorState(torState.percentage)
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        updateAddressView()
        requireActivity().invalidateOptionsMenu()
    }

    private fun updateAddressView() {
        val account = MbwManager.getInstance(this.activity).selectedAccount
        val fragmentTransaction = childFragmentManager.beginTransaction()
        binding?.defineAddressAccountView(fragmentTransaction, account)
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun showTorState(percentage: Int) {
        binding?.tvTorState?.text =
            if (percentage == 0 || percentage == 100) "" else getString(R.string.tor_state_init)
    }

    internal inner class MenuImpl : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.record_options_menu_outputs, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            val mbwManager = MbwManager.getInstance(requireActivity())
            val account = mbwManager.selectedAccount
            menu.findItem(R.id.miShowOutputs)?.isVisible =
                account.isActive && account !is AbstractEthERC20Account
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId == R.id.miShowOutputs) {
                showOutputs()
                return true
            } else {
                return false
            }
        }
    }
}
