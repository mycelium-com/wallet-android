package com.mycelium.bequant.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.ModalDialog
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.getInvestmentAccounts
import com.mycelium.bequant.kyc.BequantKycActivity
import com.mycelium.bequant.market.adapter.MarketFragmentAdapter
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.bequant.sign.SignActivity
import com.mycelium.bequant.signup.TwoFactorActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantMainBinding
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wapi.wallet.SyncMode
import com.squareup.otto.Subscribe
import kotlinx.coroutines.GlobalScope


class MarketFragment : Fragment() {
    var mediator: TabLayoutMediator? = null
    var binding: FragmentBequantMainBinding? = null
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            binding?.pager?.setCurrentItem(1, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        if (!BequantPreference.isDemo() && !BequantPreference.hasKeys()) {
            loader(true)
            Api.signRepository.getApiKeys(lifecycleScope, {
                broadcastManager.sendBroadcast(Intent(BequantConstants.ACTION_BEQUANT_KEYS))
                Handler().postDelayed({ requestBalances() }, 5000) // server has lag(3-5 seconds) in move keys from reg.bequant.io to api.bequant.io, е
            }, error = { code, message ->
                if (code != 420) {
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
        broadcastManager.registerReceiver(receiver, IntentFilter(BequantConstants.ACTION_EXCHANGE))
        Handler().postDelayed({
            when (arguments?.getString("from")) {
                "registration" -> {
                    ModalDialog(getString(R.string.bequant_turn_2fa),
                            getString(R.string.bequant_recommend_enable_2fa),
                            getString(R.string.secure_your_account)) {
                        startActivity(Intent(requireActivity(), TwoFactorActivity::class.java))
                    }.show(childFragmentManager, "modal_dialog")
                }
                "totp_registration" -> {
                    ModalDialog(getString(R.string.bequant_kyc_verify_title),
                            getString(R.string.bequant_kyc_verify_message),
                            getString(R.string.bequant_kyc_verify_button)) {
                        startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
                    }.show(childFragmentManager, "modal_dialog")
                }
            }
        }, 1000)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantMainBinding.inflate(inflater, container, false)
        .apply {

        }
        .root

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
        if (BequantPreference.getKYCToken().isEmpty()) {
            Api.kycRepository.kycToken(GlobalScope, {
                checkStatus()
            })
        } else {
            checkStatus()
        }
    }

    private fun checkStatus() {
        Api.kycRepository.status(GlobalScope, {
            activity?.run {
                invalidateOptionsMenu()
                val preference = getSharedPreferences("bequant_market_fragment", Context.MODE_PRIVATE)
                val prevKYCStatus = KYCStatus.valueOf(preference.getString("prev_kyc_status", "NONE")
                        ?: "NONE")
                if (prevKYCStatus != it.global && it.global == KYCStatus.VERIFIED) {
                    startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
                }
                preference.edit().putString("bequant_market_fragment", it.global.toString()).apply()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.pager?.adapter = MarketFragmentAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding?.pager?.offscreenPageLimit = 2
        mediator = TabLayoutMediator(binding!!.tabs, binding!!.pager) { tab, position ->
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
        val isDemo = activity?.intent?.getBooleanExtra(BequantMarketActivity.IS_DEMO_KEY, false)!!
        menu.findItem(R.id.kyc).isVisible = BequantPreference.getKYCStatus() != KYCStatus.VERIFIED && !isDemo
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.supportCenter -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BequantConstants.LINK_SUPPORT_CENTER)))
                    true
                }
                R.id.kyc -> {
                    startActivity(Intent(requireActivity(), BequantKycActivity::class.java))
                    true
                }
                R.id.logOut -> {
                    Api.signRepository.logout()
                    MbwManager.getEventBus().post(AccountListChanged())
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
        binding?.pager?.adapter = null
        binding = null
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