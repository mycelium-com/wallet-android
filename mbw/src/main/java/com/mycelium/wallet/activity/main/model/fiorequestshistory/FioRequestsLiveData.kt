package com.mycelium.wallet.activity.main.model.fiorequestshistory

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.*
import com.mycelium.wallet.fio.event.FioRequestStatusChanged
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioGroup
import com.mycelium.wapi.wallet.fio.getActiveFioAccounts
import com.squareup.otto.Subscribe
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class is intended to manage requests history for all accounts.
 */
class FioRequestsLiveData(val mbwManager: MbwManager) : LiveData<MutableList<FioGroup>>() {
    private var historyList = mutableListOf<FioGroup>()
    // Used to store reference for task from syncProgressUpdated().
    // Using weak reference as as soon as task completed it's irrelevant.
    private var syncProgressTaskWR: WeakReference<AsyncTask<Void, List<FioGroup>, List<FioGroup>>>? = null
    @Volatile
    private var executorService: ExecutorService

    init {
        value = historyList
        executorService = Executors.newCachedThreadPool()
        startHistoryUpdate()
    }

    override fun onActive() {
        super.onActive()
        MbwManager.getEventBus().register(this)
        startHistoryUpdate()
    }

    override fun onInactive() {
        MbwManager.getEventBus().unregister(this)
    }

    private fun startHistoryUpdate(): AsyncTask<Void, List<FioGroup>, List<FioGroup>> =
            UpdateTxHistoryTask().executeOnExecutor(executorService)


    /**
     * Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
     * // TODO replace with something like flowable, which would throw out
     */
    @SuppressLint("StaticFieldLeak")
    private inner class UpdateTxHistoryTask : AsyncTask<Void, List<FioGroup>, List<FioGroup>>() {
        var accountsList = mbwManager.getWalletManager(false)
                .getActiveFioAccounts()
        override fun onPreExecute() {
            if (accountsList.isEmpty()) {
                cancel(true)
            }
        }

        override fun doInBackground(vararg voids: Void): List<FioGroup> {
            return accountsList.map(FioAccount::getRequestsGroups)
                    .flatten()
                    .groupBy(FioGroup::status, FioGroup::children)
                    .map { (status, groupList) ->
                        FioGroup(status, groupList.flatten()
                            .distinctBy(FIORequestContent::fioRequestId)
                            .sortedByDescending{ SimpleDateFormat("yyy-MM-dd'T'kk:mm:ss").parse(it.timeStamp)}
                            .toMutableList()) }

        }

        override fun onPostExecute(transactions: List<FioGroup>) {
            updateValue(transactions)
        }
    }

    private fun updateValue(newValue: List<FioGroup>) {
        historyList.clear()
        historyList.addAll(newValue)
        postValue(historyList)
    }

    @Subscribe
    fun syncStopped(event: SyncStopped) {
        startHistoryUpdate()
    }

    @Subscribe
    fun balanceChanged(event: BalanceChanged) {
        startHistoryUpdate()
    }

    @Subscribe
    fun addressBookChanged(event: AddressBookChanged) {
        startHistoryUpdate()
    }

    @Subscribe
    fun transactionLabelChanged(event: TransactionLabelChanged) {
        startHistoryUpdate()
    }

    @Subscribe
    fun requestChanged(event: FioRequestStatusChanged) {
        startHistoryUpdate()
    }

    /**
     * This method might be called too frequently in process of sync, so it would not start
     * next task until previous finished.
     */
    @Subscribe
    fun syncProgressUpdated(event: SyncProgressUpdated) {
        val syncProgressTask = syncProgressTaskWR?.get()
        if (syncProgressTask?.status != AsyncTask.Status.RUNNING
                && syncProgressTask?.status != AsyncTask.Status.PENDING) {
            syncProgressTaskWR = WeakReference(startHistoryUpdate())
        }
    }
}