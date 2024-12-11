package com.mycelium.wallet.activity.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.mycelium.net.ServerEndpointType
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.main.FioProtocolBannerFragment.Companion.newInstance
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
import com.mycelium.wapi.wallet.fio.FioAccount
import com.squareup.otto.Subscribe

class BalanceMasterFragment : Fragment() {

    private var binding: BalanceMasterFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BalanceMasterFragmentBinding.inflate(inflater).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragmentTransaction = fragmentManager!!.beginTransaction()
        val account = MbwManager.getInstance(this.activity).selectedAccount
        defineAddressAccountView(fragmentTransaction, account)
        fragmentTransaction.replace(R.id.phFragmentBalance, BalanceFragment())
        fragmentTransaction.replace(R.id.phFragmentNotice, NoticeFragment())
        fragmentTransaction.replace(R.id.phFragmentFioBanner, newInstance(false))
        fragmentTransaction.replace(R.id.phFragmentBuySell, BuySellFragment())
        fragmentTransaction.commitAllowingStateLoss()
        requireActivity().addMenuProvider(MenuImpl(), viewLifecycleOwner)
    }

    private fun defineAddressAccountView(
        fragmentTransaction: FragmentTransaction,
        account: WalletAccount<*>
    ) {
        fragmentTransaction.replace(
            R.id.phFragmentAddress,
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
        val activity: Activity? = activity
        // Set build version
        (activity!!.findViewById<View>(R.id.tvBuildText) as TextView).text =
            resources.getString(
                R.string.build_text,
                BuildConfig.VERSION_NAME
            )

        val mbwManager = MbwManager.getInstance(activity)
        if (mbwManager.torMode == ServerEndpointType.Types.ONLY_TOR && mbwManager.torManager != null) {
            binding?.tvTorState?.setVisibility(View.VISIBLE)
            showTorState(mbwManager.torManager.initState)
        } else {
            binding?.tvTorState?.setVisibility(View.GONE)
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
        val fragmentTransaction = fragmentManager!!.beginTransaction()
        defineAddressAccountView(fragmentTransaction, account)
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
            menu.findItem(R.id.miShowOutputs)
                ?.setVisible(account.isActive && account !is AbstractEthERC20Account && account !is FioAccount)
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
