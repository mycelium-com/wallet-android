/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.modern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder;
import com.mycelium.wallet.activity.modern.model.ViewAccountModel;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mycelium.wapi.wallet.colu.ColuModuleKt.getColuAccounts;


public class RecordRowBuilder {
    private final MbwManager mbwManager;
    private final Resources resources;

    public RecordRowBuilder(MbwManager mbwManager, Resources resources) {
        this.mbwManager = mbwManager;
        this.resources = resources;
    }

    public void buildRecordView(AccountViewHolder holder, ViewAccountModel model, boolean isSelected, boolean hasFocus) {
        // Make grey if not part of the balance
        Utils.setAlpha(holder.llAddress, !isSelected ? 0.5f : 1f);

        // Show focus if applicable
        holder.llAddress.setBackgroundColor(resources.getColor(hasFocus ? R.color.selectedrecord : R.color.transparent));

        // Show/hide key icon
        Drawable drawableForAccount = isSelected ? model.drawableForAccountSelected : model.drawableForAccount;
        if (drawableForAccount == null) {
            holder.icon.setVisibility(View.INVISIBLE);
        } else {
            holder.icon.setVisibility(VISIBLE);
            holder.icon.setImageDrawable(drawableForAccount);
        }

        updateRMCInfo(holder, model);
        int textColor = resources.getColor(R.color.white);
        if (model.label.length() == 0) {
            holder.tvLabel.setVisibility(GONE);
        } else {
            // Display name
            holder.tvLabel.setVisibility(VISIBLE);
            holder.tvLabel.setText(Html.fromHtml(model.label));
            holder.tvLabel.setTextColor(textColor);
        }

        holder.tvAddress.setText(model.displayAddress);
        holder.tvAddress.setTextColor(textColor);
        updateSyncing(holder, model);
        updateBalance(holder, model, textColor);
        // Show/hide trader account message
        holder.tvTraderKey.setVisibility(model.accountId.equals(mbwManager.getLocalTraderManager().getLocalTraderAccountId())
                ? VISIBLE : GONE);
    }

