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

import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.View;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder;
import com.mycelium.wallet.activity.modern.model.ViewAccountModel;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.mycelium.wapi.wallet.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.ArrayList;
import java.util.List;

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
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(drawableForAccount);
        }

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
            holder.tvWhatIsIt.setVisibility(View.VISIBLE);
        } else {
            holder.tvWhatIsIt.setVisibility(View.GONE);
        }
        int textColor = resources.getColor(R.color.white);
        if (model.label.length() == 0) {
            holder.tvLabel.setVisibility(View.GONE);
        } else {
            // Display name
            holder.tvLabel.setVisibility(View.VISIBLE);
            holder.tvLabel.setText(Html.fromHtml(model.label));
            holder.tvLabel.setTextColor(textColor);
        }

        holder.tvAddress.setText(model.displayAddress);
        holder.tvAddress.setTextColor(textColor);

        if (model.syncTotalRetrievedTransactions == 0 && !model.isSyncing) {
            holder.tvProgressLayout.setVisibility(View.GONE);
        } else {
            holder.tvProgressLayout.setVisibility(View.VISIBLE);
            holder.tvProgress.setText(resources.getString(R.string.sync_total_retrieved_transactions,
                    Integer.toString(model.syncTotalRetrievedTransactions)));
            holder.ivWhatIsSync.setOnClickListener(whatIsSyncHandler);
        }

        // Set balance
        if (model.isActive) {
            CurrencyBasedBalance balance = model.balance;
            holder.tvBalance.setVisibility(View.VISIBLE);
            String balanceString = Utils.getFormattedValueWithUnit(balance.confirmed, mbwManager.getBitcoinDenomination());
            if (model.accountType == WalletAccount.Type.COLU) {
                balanceString = Utils.getColuFormattedValueWithUnit(balance.confirmed);
            }
            holder.tvBalance.setText(balanceString);
            holder.tvBalance.setTextColor(textColor);

            // Show legacy account with funds warning if necessary
            holder.backupMissing.setVisibility(model.showBackupMissingWarning ? View.VISIBLE : View.GONE);
            if (mbwManager.getMetadataStorage().getOtherAccountBackupState(model.accountId) == MetadataStorage.BackupState.NOT_VERIFIED) {
                holder.backupMissing.setText(R.string.backup_not_verified);
            } else {
                holder.backupMissing.setText(R.string.backup_missing);
            }
            holder.tvAccountType.setVisibility(View.GONE);

        } else {
            // We don't show anything if the account is archived
            holder.tvBalance.setVisibility(View.GONE);
            holder.backupMissing.setVisibility(View.GONE);
            if (model.accountType == WalletAccount.Type.BCHBIP44
                    || model.accountType == WalletAccount.Type.BCHSINGLEADDRESS) {
                holder.tvAccountType.setText(Html.fromHtml(holder.tvAccountType.getResources().getString(R.string.bitcoin_cash)));
                holder.tvAccountType.setVisibility(View.VISIBLE);
            } else {
                holder.tvAccountType.setVisibility(View.GONE);
            }
        }

        // Show/hide trader account message
        holder.tvTraderKey.setVisibility(model.accountId.equals(mbwManager.getLocalTraderManager().getLocalTraderAccountId())
                ? View.VISIBLE : View.GONE);
    }

    private View.OnClickListener whatIsSyncHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog dialog = new AlertDialog.Builder(view.getContext(), R.style.MyceliumModern_Dialog)
                    .setTitle(resources.getString(R.string.what_is_sync))
                    .setMessage(resources.getString(R.string.what_is_sync_description))
                    .setPositiveButton(R.string.button_ok, null)
                    .create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.mycelium_midblue));
        }
    };

    public ViewAccountModel convert(WalletAccount walletAccount) {
        ViewAccountModel result = new ViewAccountModel();
        result.accountId = walletAccount.getId();

        result.drawableForAccount = Utils.getDrawableForAccount(walletAccount, false, resources);
        result.drawableForAccountSelected = Utils.getDrawableForAccount(walletAccount, true, resources);
        result.accountType = walletAccount.getType();
        result.syncTotalRetrievedTransactions = walletAccount.getSyncTotalRetrievedTransactions();

        WalletAccount linked = Utils.getLinkedAccount(walletAccount, mbwManager.getColuManager().getAccounts().values());
        if (linked != null
                && linked.getType() == WalletAccount.Type.COLU
                && ((ColuAccount) linked).getColuAsset().assetType == ColuAccount.ColuAssetType.RMC) {
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
                Optional<Address> receivingAddress = walletAccount.getReceivingAddress();
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
            result.balance = walletAccount.getCurrencyBasedBalance();
            result.showBackupMissingWarning = showBackupMissingWarning(walletAccount, mbwManager);
        }
        return result;
    }

    @NonNull
    public List<ViewAccountModel> convertList(List<WalletAccount> accounts) {
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
