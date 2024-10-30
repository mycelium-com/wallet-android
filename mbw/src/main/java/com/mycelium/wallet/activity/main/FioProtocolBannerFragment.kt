package com.mycelium.wallet.activity.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.AddAccountActivity
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity.Companion.start
import com.mycelium.wallet.activity.modern.AccountsFragment
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.event.SelectTab
import com.mycelium.wallet.databinding.FioProtocolBannerFragmentBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.getFioAccounts
import com.squareup.otto.Subscribe

class FioProtocolBannerFragment : Fragment() {
    enum class Banner {
        ACCOUNTS, BALANCE_NO_NAMES, BALANCE_NAMES, NONE
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mbwManager: MbwManager
    private lateinit var banner: Banner
    private val isAccountsListBanner: Boolean by lazy {
        requireArguments().getSerializable(IS_ACCOUNTS_LIST) as Boolean
    }
    var binding: FioProtocolBannerFragmentBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(false)
        super.onCreate(savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences(FIO_BANNER_PREF, Context.MODE_PRIVATE)
        mbwManager = MbwManager.getInstance(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FioProtocolBannerFragmentBinding.inflate(inflater, container, false)
            .apply {
                binding = this
            }
            .root

    override fun onStart() {
        MbwManager.getEventBus().register(this)
        super.onStart()
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.layoutCreate?.icClose?.setOnClickListener {
            sharedPreferences.edit()
                    .putBoolean(if (isAccountsListBanner) SHOW_FIO_CREATE_ACCOUNT_BANNER else
                        "$SHOW_FIO_CREATE_NAME_BANNER${mbwManager.selectedAccount.label}", false)
                    .apply()
            recheckBanner()
        }
        binding?.layoutCreate?.btCreateFioName?.setOnClickListener {
            start(requireContext(), mbwManager.selectedAccount.id)
        }
        binding?.layoutCreate?.btCreateFioAccount?.setOnClickListener {
            AddAccountActivity.callMe(this, AccountsFragment.ADD_RECORD_RESULT_CODE)
        }
        binding?.layoutCreated?.btFioRequests?.setOnClickListener {
            MbwManager.getEventBus().post(SelectTab(ModernMain.TAB_FIO_REQUESTS))
        }
        binding?.layoutCreated?.btFioNames?.setOnClickListener {
            startActivity(Intent(context, AccountMappingActivity::class.java)
                    .putExtra("accountId", mbwManager.selectedAccount.id))
        }
        recheckBanner()
    }

    private fun determineBanner(): Banner {
        if (isAccountsListBanner) {
            val showBanner = sharedPreferences.getBoolean(SHOW_FIO_CREATE_ACCOUNT_BANNER, true)
            return if (showBanner) {
                if (mbwManager.getWalletManager(false).getFioAccounts().isEmpty()) {
                    Banner.ACCOUNTS
                } else {
                    Banner.NONE
                }
            } else {
                Banner.NONE
            }
        }

        val account = mbwManager.selectedAccount
        return if (account is FioAccount) {
            if (!account.canSpend()) return Banner.NONE

            val names = account.registeredFIONames
            if (names.isNotEmpty()) {
                Banner.BALANCE_NAMES
            } else {
                val showBanner = sharedPreferences.getBoolean("$SHOW_FIO_CREATE_NAME_BANNER${mbwManager.selectedAccount.label}", true)
                if (showBanner) {
                    Banner.BALANCE_NO_NAMES
                } else {
                    Banner.NONE
                }
            }
        } else {
            Banner.NONE
        }
    }

    private fun updateUi() {
        when (banner) {
            Banner.ACCOUNTS -> {
                binding?.fioProtocolBannerMainLayout?.visibility = View.VISIBLE
                binding?.layoutCreated?.root?.visibility = View.GONE
                binding?.layoutCreate?.root?.visibility = View.VISIBLE
                binding?.layoutCreate?.btCreateFioName?.visibility = View.GONE
                binding?.layoutCreate?.btCreateFioAccount?.visibility = View.VISIBLE
                binding?.layoutCreate?.tvFioProtocol?.text = "Service"
            }
            Banner.BALANCE_NO_NAMES -> {
                binding?.fioProtocolBannerMainLayout?.visibility = View.VISIBLE
                binding?.layoutCreated?.root?.visibility = View.GONE
                binding?.layoutCreate?.root?.visibility = View.VISIBLE
                binding?.layoutCreate?.btCreateFioName?.visibility = View.VISIBLE
                binding?.layoutCreate?.btCreateFioAccount?.visibility = View.GONE
                binding?.layoutCreate?.tvFioProtocol?.text = "Protocol"
            }
            Banner.BALANCE_NAMES -> {
                binding?.fioProtocolBannerMainLayout?.visibility = View.VISIBLE
                binding?.layoutCreated?.root?.visibility = View.VISIBLE
                binding?.layoutCreate?.root?.visibility = View.GONE
            }
            Banner.NONE -> {
                binding?.fioProtocolBannerMainLayout?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun recheckBanner() {
        banner = determineBanner()
        updateUi()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged?) {
        recheckBanner()
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        recheckBanner()
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        recheckBanner()
    }

    companion object {
        private const val SHOW_FIO_CREATE_NAME_BANNER = "showFioCreateNameBanner"
        private const val SHOW_FIO_CREATE_ACCOUNT_BANNER = "showFioCreateAccountBanner"
        private const val IS_ACCOUNTS_LIST = "isAccountsListBanner"
        private const val FIO_BANNER_PREF = "fio_banner"

        @JvmStatic
        fun newInstance(isAccountsListBanner: Boolean): FioProtocolBannerFragment {
            val f = FioProtocolBannerFragment()
            val args = Bundle()

            args.putBoolean(IS_ACCOUNTS_LIST, isAccountsListBanner)

            f.arguments = args
            return f
        }
    }
}