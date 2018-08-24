package com.mycelium.wallet.activity.main.model.transactionhistory

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.*
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.GenericTransaction
import com.squareup.otto.Subscribe
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

/**
 * This class is intended to manage transaction history for current selected account.
 */
class TransactionHistoryLiveData(val mbwManager: MbwManager) : LiveData<List<GenericTransaction>>() {
    private var account = mbwManager.selectedAccount!!
    private var historyList: MutableList<GenericTransaction> = ArrayList()
    // Used to store reference for task from syncProgressUpdated().
    // Using weak reference as as soon as task completed it's irrelevant.
    private var syncProgressTaskWR: WeakReference<AsyncTask<Void, MutableList<GenericTransaction>, MutableList<GenericTransaction>>>? = null
    @Volatile
    private var executorService: ExecutorService

    init {
        value = historyList
        executorService = Executors.newCachedThreadPool()
        startHistoryUpdate()
    }

    fun appendList(list: List<GenericTransaction>) {
        historyList.addAll(list)
        value = historyList
    }

    override fun onActive() {
        super.onActive()
        mbwManager.eventBus.register(this)
        if (account !== mbwManager.selectedAccount) {
            account = mbwManager.selectedAccount
            updateValue(ArrayList())
        }
        startHistoryUpdate()
    }

    override fun onInactive() {
        mbwManager.eventBus.unregister(this)
    }

    private fun startHistoryUpdate(): AsyncTask<Void, MutableList<GenericTransaction>, MutableList<GenericTransaction>> =
            UpdateTxHistoryTask().executeOnExecutor(executorService)


    /**
     * Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
     */
    @SuppressLint("StaticFieldLeak")
    private inner class UpdateTxHistoryTask : AsyncTask<Void, MutableList<GenericTransaction>, MutableList<GenericTransaction>>() {
        var account = mbwManager.selectedAccount!!
        override fun onPreExecute() {
            if (account.isArchived) {
                cancel(true)
            }
        }

        override fun doInBackground(vararg voids: Void): MutableList<GenericTransaction>  =
                account.getTransactions(0, Math.max(20, value!!.size)) as MutableList<GenericTransaction>

        override fun onPostExecute(transactions: MutableList<GenericTransaction>) {
            if (account === mbwManager.selectedAccount) {
                updateValue(transactions)
            }
        }
    }

    private fun updateValue(newValue: MutableList<GenericTransaction>) {
        historyList = newValue
        value = historyList
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        val oldExecutor = executorService
        executorService = Executors.newCachedThreadPool()
        oldExecutor.shutdownNow()
        if (event.account != account.id) {
            account = mbwManager.selectedAccount
            updateValue(ArrayList())
            startHistoryUpdate()
        }
    }

    @Subscribe
    fun syncStopped(event: SyncStopped) {
        startHistoryUpdate()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged) {
        if (event.account == account.id) {
            startHistoryUpdate()
        }
    }

    @Subscribe
    fun balanceChanged(event: BalanceChanged) {
        if (event.account == account.id) {
            startHistoryUpdate()
        }
    }

    @Subscribe
    fun addressBookChanged(event: AddressBookChanged) {
        startHistoryUpdate()
    }

    @Subscribe
    fun transactionLabelChanged(event: TransactionLabelChanged) {
        startHistoryUpdate()
    }

    /**
     * This method might be called too frequently in process of sync, so it would not start
     * next task until previous finished.
     */
    @Subscribe
    fun syncProgressUpdated(event: SyncProgressUpdated) {
        val syncProgressTask = syncProgressTaskWR?.get()
        if (event.account == account.id && syncProgressTask?.status != AsyncTask.Status.RUNNING
                && syncProgressTask?.status != AsyncTask.Status.PENDING) {
            syncProgressTaskWR = WeakReference(startHistoryUpdate())
        }
    }
}