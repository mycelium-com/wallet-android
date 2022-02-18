package com.mycelium.wallet.activity.main

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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
import com.mycelium.wallet.event.*
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.fio.FioGroup
import com.squareup.otto.Subscribe
import kotlinx.android.synthetic.main.fio_request_history_view.*
import java.util.concurrent.atomic.AtomicBoolean

class FioRequestsHistoryFragment : Fragment(R.layout.fio_request_history_view) {
    private lateinit var _mbwManager: MbwManager
    private var _storage: MetadataStorage? = null
    private val currentActionMode: ActionMode? = null
    private val isLoadingPossible = AtomicBoolean(true)
    private val adapter = FioRequestAdapter()
    private lateinit var viewModel: FioRequestsHistoryViewModel
    private val preference by lazy { requireContext().getSharedPreferences("fio_request_fragment", Context.MODE_PRIVATE) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btAboutFioProtocol.setOnClickListener {
            AboutFIOProtocolDialog().show(parentFragmentManager, "modal")
        }
        lvTransactionHistory.itemAnimator = null
        lvTransactionHistory.adapter = adapter
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
        viewModel = ViewModelProviders.of(this).get(FioRequestsHistoryViewModel::class.java)
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
        llNoRecords.visibility = if (hasHistory) View.GONE else View.VISIBLE
        lvTransactionHistory.visibility = if (hasHistory) View.VISIBLE else View.GONE
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