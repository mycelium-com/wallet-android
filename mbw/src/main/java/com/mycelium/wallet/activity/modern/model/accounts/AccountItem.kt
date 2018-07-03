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
}
