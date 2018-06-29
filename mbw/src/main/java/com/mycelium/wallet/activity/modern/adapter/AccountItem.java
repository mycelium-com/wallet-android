package com.mycelium.wallet.activity.modern.adapter;

import com.mycelium.wallet.activity.modern.model.ViewAccountModel;

import java.util.List;

public class AccountItem {
    int type;
    ViewAccountModel walletAccount;

    String title;
    List<ViewAccountModel> walletAccountList;

    AccountItem(int type, ViewAccountModel walletAccount) {
        this.type = type;
        this.walletAccount = walletAccount;
    }

    AccountItem(int type, String title, List<ViewAccountModel> walletAccountList) {
        this.type = type;
        this.title = title;
        this.walletAccountList = walletAccountList;
    }
}
