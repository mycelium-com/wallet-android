package com.mycelium.wallet.activity.modern.model.accounts

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_TITLE_TYPE
import com.mycelium.wallet.activity.util.getBTCSingleAddressAccounts
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.getBCHBip44Accounts
import com.mycelium.wapi.wallet.bch.single.getBCHSingleAddressAccounts
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.coinapult.getCoinapultAccounts
import com.mycelium.wapi.wallet.colu.getColuAccounts
import com.mycelium.wapi.wallet.eth.getEthAccounts
import com.squareup.otto.Subscribe
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This class is intended to monitor current accounts and must post changes as soon as accounts list was updated.
 * @see AccountsViewLiveData.getValue retuns list of current accounts, with filtered hidden ones.
 */
class AccountsViewLiveData(private val mbwManager: MbwManager) : LiveData<List<AccountsGroupModel>>() {
    // List of all currently available accounts
    private var accountsList = Collections.emptyList<AccountsGroupModel>()
    private val executionService: ExecutorService

    init {
        value = Collections.emptyList()
        executionService = Executors.newCachedThreadPool()
        updateData()
    }

    override fun onActive() {
        MbwManager.getEventBus().register(this)
    }

    override fun onInactive() {
        MbwManager.getEventBus().unregister(this)
    }

    @Subscribe
    fun onAccountsListChanged(event: AccountListChanged) {
        updateData()
    }

    /**
     * Leak might not occur, as only application context passed and whole class don't contains any Activity related contexts
     */
    @SuppressLint("StaticFieldLeak")
    private inner class DataUpdateAsyncTask : AsyncTask<Void, List<AccountsGroupModel>, List<AccountsGroupModel>>() {
        override fun doInBackground(vararg voids: Void): List<AccountsGroupModel> {
            val walletManager = mbwManager.getWalletManager(false)
            val accountsList = mutableListOf(AccountsGroupModel(R.string.active_hd_accounts_name, GROUP_TITLE_TYPE,
                    bipAccountsToViewModel(sortAccounts(walletManager.getActiveAccountsFrom(walletManager.getBTCBip44Accounts())))))

            val singleAddressList = sortAccounts(walletManager.getBTCSingleAddressAccounts())
            if (walletManager.getActiveAccountsFrom(singleAddressList).isNotEmpty()) {
                accountsList.add(AccountsGroupModel((R.string.active_bitcoin_sa_group_name), GROUP_TITLE_TYPE,
                        accountsToViewModel(singleAddressList)))
            }
            if (value!!.isEmpty()) {
                publishProgress(accountsList)
            }

            val bchBipList = sortAccounts(walletManager.getBCHBip44Accounts())
            if (walletManager.getActiveAccountsFrom(bchBipList).isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.bitcoin_cash_hd, GROUP_TITLE_TYPE,
                        bipAccountsToViewModel(bchBipList)))
            }
            val bchSAList = sortAccounts(walletManager.getBCHSingleAddressAccounts())
            if (walletManager.getActiveAccountsFrom(bchSAList).isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.bitcoin_cash_sa, GROUP_TITLE_TYPE,
                        accountsToViewModel(bchSAList)))
            }
            val ethList = sortAccounts(walletManager.getEthAccounts())
            if (walletManager.getActiveAccountsFrom(ethList).isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.eth_accounts_name, GROUP_TITLE_TYPE,
                        accountsToViewModel(ethList)))
            }

            val coluAccounts = ArrayList<WalletAccount<out GenericTransaction, out GenericAddress>>()
            coluAccounts.addAll(walletManager.getColuAccounts())
            for (walletAccount in walletManager.getColuAccounts()) {
                val linkedAccount = Utils.getLinkedAccount(walletAccount, walletManager.getAccounts())
                if(linkedAccount != null) {
                    coluAccounts.add(linkedAccount)
                }
            }
            if (walletManager.getActiveAccountsFrom(coluAccounts).isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.digital_assets, GROUP_TITLE_TYPE,
                        accountsToViewModel(sortAccounts(coluAccounts))))
            }

            val other = ArrayList<WalletAccount<out GenericTransaction, out GenericAddress>>()
            other.addAll(walletManager.getCoinapultAccounts())

            if (walletManager.getActiveAccountsFrom(other).isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.active_other_accounts_name, GROUP_TITLE_TYPE,
                        accountsToViewModel(sortAccounts(other))))
            }

            val archivedList = accountsToViewModel(walletManager.getArchivedAccounts())
            if (archivedList.isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.archive_name, GROUP_ARCHIVED_TITLE_TYPE,
                        archivedList))
            }
            if (accountsList == value) {
                cancel(true)
            }
            return accountsList
        }

        private fun accountsToViewModel(accounts: Collection<WalletAccount<out GenericTransaction, out GenericAddress>>) =
                accounts.map { AccountViewModel(it, mbwManager) }

        private fun bipAccountsToViewModel(accounts: Collection<WalletAccount<out GenericTransaction, out GenericAddress>>) =
                accounts.map { AccountViewModel(it, mbwManager) }

        private fun sortAccounts(accounts: Collection<WalletAccount<out GenericTransaction, out GenericAddress>>) =
                Utils.sortAccounts(accounts, mbwManager.metadataStorage)

        @SafeVarargs
        override fun onProgressUpdate(vararg values: List<AccountsGroupModel>) {
            super.onProgressUpdate(*values)
            accountsList = values[0]
            updateList()
        }

        override fun onPostExecute(result: List<AccountsGroupModel>) {
            accountsList = result
            updateList()
        }

        private fun addTitleGroup(titleId: Int, accountsGroupsList: MutableList<AccountsGroupModel>,
                                  accountsModelsList: List<AccountViewModel>) {
            addGroup(titleId, accountsGroupsList,GROUP_TITLE_TYPE, accountsModelsList)
        }

        private fun addGroup(titleId: Int, accountsGroupsList: MutableList<AccountsGroupModel>,
                             type: AccountListItem.Type, list: List<AccountViewModel>) {
            if (list.isNotEmpty()) {
                accountsGroupsList.add(AccountsGroupModel(titleId, type, list))
            }
        }
    }

    private fun updateData() {
        DataUpdateAsyncTask().executeOnExecutor(executionService)
    }

    /**
     * This method is mainly intended to be able to quickly collapse groups.
     */
    private fun updateList() {
        if (value != accountsList) {
            value = accountsList
        }
    }
}