package com.mycelium.wallet.activity.fio.mapaccount

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountNamesAdapter
import com.mycelium.wallet.activity.fio.mapaccount.adapter.FIONameItem
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.FIODomainViewModel
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.databinding.FragmentFioDomainDetailsBinding
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.coins.FIOToken
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import kotlinx.android.synthetic.main.fragment_fio_account_mapping.list
import kotlinx.android.synthetic.main.fragment_fio_domain_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FIODomainDetailsFragment : Fragment() {

    private val viewModel: FIODomainViewModel by viewModels()
    val adapter = AccountNamesAdapter()

    val args: FIODomainDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return DataBindingUtil.inflate<FragmentFioDomainDetailsBinding>(inflater, R.layout.fragment_fio_domain_details, container, false)
                .apply {
                    viewModel = this@FIODomainDetailsFragment.viewModel
                    lifecycleOwner = this@FIODomainDetailsFragment
                }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.domain_details)
        }
        val mbwManager = MbwManager.getInstance(requireContext())
        val walletManager = mbwManager.getWalletManager(false)
        viewModel.fioDomain.value = args.domain
        (walletManager.getModuleById(FioModule.ID) as FioModule).getFioAccountByFioDomain(args.domain.domain)?.run {
            viewModel.fioAccount.value = walletManager.getAccount(this) as FioAccount
        }
        list.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        list.adapter = adapter
        list.itemAnimator = null
        adapter.fioNameClickListener = {
            findNavController().navigate(FIODomainDetailsFragmentDirections.actionName(it))
        }
        updateList(walletManager)
        createFIOName.setOnClickListener {
            RegisterFioNameActivity.start(requireContext(),
                    MbwManager.getInstance(requireContext()).selectedAccount.id)
        }
    }

    fun updateList(walletManager: WalletManager) {
        CoroutineScope(Dispatchers.IO).launch {
            adapter.submitList((walletManager.getModuleById(FioModule.ID) as FioModule)
                    .getFIONames(args.domain.domain).map { FIONameItem(it) })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.domain_details_context_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.miAddFIOName) {
            RegisterFioNameActivity.start(requireContext())
            return true
        } else if (item.itemId == R.id.miMakeDomainPublic) {
            if (args.domain.isPublic) {
                Toaster(this).toast("Domain is already public", false)
            } else {
                val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
                val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
                val uuid = fioModule.getFioAccountByFioDomain(args.domain.domain)
                        ?: throw IllegalStateException("Illegal domain ${args.domain.domain} (Not owned by any of user's accounts)")
                val ownerFioAccount = walletManager.getAccount(uuid) as FioAccount
                MakeMyceliumDomainPublicTask(ownerFioAccount, args.domain.domain) {
                    Toaster(this).toast(it, false)
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class MakeMyceliumDomainPublicTask(
            private val ownerFioAccount: FioAccount,
            private val domain: String,
            val listener: ((String) -> Unit)) : AsyncTask<Void, String, String>() {
        private val fioToken = Utils.getFIOCoinType() as FIOToken

        override fun doInBackground(vararg args: Void): String {
            return try {
                // checking if enough balance to pay for fee
                val fee = ownerFioAccount.getFeeByEndpoint(FIOApiEndPoints.FeeEndPoint.SetDomainVisibility)
                val accountBalance = ownerFioAccount.accountBalance.spendable.value
                if (accountBalance < fee) {
                    return "Not enough funds to pay for the service. " +
                            "Account balance ${Value.valueOf(fioToken, accountBalance)}, fee ${Value.valueOf(fioToken, fee)}"
                }

                val actionTraceResponse = ownerFioAccount.setDomainVisibility(domain, isPublic = true)
                return if (actionTraceResponse != null && actionTraceResponse.status == "OK") {
                    "Domain successfully made public"
                } else {
                    "Something went wrong $status"
                }
            } catch (e: Exception) {
                e.localizedMessage
            }
        }

        override fun onPostExecute(result: String) {
            listener(result)
        }
    }
}