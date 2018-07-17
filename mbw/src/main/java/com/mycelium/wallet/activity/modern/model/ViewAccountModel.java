package com.mycelium.wallet.activity.modern.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44PubOnlyAccount;
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

    public ViewAccountModel() {

    }

    public ViewAccountModel(AccountViewModel viewModel, Context context) {
        accountId = viewModel.getAccountId();
        accountType = viewModel.getAccountType();
        final WalletAccount account = MbwManager.getInstance(context).getWalletManager(false).getAccount(accountId);
        if (account instanceof Bip44PubOnlyAccount && account.isActive()) {
            if (Integer.valueOf(viewModel.getDisplayAddress()) > 1) {
                displayAddress = context.getString(R.string.contains_addresses, viewModel.getDisplayAddress());
            } else {
                displayAddress = context.getString(R.string.account_contains_one_address_info);
            }
        } else if (account instanceof Bip44Account && account.isActive()) {
            if (Integer.valueOf(viewModel.getDisplayAddress()) > 1) {
                displayAddress = context.getString(R.string.contains_keys, viewModel.getDisplayAddress());
            } else {
                displayAddress = context.getString(R.string.account_contains_one_key_info);
            }
        } else {
            displayAddress = viewModel.getDisplayAddress();
        }
        balance = viewModel.getBalance();
        isActive = viewModel.isActive();
        label = viewModel.getLabel();
        isRMCLinkedAccount = viewModel.isRMCLinkedAccount();
        showBackupMissingWarning = viewModel.getShowBackupMissingWarning();
        syncTotalRetrievedTransactions = viewModel.getSyncTotalRetrievedTransactions();
        final Resources resources = context.getResources();
        drawableForAccount = Utils.getDrawableForAccount(account, false, resources);
        drawableForAccountSelected = Utils.getDrawableForAccount(account, true, resources);
    }


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
