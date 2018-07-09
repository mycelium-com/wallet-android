package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wallet.activity.modern.model.ViewAccountModel

class AccountItem {
    val type: Int
    var walletAccount: ViewAccountModel? = null

    var title: String? = null
    var walletAccountList: List<ViewAccountModel>? = null

    internal constructor(type: Int, walletAccount: ViewAccountModel) {
        this.type = type
        this.walletAccount = walletAccount
    }

    internal constructor(type: Int, title: String, walletAccountList: List<ViewAccountModel>) {
        this.type = type
        this.title = title
        this.walletAccountList = walletAccountList
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountItem

        if (type != other.type) return false
        if (walletAccount != other.walletAccount) return false
        if (title != other.title) return false
        if (walletAccountList != other.walletAccountList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + (walletAccount?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (walletAccountList?.hashCode() ?: 0)
        return result
    }
}