    private void updateRMCInfo(AccountViewHolder holder, ViewAccountModel model) {
        if (model.isRMCLinkedAccount) {
            holder.tvWhatIsIt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(view.getContext())
                            .setMessage(resources.getString(R.string.rmc_bitcoin_acc_what_is_it))
                            .setPositiveButton(R.string.button_ok, null)
                            .create()
                            .show();
                }
            });
            holder.tvWhatIsIt.setVisibility(VISIBLE);
        } else {
            holder.tvWhatIsIt.setVisibility(GONE);
        }
    }

    private void updateSyncing(AccountViewHolder holder, ViewAccountModel model) {
        if (model.isSyncing) {
            holder.tvProgressLayout.setVisibility(VISIBLE);
            if (model.syncTotalRetrievedTransactions == 0) {
                holder.layoutProgressTxRetreived.setVisibility(GONE);
            } else {
                holder.layoutProgressTxRetreived.setVisibility(VISIBLE);
                holder.tvProgress.setText(resources.getString(R.string.sync_total_retrieved_transactions,
                        model.syncTotalRetrievedTransactions));
                holder.ivWhatIsSync.setOnClickListener(whatIsSyncHandler);
            }
        } else {
            holder.tvProgressLayout.setVisibility(GONE);
        }
    }

    private void updateBalance(AccountViewHolder holder, ViewAccountModel model, int textColor) {
        if (model.isActive) {
            Balance balance = model.balance;
            holder.tvBalance.setVisibility(VISIBLE);
            String balanceString = ValueExtensionsKt.toStringWithUnit(balance.getSpendable(),
                    mbwManager.getDenomination(model.coinType));
            holder.tvBalance.setText(balanceString);
            holder.tvBalance.setTextColor(textColor);

            // Show legacy account with funds warning if necessary
            holder.backupMissing.setVisibility(model.showBackupMissingWarning ? VISIBLE : GONE);
            if (mbwManager.getMetadataStorage().getOtherAccountBackupState(model.accountId) == MetadataStorage.BackupState.NOT_VERIFIED) {
                holder.backupMissing.setText(R.string.backup_not_verified);
            } else {
                holder.backupMissing.setText(R.string.backup_missing);
            }
            holder.tvAccountType.setVisibility(GONE);
        } else {
            // We don't show anything if the account is archived
            holder.tvBalance.setVisibility(GONE);
            holder.backupMissing.setVisibility(GONE);
            if (model.accountType.isInstance(Bip44BCHAccount.class)
                    || model.accountType.isInstance(SingleAddressBCHAccount.class)) {
                holder.tvAccountType.setText(Html.fromHtml(holder.tvAccountType.getResources().getString(R.string.bitcoin_cash)));
                holder.tvAccountType.setVisibility(VISIBLE);
            } else {
                holder.tvAccountType.setVisibility(GONE);
            }
        }
    }

    private View.OnClickListener whatIsSyncHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new AlertDialog.Builder(view.getContext(), R.style.MyceliumModern_Dialog_BlueButtons)
                    .setTitle(resources.getString(R.string.what_is_sync))
                    .setMessage(resources.getString(R.string.what_is_sync_description))
                    .setPositiveButton(R.string.button_ok, null)
                    .create()
                    .show();
        }
    };

    @SuppressLint("StringFormatMatches")
    private ViewAccountModel convert(WalletAccount walletAccount) {
        ViewAccountModel result = new ViewAccountModel();
        result.accountId = walletAccount.getId();
        result.coinType = walletAccount.getCoinType();

        result.drawableForAccount = Utils.getDrawableForAccount(walletAccount, false, resources);
        result.drawableForAccountSelected = Utils.getDrawableForAccount(walletAccount, true, resources);
        result.accountType = walletAccount.getClass();
        result.syncTotalRetrievedTransactions = walletAccount.getSyncTotalRetrievedTransactions();

        WalletAccount linked = Utils.getLinkedAccount(walletAccount, getColuAccounts(mbwManager.getWalletManager(false)));
        if (linked != null && (linked.getCoinType().equals(RMCCoin.INSTANCE) || linked.getCoinType().equals(RMCCoinTest.INSTANCE))) {
            result.isRMCLinkedAccount = true;
        }
        result.label = mbwManager.getMetadataStorage().getLabelByAccount(walletAccount.getId());
        if (walletAccount.isActive()) {
            if (walletAccount instanceof HDPubOnlyAccount) {
                int numKeys = ((HDAccount) walletAccount).getPrivateKeyCount();
                result.displayAddress = resources.getQuantityString(R.plurals.contains_addresses, numKeys, numKeys);
            } else if (walletAccount instanceof HDAccount) {
                int numKeys = ((HDAccount) walletAccount).getPrivateKeyCount();
                result.displayAddress = resources.getQuantityString(R.plurals.contains_keys, numKeys, numKeys);
            } else {
                Optional<BitcoinAddress> receivingAddress = ((WalletBtcAccount)(walletAccount)).getReceivingAddress();
                if (receivingAddress.isPresent()) {
                    if (result.label.length() == 0) {
                        // Display address in it's full glory, chopping it into three
                        result.displayAddress = receivingAddress.get().toMultiLineString();
                    } else {
                        // Display address in short form
                        result.displayAddress = receivingAddress.get().getShortAddress();
                    }
                } else {
                    result.displayAddress = "";
                }
            }
        } else {
            result.displayAddress = ""; //dont show key count of archived accs
        }
        result.isActive = walletAccount.isActive();
        if (result.isActive) {
            result.balance = walletAccount.getAccountBalance();
            result.showBackupMissingWarning = showBackupMissingWarning(walletAccount, mbwManager);
        }
        return result;
    }

    @NonNull
    public List<ViewAccountModel> convertList(List<WalletAccount<?>> accounts) {
        List<ViewAccountModel> viewAccountList = new ArrayList<>();
        for (WalletAccount account : accounts) {
            viewAccountList.add(convert(account));
        }
        return viewAccountList;
    }

    private static boolean showBackupMissingWarning(WalletAccount account, MbwManager mbwManager) {
        if (account.isArchived()) {
            return false;
        }

        boolean showBackupMissingWarning = false;
        if (account.canSpend()) {
            if (account.isDerivedFromInternalMasterseed()) {
                showBackupMissingWarning = mbwManager.getMetadataStorage().getMasterSeedBackupState() != MetadataStorage.BackupState.VERIFIED;
            } else {
                MetadataStorage.BackupState backupState = mbwManager.getMetadataStorage().getOtherAccountBackupState(account.getId());
                showBackupMissingWarning = (backupState != MetadataStorage.BackupState.VERIFIED) && (backupState != MetadataStorage.BackupState.IGNORED);
            }
        }

        return showBackupMissingWarning;
    }
}
