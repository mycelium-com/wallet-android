package com.mycelium.wallet.activity.modern.model.accounts

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.LiveData
import android.content.SharedPreferences
import android.os.AsyncTask
import com.mycelium.wallet.AccountManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.RecordRowBuilder
import com.mycelium.wallet.activity.modern.adapter.AccountListAdapter
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wallet.event.AccountGroupCollapsed
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.WalletAccount
import com.squareup.otto.Subscribe
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class is intended to monitor current accounts and must post changes as soon as accounts list was updated.
 * @param pagePrefs is used to determine which groups are collapsed
 * @see AccountsLiveData.getValue retuns list of current accounts, with filtered hidden ones.
 */
class AccountsLiveData(private val context: Application, private val mbwManager: MbwManager,
                       private val pagePrefs: SharedPreferences) : LiveData<List<AccountItem>>() {
    private val builder = RecordRowBuilder(mbwManager, context.resources)
    // List of all currently available accounts
    private var accountsList = Collections.synchronizedList(ArrayList<AccountItem>())

    private val executionService: ExecutorService

    init {
        value = Collections.emptyList()
        executionService = Executors.newCachedThreadPool()
        updateData()
    }

    override fun onActive() {
        mbwManager.eventBus.register(this)
    }

    override fun onInactive() {
        mbwManager.eventBus.unregister(this)
    }

    @Subscribe
    fun onAccountsListChanged(event: AccountListChanged) {
        updateData()
    }

    @Subscribe
    fun onGroupCollapsed(event: AccountGroupCollapsed) {
        updateList()
    }

    /**
     * Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
     */
    @SuppressLint("StaticFieldLeak")
    private inner class DataUpdateAsyncTask internal constructor(val context: Application) : AsyncTask<Void, List<AccountItem>, List<AccountItem>>() {
        override fun doInBackground(vararg voids: Void): List<AccountItem> {
            val am = AccountManager
            val accountsList = addGroup(R.string.active_hd_accounts_name, AccountListAdapter.GROUP_TITLE_TYPE, am.getBTCBip44Accounts().values)
            accountsList.addAll(addGroup(context.getString(R.string.active_bitcoin_sa_group_name),
                    AccountListAdapter.GROUP_TITLE_TYPE, am.getBTCSingleAddressAccounts().values))
            if (value!!.isEmpty()) {
                publishProgress(accountsList)
            }

            accountsList.addAll(addGroup(R.string.bitcoin_cash_hd, AccountListAdapter.GROUP_TITLE_TYPE, am.getBCHBip44Accounts().values))
            accountsList.addAll(addGroup(R.string.bitcoin_cash_sa, AccountListAdapter.GROUP_TITLE_TYPE, am.getBCHSingleAddressAccounts().values))

            val coluAccounts = ArrayList<WalletAccount>()
            for (walletAccount in am.getColuAccounts().values) {
                coluAccounts.add(walletAccount)
                coluAccounts.add((walletAccount as ColuAccount).linkedAccount)
            }
            accountsList.addAll(addGroup(R.string.digital_assets, AccountListAdapter.GROUP_TITLE_TYPE, coluAccounts))
            val accounts = am.getActiveAccounts().values.asList()
            val other = ArrayList<WalletAccount>()
            for (account in accounts) {
                when (account.type) {
                    WalletAccount.Type.BTCSINGLEADDRESS, WalletAccount.Type.BTCBIP44,
                    WalletAccount.Type.BCHSINGLEADDRESS, WalletAccount.Type.BCHBIP44, WalletAccount.Type.COLU -> {
                    }
                    else -> other.add(account)
                }
            }
            accountsList.addAll(addGroup(R.string.active_other_accounts_name, AccountListAdapter.GROUP_TITLE_TYPE, other))

            accountsList.add(AccountItem(AccountListAdapter.TOTAL_BALANCE_TYPE, "", builder.convertList(am.getActiveAccounts().values.asList())))
            accountsList.addAll(addGroup(R.string.archive_name, AccountListAdapter.GROUP_ARCHIVED_TITLE_TYPE, am.getArchivedAccounts().values))

            var isCollapsed = false
            val tempValue = accountsList.filter {
                if (it.title == null) {
                    isCollapsed
                } else {
                    isCollapsed = pagePrefs.getBoolean(it.title, true)
                    true
                }
            }
            if (tempValue == value) {
                cancel(true)
            }
            return accountsList
        }

        @SafeVarargs
        override fun onProgressUpdate(vararg values: List<AccountItem>) {
            super.onProgressUpdate(*values)
            accountsList = values[0]
            updateList()
        }

        override fun onPostExecute(result: List<AccountItem>) {
            accountsList = result
            updateList()
        }

        private fun addGroup(titleId: Int, titleType: Int, accounts: Collection<WalletAccount>): MutableList<AccountItem> {
            return addGroup(context.getString(titleId), titleType, accounts)
        }

        private fun addGroup(title: String, titleType: Int, accounts: Collection<WalletAccount>): MutableList<AccountItem> {
            val storage = mbwManager.metadataStorage
            return buildGroup(ArrayList(accounts), storage, title, titleType)
        }

        private fun buildGroup(accountList: List<WalletAccount>, storage: MetadataStorage, title: String, type: Int): MutableList<AccountItem> {
            val accounts = Utils.sortAccounts(accountList, storage)
            val viewAccountList = builder.convertList(accounts)

            val result = ArrayList<AccountItem>()
            if (!viewAccountList.isEmpty()) {
                result.add(AccountItem(type, title, viewAccountList))
                for (account in viewAccountList) {
                    result.add(AccountItem(AccountListAdapter.ACCOUNT_TYPE, account))
                }
            }
            return result
        }
    }

    private fun updateData() {
        DataUpdateAsyncTask(context).executeOnExecutor(executionService)
    }

    /**
     * This method is mainly intended to be able to quickly collapse groups.
     */
    private fun updateList() {
        synchronized(accountsList)
        {
            var isCollapsed = false
            value = accountsList!!.filter {
                if (it.title == null) {
                    isCollapsed
                } else {
                    isCollapsed = pagePrefs.getBoolean(it.title, true)
                    true
                }
            }
        }
    }
}