package com.mycelium.wallet.activity.main.model.transactionhistory

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.*
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIOOBTransaction
import com.mycelium.wapi.wallet.fio.FioModule
import com.squareup.otto.Subscribe
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * This class is intended to manage transaction history for current selected account.
 */
class TransactionHistoryLiveData(val mbwManager: MbwManager,
                                 private var account: WalletAccount<*>,
                                 private val fioMetadataMap : MutableMap<String, FIOOBTransaction>)
    : LiveData<Set<TransactionSummary>>() {
    private var historyList = mutableSetOf<TransactionSummary>()

    // Used to store reference for task from syncProgressUpdated().
    // Using weak reference as as soon as task completed it's irrelevant.
    private var syncProgressTaskWR: WeakReference<AsyncTask<Void, List<TransactionSummary>, List<TransactionSummary>>>? = null
    @Volatile
    private var executorService: ExecutorService
    val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule

    init {
        value = historyList
        executorService = Executors.newCachedThreadPool()
        startHistoryUpdate()
    }

    fun appendList(list: List<TransactionSummary>) {
        historyList.addAll(list)
        value = historyList
    }

    override fun onActive() {
        super.onActive()
        MbwManager.getEventBus().register(this)
        startHistoryUpdate()
    }

    override fun onInactive() {
        MbwManager.getEventBus().unregister(this)
    }

    private fun startHistoryUpdate(): AsyncTask<Void, List<TransactionSummary>, List<TransactionSummary>> =
            UpdateTxHistoryTask(account).executeOnExecutor(executorService)


    /**
     * Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
     */
    @SuppressLint("StaticFieldLeak")
    private inner class UpdateTxHistoryTask(val account: WalletAccount<*>) : AsyncTask<Void, List<TransactionSummary>, List<TransactionSummary>>() {

        override fun onPreExecute() {
            if (account.isArchived) {
                cancel(true)
            }
        }

        override fun doInBackground(vararg voids: Void): List<TransactionSummary> =
            (account.getTransactionSummaries(0, max(20, value!!.size)) as List<TransactionSummary>)
                .onEach { txSummary ->
                    fioModule.getFioTxMetadata(txSummary.idHex)?.let {
                        fioMetadataMap[txSummary.idHex] = it
                    }
                }

        override fun onPostExecute(transactions: List<TransactionSummary>) {
            updateValue(transactions)
        }
    }

    private fun updateValue(newValue: List<TransactionSummary>) {
        historyList = newValue.toMutableSet()
        value = historyList
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