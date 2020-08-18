package com.mycelium.bequant.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.getInvestmentAccounts
import com.mycelium.bequant.kyc.BequantKycActivity
import com.mycelium.bequant.market.adapter.MarketFragmentAdapter
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.bequant.sign.SignActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.SyncMode
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.fragment_bequant_main.*


class MarketFragment : Fragment(R.layout.fragment_bequant_main) {
    var mediator: TabLayoutMediator? = null
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            pager.setCurrentItem(1, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        if (!BequantPreference.isDemo() && !BequantPreference.hasKeys()) {
            loader(true)
            Api.signRepository.getApiKeys(lifecycleScope, {
                broadcastManager.sendBroadcast(Intent(Constants.ACTION_BEQUANT_KEYS))
                Handler().postDelayed({ requestBalances() }, 5000) // server has lag(3-5 seconds) in move keys from reg.bequant.io to api.bequant.io, е
            }, error = { code, message ->
                if(code != 420) {
                    ErrorHandler(requireContext()).handle(message)
                }
            }, finally = {
                loader(false)
            })
        } else {
            requestBalances()
            MbwManager.getInstance(requireContext()).getWalletManager(false).let {
                it.startSynchronization(SyncMode.NORMAL_FORCED, it.getInvestmentAccounts())
            }
        }
        MbwManager.getEventBus().register(this)
        broadcastManager.registerReceiver(receiver, IntentFilter(Constants.ACTION_EXCHANGE))
    }

    @Subscribe
    fun requestBalance(request: RequestBalance) {
        requestBalances()
    }

    private fun requestBalances() {
        if (BequantPreference.hasKeys()) {
            Api.tradingRepository.tradingBalanceGet(lifecycleScope, { arrayOfBalances ->
                MbwManager.getEventBus().post(TradingBalance(arrayOfBalances ?: arrayOf()))
            }, { _, error ->
                ErrorHandler(requireContext()).handle(error)
            }, {
            })
            Api.accountRepository.accountBalanceGet(lifecycleScope, { arrayOfBalances ->
                MbwManager.getEventBus().post(AccountBalance(arrayOfBalances ?: arrayOf()))
            }, { _, error ->
                ErrorHandler(requireContext()).handle(error)
            }, {
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = MarketFragmentAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        pager.offscreenPageLimit = 2
        mediator = TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "Markets"
                1 -> tab.text = "Exchange"
                2 -> tab.text = "Account"
            }
        }
        mediator?.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bequant_market, menu)
        menu.findItem(R.id.logIn).isVisible = !BequantPreference.isLogged()
        menu.findItem(R.id.logOut).isVisible = BequantPreference.isLogged()
        menu.findItem(R.id.kyc).isVisible = BequantPreference.getKYCStatus() != KYCStatus.APPROVED
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.supportCenter -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_SUPPORT_CENTER)))
                    true
                }
                R.id.kyc -> {
                    startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
                    true
                }
                R.id.logOut -> {
                    Api.signRepository.logout()
                    activity?.finish()
                    startActivity(Intent(requireContext(), SignActivity::class.java))
                    true
                }
                R.id.logIn -> {
                    activity?.finish()
                    startActivity(Intent(requireContext(), SignActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        pager.adapter = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MbwManager.getEventBus().unregister(this)
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        super.onDestroy()
    }
}

data class TradingBalance(val balances: Array<Balance>)
data class AccountBalance(val balances: Array<Balance>)
class RequestBalance