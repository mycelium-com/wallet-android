package com.mycelium.wallet.activity.modern.adapter

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.RecordRowBuilder
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.ArchivedGroupTitleViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.GroupTitleViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.TotalViewHolder
import com.mycelium.wallet.activity.modern.model.ViewAccountModel
import com.mycelium.wallet.activity.modern.model.accounts.*
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.*
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.currency.CurrencySum
import java.util.*
import kotlin.collections.ArrayList

class AccountListAdapter(fragment: Fragment, private val mbwManager: MbwManager)
    : ListAdapter<AccountListItem, RecyclerView.ViewHolder>(ItemListDiffCallback(fragment.context!!)) {
    private val context = fragment.context!!

    private var focusedAccountId: UUID? = null
    private var selectedAccountId: UUID? = mbwManager.selectedAccount.id

    private var itemClickListener: ItemClickListener? = null
    private val layoutInflater: LayoutInflater
    private val pagePrefs = context.getSharedPreferences("account_list", Context.MODE_PRIVATE)
    private val listModel: AccountsListModel = ViewModelProviders.of(fragment).get(AccountsListModel::class.java)

    val focusedAccount: WalletAccount?
        get() = mbwManager.getWalletManager(false).getAccount(focusedAccountId)

    init {
        layoutInflater = LayoutInflater.from(context)
        listModel.accountsData.observe(fragment, Observer { accountsGroupModels ->
            accountsGroupModels!!
            val selectedAccountExists = accountsGroupModels.any { it.accountsList.any { it.accountId == selectedAccountId } }
            if (!selectedAccountExists) {
                setFocusedAccountId(null)
            }
            refreshList(accountsGroupModels)
        })
        val accountsGroupsList = listModel.accountsData.value!!
        refreshList(accountsGroupsList)
    }

    private fun refreshList(accountsGroupModels: List<AccountsGroupModel>) {
        submitList(generateListView(accountsGroupModels))
    }

    private fun generateListView(accountsGroupsList: List<AccountsGroupModel>): List<AccountListItem> {
        val itemList = ArrayList<AccountListItem>()
        var totalAdded = false

        for (accountsGroup in accountsGroupsList) {
            if (accountsGroup.getType() == GROUP_ARCHIVED_TITLE_TYPE) {
                itemList.add(TotalViewModel(getSpendableBalance(itemList)))
                totalAdded = true
            }
            itemList.add(accountsGroup)
            val title = accountsGroup.getTitle(context)
            val isGroupVisible = pagePrefs.getBoolean(title, true)
            if (isGroupVisible) {
                itemList.addAll(accountsGroup.accountsList)
            }
        }
        if (!itemList.isEmpty() && !totalAdded) {
            itemList.add(TotalViewModel(getSpendableBalance(itemList)))
        }

        return itemList
    }

    fun setItemClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    fun setFocusedAccountId(focusedAccountId: UUID?) {
        if (this.focusedAccountId == null) {
            this.focusedAccountId = mbwManager.selectedAccount.id
        }
        val oldFocusedPosition = findPosition(this.focusedAccountId)
        // If old account was removed we don't want to notify removed element. It would be updated itself.
        val updateOld = mbwManager.getWalletManager(false).getAccount(this.focusedAccountId) != null
        val oldSelectedPosition = findPosition(this.selectedAccountId)
        this.focusedAccountId = focusedAccountId
        if (focusedAccountId != null && mbwManager.getWalletManager(false).getAccount(focusedAccountId).isActive) {
            // If archived account selected - selection stays on previous account, while focus moves to archived.
            this.selectedAccountId = focusedAccountId
            notifyItemChanged(oldSelectedPosition)
        }
        if (updateOld) {
            notifyItemChanged(oldFocusedPosition)
        }
        notifyItemChanged(findPosition(this.focusedAccountId))
    }

    private fun findPosition(account: UUID?): Int {
        var position = -1
        for (i in 0 until itemCount) {
            if (getItem(i).getType() == ACCOUNT_TYPE) {
                val item = getItem(i) as AccountViewModel
                if (item.accountId == account) {
                    position = i
                    break
                }
            }
        }
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (AccountListItem.Type.fromId(viewType)) {
            GROUP_TITLE_TYPE -> createGroupViewHolder(parent)
            GROUP_ARCHIVED_TITLE_TYPE -> createArchivedTitleViewHolder(parent)
            ACCOUNT_TYPE -> createAccountViewHolder(parent)
            TOTAL_BALANCE_TYPE -> createTotalBalanceViewHolder(parent)
            else -> throw IllegalArgumentException("Unknow account type")
        }
    }

    private fun createGroupViewHolder(parent: ViewGroup): GroupTitleViewHolder {
        val view = layoutInflater.inflate(R.layout.accounts_title_view, parent, false)
        return GroupTitleViewHolder(view)
    }

    private fun createArchivedTitleViewHolder(parent: ViewGroup): ArchivedGroupTitleViewHolder {
        val view = layoutInflater.inflate(R.layout.accounts_title_view, parent, false)
        return ArchivedGroupTitleViewHolder(view)
    }

    private fun createAccountViewHolder(parent: ViewGroup): AccountViewHolder {
        val view = layoutInflater.inflate(R.layout.record_row, parent, false)
        return AccountViewHolder(view)
    }

    private fun createTotalBalanceViewHolder(parent: ViewGroup): TotalViewHolder {
        val view = layoutInflater.inflate(R.layout.record_row_total, parent, false)
        return TotalViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        when (AccountListItem.Type.fromId(viewType)) {
            ACCOUNT_TYPE -> {
                val accountHolder = holder as AccountViewHolder
                val account = item as AccountViewModel
                val viewModel = ViewAccountModel(account, context)
                val builder = RecordRowBuilder(mbwManager, context.resources)
                builder.buildRecordView(accountHolder, viewModel, mbwManager.selectedAccount.id == account.accountId,
                        focusedAccountId == account.accountId)
                accountHolder.llAddress.setOnClickListener {
                    setFocusedAccountId(account.accountId)
                    if (itemClickListener != null) {
                        itemClickListener!!.onItemClick(mbwManager.getWalletManager(false)
                                .getAccount(account.accountId))
                    }
                }
            }
            GROUP_TITLE_TYPE -> {
                val groupHolder = holder as GroupTitleViewHolder
                val group = item as AccountsGroupModel
                buildGroupBase(group, groupHolder)
                val sum = getSpendableBalance(listOf(group))
                groupHolder.tvBalance.setValue(sum)
                groupHolder.tvBalance.visibility = View.VISIBLE
            }
            GROUP_ARCHIVED_TITLE_TYPE -> {
                val groupHolder = holder as ArchivedGroupTitleViewHolder
                val group = item as AccountsGroupModel
                buildGroupBase(group, groupHolder)
            }
            TOTAL_BALANCE_TYPE -> {
                val totalHolder = holder as TotalViewHolder
                val sum = (item as TotalViewModel).balance
                totalHolder.tcdBalance.setValue(sum)
            }
            UKNOWN -> throw IllegalArgumentException("Unknown view type")
        }
    }

    private fun buildGroupBase(group: AccountsGroupModel, groupHolder: GroupTitleViewHolder) {
        val title = group.getTitle(context)
        groupHolder.tvTitle.text = Html.fromHtml(title)
        val count = group.accountsList.size
        groupHolder.tvAccountsCount.visibility = if (count > 0) View.VISIBLE else View.GONE
        groupHolder.tvAccountsCount.text = "($count)"
        groupHolder.itemView.setOnClickListener {
            //Should be here as initial state in model is wrong
            val isGroupVisible = !pagePrefs.getBoolean(title, true)
            pagePrefs.edit().putBoolean(title, isGroupVisible).apply()
            refreshList(listModel.accountsData.value!!)
        }
        groupHolder.expandIcon.rotation = (if (pagePrefs.getBoolean(title, true)) 180 else 0).toFloat()
    }

    private fun getSpendableBalance(walletAccountList: List<AccountListItem>): CurrencySum {
        val currencySum = CurrencySum()
        for (item in walletAccountList) {
            if (item.getType() == GROUP_TITLE_TYPE) {
                for (account in (item as AccountsGroupModel).accountsList) {
                    if (account.isActive) {
                        currencySum.add(account.balance!!.confirmed)
                    }
                }
            }
        }
        return currencySum
    }

    override fun getItemViewType(position: Int) = getItem(position).getType().typeId

    interface ItemClickListener {
        fun onItemClick(account: WalletAccount)
    }

    class ItemListDiffCallback(val context: Context) : DiffUtil.ItemCallback<AccountListItem>() {
        override fun areItemsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean {
            return when {
                oldItem.getType() != newItem.getType() -> false
                listOf(GROUP_TITLE_TYPE, GROUP_ARCHIVED_TITLE_TYPE).any { it == oldItem.getType() } -> {
                    (oldItem as AccountsGroupModel).titleId == (newItem as AccountsGroupModel).titleId
                }
                oldItem.getType() == ACCOUNT_TYPE -> {
                    (oldItem as AccountViewModel).accountId == (newItem as AccountViewModel).accountId
                }
                else -> true
            }
        }

        private val pagePrefs = context.getSharedPreferences("account_list", Context.MODE_PRIVATE)

        override fun areContentsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean {
            return when {
                listOf(GROUP_TITLE_TYPE, GROUP_ARCHIVED_TITLE_TYPE).any { it == oldItem.getType() } -> {
                    val title = (newItem as AccountsGroupModel).getTitle(context)
                    val sameCollapseState = newItem.isCollapsed == pagePrefs.getBoolean(title, true)
                    newItem.isCollapsed = pagePrefs.getBoolean(title, true)
                    sameCollapseState && (oldItem == newItem)
                }
                else -> oldItem == newItem
            }
        }
    }
}
