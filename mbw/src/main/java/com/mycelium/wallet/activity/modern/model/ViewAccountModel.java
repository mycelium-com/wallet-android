package com.mycelium.wallet.activity.modern.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.AssetInfo;

import java.util.Objects;
import java.util.UUID;

public class ViewAccountModel{
    public UUID accountId;
    public AssetInfo coinType;
    public Class<?> accountType;
    public String displayAddress;
    public Balance balance;
    public boolean isActive;
    public String label;
    public Drawable drawableForAccount;
    public Drawable drawableForAccountSelected;
    public boolean isRMCLinkedAccount;
    public boolean showBackupMissingWarning;
    public int syncTotalRetrievedTransactions;
    public boolean isSyncing;
    public boolean isSyncError;

    public ViewAccountModel() {}

    public ViewAccountModel(AccountViewModel viewModel, Context context) {
        accountId = viewModel.getAccountId();
        coinType = viewModel.getCoinType();
        accountType = viewModel.getAccountType();
        if (HDPubOnlyAccount.class.isAssignableFrom(accountType) && viewModel.isActive()) {
            int numKeys = viewModel.getPrivateKeyCount();
            displayAddress = context.getResources().getQuantityString(R.plurals.contains_addresses, numKeys, numKeys);
        } else if (HDAccount.class.isAssignableFrom(accountType) && viewModel.isActive()) {
            int numKeys = viewModel.getPrivateKeyCount();
            displayAddress = context.getResources().getQuantityString(R.plurals.contains_keys, numKeys, numKeys);
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
        drawableForAccount = Utils.getDrawableForAccount(viewModel, false, resources);
        drawableForAccountSelected = Utils.getDrawableForAccount(viewModel, true, resources);
        isSyncing = viewModel.isSyncing();
        isSyncError = viewModel.isSyncError();
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
                Objects.equals(label, that.label) &&
                isSyncing == that.isSyncing &&
                isSyncError == that.isSyncError;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, accountType, displayAddress, balance, isActive, label,
                isRMCLinkedAccount, showBackupMissingWarning, syncTotalRetrievedTransactions, isSyncing,
                isSyncError);
    }
}
