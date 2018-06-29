package com.mycelium.wallet.activity.modern.model.accounts;

import com.mycelium.wallet.activity.modern.model.ViewAccountModel;

import java.util.List;

public class AccountItem {
    public final int type;
    public ViewAccountModel walletAccount;

    public String title;
    public List<ViewAccountModel> walletAccountList;

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
