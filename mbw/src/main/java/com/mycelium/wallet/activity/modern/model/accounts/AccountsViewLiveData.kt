package com.mycelium.wallet.activity.modern.model.accounts

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_TITLE_TYPE
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.colu.ColuAccount
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
        mbwManager.eventBus.register(this)
    }

    override fun onInactive() {
        mbwManager.eventBus.unregister(this)
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
                    bipAccountsToViewModel(sortAccounts(walletManager.getBTCBip44Accounts()))))
            val singleAddressList =  accountsToViewModel(sortAccounts(walletManager.getBTCSingleAddressAccounts()))
            if (singleAddressList.isNotEmpty()) {
                accountsList.add(AccountsGroupModel((R.string.active_bitcoin_sa_group_name), GROUP_TITLE_TYPE,
                        singleAddressList))
            }
            if (value!!.isEmpty()) {
                publishProgress(accountsList)
            }

            val bchBipList = bipAccountsToViewModel(sortAccounts(walletManager.getBCHBip44Accounts()))
            if (bchBipList.isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.bitcoin_cash_hd, GROUP_TITLE_TYPE,
                        bchBipList))
            }
            val bchSAList = accountsToViewModel(sortAccounts(walletManager.getBCHSingleAddressAccounts()))
            if (bchSAList.isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.bitcoin_cash_sa, GROUP_TITLE_TYPE,
                        bchSAList))
            }
            val ethList = accountsToViewModel(sortAccounts(walletManager.getEthAccounts()))
            if (ethList.isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.eth_accounts_name, GROUP_TITLE_TYPE,
                        ethList))
            }

            val coluAccounts = ArrayList<WalletAccount<out GenericTransaction, out GenericAddress>>()
            coluAccounts.addAll(walletManager.getColuAccounts())
            for (walletAccount in walletManager.getColuAccounts()) {
                val linkedAccount = Utils.getLinkedAccount(walletAccount, walletManager.getAccounts())
                if(linkedAccount != null) {
                    coluAccounts.add(linkedAccount)
                }
            }
            if (coluAccounts.isNotEmpty()) {
                accountsList.add(AccountsGroupModel(R.string.digital_assets, GROUP_TITLE_TYPE,
                        accountsToViewModel(sortAccounts(coluAccounts))))
            }

            val other = ArrayList<WalletAccount<out GenericTransaction, out GenericAddress>>()
            walletManager.getCoinapultAccounts().forEach {
                other.add(it)
            }

            if (other.isNotEmpty()) {
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