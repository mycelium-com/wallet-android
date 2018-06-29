package com.mycelium.wallet.activity.modern.adapter

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
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wallet.event.*
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.WalletAccount
import com.squareup.otto.Subscribe
import java.util.*

class AccountsLiveData(val context: Application, val mbwManager: MbwManager, val pagePrefs: SharedPreferences) : LiveData<List<AccountItem>>() {
    private val builder = RecordRowBuilder(mbwManager, context.resources)
    private var accountsList = Collections.emptyList<AccountItem>()

    init {
        value = Collections.emptyList()
    }

    override fun onActive() {
        super.onActive()
        mbwManager.eventBus.register(this)
    }

    override fun onInactive() {
        super.onInactive()
        mbwManager.eventBus.unregister(this)
    }

    @SuppressLint("StaticFieldLeak")
    //Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
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
        DataUpdateAsyncTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @Subscribe
    fun onAccountsListChanged(event: AccountListChanged) {
        updateData()
    }

    fun updateList() {
        var isCollapsed = false
        value = accountsList!!.filter {
            if (it.title == null) {
                return@filter !isCollapsed
            } else {
                isCollapsed = pagePrefs.getBoolean(it.title, true)
                return@filter true
            }
        }
    }
}