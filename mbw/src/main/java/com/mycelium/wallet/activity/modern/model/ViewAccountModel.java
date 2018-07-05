package com.mycelium.wallet.activity.modern.model;

import android.graphics.drawable.Drawable;

import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewAccountModel that = (ViewAccountModel) o;
        return isActive == that.isActive &&
                isRMCLinkedAccount == that.isRMCLinkedAccount &&
                showBackupMissingWarning == that.showBackupMissingWarning &&
                syncTotalRetrievedTransactions == that.syncTotalRetrievedTransactions &&
                Objects.equals(accountId, that.accountId) &&
                accountType == that.accountType &&
                Objects.equals(displayAddress, that.displayAddress) &&
                Objects.equals(balance, that.balance) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, accountType, displayAddress, balance, isActive, label,
                isRMCLinkedAccount, showBackupMissingWarning, syncTotalRetrievedTransactions);
    }
}
