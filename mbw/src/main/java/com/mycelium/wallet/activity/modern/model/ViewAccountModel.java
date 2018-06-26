package com.mycelium.wallet.activity.modern.model;

import android.graphics.drawable.Drawable;

import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.UUID;

public class ViewAccountModel {
    public UUID accountId;
    public WalletAccount.Type accountType;
    public String displayAddress;
    public CurrencyBasedBalance balance;
    public boolean isActive;
    public String label;
    public Drawable drawableForAccount;
    public Drawable drawableForAccountSelected;
    public boolean isRMCLinkedAccount;
    public boolean showBackupMissingWarning;
    public int syncTotalRetrievedTransactions;
}
