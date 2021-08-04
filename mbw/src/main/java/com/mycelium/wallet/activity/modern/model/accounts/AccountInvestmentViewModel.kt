package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount


class AccountInvestmentViewModel(val account: WalletAccount<out Address>, val balance: String) : AccountListItem, SyncStatusItem {
    val accountId = account.id!!
    var label = "Trading Account"
    override var isSyncError = account.lastSyncStatus()?.status in arrayOf(SyncStatus.INTERRUPT, SyncStatus.ERROR)

    override fun getType(): AccountListItem.Type = AccountListItem.Type.INVESTMENT_TYPE
}