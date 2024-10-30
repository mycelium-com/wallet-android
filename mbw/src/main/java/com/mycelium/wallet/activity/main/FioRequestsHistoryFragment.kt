package com.mycelium.wallet.activity.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.AboutFIOProtocolDialog
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity
import com.mycelium.wallet.activity.fio.requests.SentFioRequestStatusActivity
import com.mycelium.wallet.activity.main.adapter.FioRequest
import com.mycelium.wallet.activity.main.adapter.FioRequestAdapter
import com.mycelium.wallet.activity.main.adapter.FioRequestAdapterItem
import com.mycelium.wallet.activity.main.adapter.Group
import com.mycelium.wallet.activity.main.model.fiorequestshistory.FioRequestsHistoryViewModel
import com.mycelium.wallet.databinding.FioRequestHistoryViewBinding
import com.mycelium.wallet.event.*
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.fio.FioGroup
import com.squareup.otto.Subscribe
import java.util.concurrent.atomic.AtomicBoolean

class FioRequestsHistoryFragment : Fragment(R.layout.fio_request_history_view) {
    private lateinit var _mbwManager: MbwManager
    private var _storage: MetadataStorage? = null
    private val currentActionMode: ActionMode? = null
    private val isLoadingPossible = AtomicBoolean(true)
    private val adapter = FioRequestAdapter()
    val viewModel: FioRequestsHistoryViewModel by viewModels()
    private val preference by lazy { requireContext().getSharedPreferences("fio_request_fragment", Context.MODE_PRIVATE) }
    private var binding: FioRequestHistoryViewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FioRequestHistoryViewBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.btAboutFioProtocol?.setOnClickListener {
            AboutFIOProtocolDialog().show(parentFragmentManager, "modal")
        }
        binding?.lvTransactionHistory?.itemAnimator = null
        binding?.lvTransactionHistory?.adapter = adapter
        adapter.itemClickListener = { item, group ->
            if (group.status == FioGroup.Type.SENT) {
                SentFioRequestStatusActivity.start(requireActivity(), item)
            } else {
                ApproveFioRequestActivity.start(requireActivity(), item)
            }
        }
        adapter.groupClickListener = {
            val expanded = preference.getBoolean("group_${it.status.name}", true)
            preference.edit().putBoolean("group_${it.status.name}", !expanded).apply()
            adapter.submitList(generateList(viewModel.fioRequestHistory.value!!))
        }
        viewModel.fioRequestHistory.observe(viewLifecycleOwner, Observer {
            showHistory(!it.isNullOrEmpty())
            adapter.submitList(generateList(it))
        })
    }

    private fun generateList(groupList: List<FioGroup>): List<FioRequestAdapterItem> =
            mutableListOf<FioRequestAdapterItem>().apply {
                groupList.forEach { group ->
                    val expanded = preference.getBoolean("group_${group.status.name}", true)
                    add(Group(group, expanded))
                    if(expanded) {
                        addAll(group.children.map { FioRequest(group, it) })
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
        // cache the addressbook for faster lookup
        viewModel.cacheAddressBook()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _mbwManager = MbwManager.getInstance(context)
        _storage = _mbwManager.metadataStorage
    }

    override fun onResume() {
        MbwManager.getEventBus().register(this)
        super.onResume()
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    @Subscribe
    fun exchangeRateChanged(event: ExchangeRatesRefreshed?) {
        adapter.submitList(generateList(viewModel.fioRequestHistory.value!!))
    }


    @Subscribe
    fun fiatCurrencyChanged(event: SelectedCurrencyChanged?) {
        adapter.submitList(generateList(viewModel.fioRequestHistory.value!!))
    }

    @Subscribe
    fun addressBookEntryChanged(event: AddressBookChanged?) {
        viewModel.cacheAddressBook()
        adapter.submitList(generateList(viewModel.fioRequestHistory.value!!))
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        isLoadingPossible.set(true)
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        // It's possible that new transactions came. Adapter should allow to try to scroll
        isLoadingPossible.set(true)
    }

    private fun showHistory(hasHistory: Boolean) {
        binding?.llNoRecords?.visibility = if (hasHistory) View.GONE else View.VISIBLE
        binding?.lvTransactionHistory?.visibility = if (hasHistory) View.VISIBLE else View.GONE
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser) {
            finishActionMode()
        }
    }

    private fun finishActionMode() {
        currentActionMode?.finish()
    }
}