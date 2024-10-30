package com.mycelium.wallet.activity.fio.mapaccount

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
import com.mycelium.wallet.activity.fio.ExpirationDetailsDialog
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountNamesAdapter
import com.mycelium.wallet.activity.fio.mapaccount.adapter.FIONameItem
import com.mycelium.wallet.activity.fio.mapaccount.viewmodel.FIODomainViewModel
import com.mycelium.wallet.activity.fio.registerdomain.RegisterFIODomainActivity
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.databinding.FragmentFioDomainDetailsBinding
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.fio.coins.FIOToken
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FIODomainDetailsFragment : Fragment() {
    private val viewModel: FIODomainViewModel by viewModels()
    val adapter = AccountNamesAdapter()
    private lateinit var walletManager: WalletManager
    private lateinit var fioModule: FioModule

    val args: FIODomainDetailsFragmentArgs by navArgs()
    var binding: FragmentFioDomainDetailsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        fioModule.getFioAccountByFioDomain(args.domain.domain)?.run {
            viewModel.fioAccount.value = walletManager.getAccount(this) as FioAccount
        }
    }

    override fun onResume() {
        setFioDomain()
        updateList()
        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentFioDomainDetailsBinding.inflate(inflater, container, false)
                .apply {
                    binding = this
                    viewModel = this@FIODomainDetailsFragment.viewModel
                    lifecycleOwner = this@FIODomainDetailsFragment
                }.root

    private fun setFioDomain() {
        viewModel.fioDomain.value = fioModule.getFIODomainInfo(args.domain.domain)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = getString(R.string.domain_details)
        }
        setFioDomain()
        binding?.list?.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        binding?.list?.adapter = adapter
        binding?.list?.itemAnimator = null
        adapter.fioNameClickListener = {
            findNavController().navigate(FIODomainDetailsFragmentDirections.actionName(it))
        }
        updateList()
        binding?.createFIOName?.setOnClickListener {
            RegisterFioNameActivity.start(requireContext(),
                    viewModel.fioAccount.value!!.id, viewModel.fioDomain.value!!)
        }
        binding?.renewFIODomain?.setOnClickListener {
            val fioDomain = viewModel.fioDomain.value!!.domain
            RegisterFIODomainActivity.startRenew(requireContext(), viewModel.fioAccount.value!!.id, fioDomain)
        }
    }

    private fun updateList() {
        CoroutineScope(Dispatchers.IO).launch {
            adapter.submitList((walletManager.getModuleById(FioModule.ID) as FioModule)
                    .getFIONames(args.domain.domain).map { FIONameItem(it) })
        }
        binding?.tvFioNamesDesc?.text = if (adapter.itemCount > 0) {
            if (viewModel.fioAccount.value!!.canSpend()) {
                getString(R.string.fio_names_registered)
            } else {
                getString(R.string.fio_names_registered_read_only_account)
            }
        } else {
            getString(R.string.no_fio_names_registered)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (viewModel.fioAccount.value!!.canSpend()) {
            inflater.inflate(R.menu.domain_details_context_menu, menu)
        }

        inflater.inflate(R.menu.expiration_details_option, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.miAddFIOName -> {
                RegisterFioNameActivity.start(requireContext(), viewModel.fioAccount.value!!.id, viewModel.fioDomain.value!!)
                return true
            }
            R.id.miMakeDomainPublic -> {
                if (args.domain.isPublic) {
                    Toaster(this).toast("Domain is already public", false)
                } else {
                    MakeMyceliumDomainPublicTask(viewModel.fioAccount.value!!, args.domain.domain) {
                        Toaster(this).toast(it, false)
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
                return true
            }
            R.id.miExpirationDetails -> {
                ExpirationDetailsDialog().show(parentFragmentManager, "modal")
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                e.localizedMessage
            }
        }

        override fun onPostExecute(result: String) {
            listener(result)
        }
    }
}