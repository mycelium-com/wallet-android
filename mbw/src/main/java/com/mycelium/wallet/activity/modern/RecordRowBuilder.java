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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.model.ViewAccountModel;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44PubOnlyAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;

import java.util.ArrayList;
import java.util.List;

public class RecordRowBuilder {
   private final MbwManager mbwManager;
   private final Resources resources;
   private final LayoutInflater inflater;

   public RecordRowBuilder(MbwManager mbwManager, Resources resources, LayoutInflater inflater) {
      this.mbwManager = mbwManager;
      this.resources = resources;
      this.inflater = inflater;
   }

   public View buildRecordView(ViewGroup parent, ViewAccountModel model, boolean isSelected, boolean hasFocus, View convertView) {
      View rowView = convertView;
      if(rowView == null) {
         rowView = inflater.inflate(R.layout.record_row, parent, false);
      }

      // Make grey if not part of the balance
      if (!isSelected) {
         Utils.setAlpha(rowView, 0.5f);
      } else {
         Utils.setAlpha(rowView, 1f);
      }

      int textColor = resources.getColor(R.color.white);

      // Show focus if applicable
      if (hasFocus) {
         rowView.setBackgroundColor(resources.getColor(R.color.selectedrecord));
      } else {
         rowView.setBackgroundColor(resources.getColor(R.color.transparent));
      }

      // Show/hide key icon
      ImageView icon = rowView.findViewById(R.id.ivIcon);

      Drawable drawableForAccount = isSelected ? model.drawableForAccountSelected : model.drawableForAccount;
      if (drawableForAccount == null) {
         icon.setVisibility(View.INVISIBLE);
      } else {
         icon.setVisibility(View.VISIBLE);
         icon.setImageDrawable(drawableForAccount);
      }

      TextView tvLabel = rowView.findViewById(R.id.tvLabel);
      TextView tvWhatIsIt = rowView.findViewById(R.id.tvWhatIsIt);


      if (model.isRMCLinkedAccount) {
         tvWhatIsIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               new AlertDialog.Builder(view.getContext())
                       .setMessage(resources.getString(R.string.rmc_bitcoin_acc_what_is_it))
                       .setPositiveButton(R.string.button_ok, null)
                       .create()
                       .show();
            }
         });
         tvWhatIsIt.setVisibility(View.VISIBLE);
      } else {
         tvWhatIsIt.setVisibility(View.GONE);
      }
      if (model.label.length() == 0) {
         tvLabel.setVisibility(View.GONE);
      } else {
         // Display name
         tvLabel.setVisibility(View.VISIBLE);
         tvLabel.setText(Html.fromHtml(model.label));
         tvLabel.setTextColor(textColor);
      }

      TextView tvAddress = rowView.findViewById(R.id.tvAddress);
      tvAddress.setText(model.displayAddress);
      tvAddress.setTextColor(textColor);

      TextView tvAccountType = rowView.findViewById(R.id.tvAccountType);
      // Set balance
      if (model.isActive) {
         CurrencyBasedBalance balance = model.balance;
         rowView.findViewById(R.id.tvBalance).setVisibility(View.VISIBLE);
         String balanceString = Utils.getFormattedValueWithUnit(balance.confirmed, mbwManager.getBitcoinDenomination());
         if(model.accountType == WalletAccount.Type.COLU) {
            balanceString = Utils.getColuFormattedValueWithUnit(balance.confirmed);
         }
         TextView tvBalance = rowView.findViewById(R.id.tvBalance);
         tvBalance.setText(balanceString);
         tvBalance.setTextColor(textColor);

         // Show legacy account with funds warning if necessary

         TextView backupMissing = rowView.findViewById(R.id.tvBackupMissingWarning);
         backupMissing.setVisibility(model.showBackupMissingWarning ? View.VISIBLE : View.GONE);
         if(mbwManager.getMetadataStorage().getOtherAccountBackupState(model.accountId) == MetadataStorage.BackupState.NOT_VERIFIED) {
            backupMissing.setText(R.string.backup_not_verified);
         } else {
            backupMissing.setText(R.string.backup_missing);
         }
         tvAccountType.setVisibility(View.GONE);

      } else {
         // We don't show anything if the account is archived
         rowView.findViewById(R.id.tvBalance).setVisibility(View.GONE);
         rowView.findViewById(R.id.tvBackupMissingWarning).setVisibility(View.GONE);
         if (model.accountType == WalletAccount.Type.BCHBIP44
                 || model.accountType == WalletAccount.Type.BCHSINGLEADDRESS) {
            tvAccountType.setText(Html.fromHtml(tvAccountType.getResources().getString(R.string.bitcoin_cash)));
            tvAccountType.setVisibility(View.VISIBLE);
         } else {
            tvAccountType.setVisibility(View.GONE);
         }
      }

      // Show/hide trader account message
      if (model.accountId.equals(mbwManager.getLocalTraderManager().getLocalTraderAccountId())) {
         rowView.findViewById(R.id.tvTraderKey).setVisibility(View.VISIBLE);
      } else {
         rowView.findViewById(R.id.tvTraderKey).setVisibility(View.GONE);
      }

      return rowView;
   }

   public ViewAccountModel convert(WalletAccount walletAccount) {
      ViewAccountModel result = new ViewAccountModel();
      result.accountId = walletAccount.getId();

      result.drawableForAccount = Utils.getDrawableForAccount(walletAccount, false, resources);
      result.drawableForAccountSelected = Utils.getDrawableForAccount(walletAccount, true, resources);



      WalletAccount linked = Utils.getLinkedAccount(walletAccount, mbwManager.getColuManager().getAccounts().values());
      if (linked != null
              && linked.getType() == WalletAccount.Type.COLU
              && ((ColuAccount) linked).getColuAsset().assetType == ColuAccount.ColuAssetType.RMC) {
         result.isRMCLinkedAccount = true;
      }
      result.label = mbwManager.getMetadataStorage().getLabelByAccount(walletAccount.getId());
      if (walletAccount.isActive()) {
         if (walletAccount instanceof Bip44PubOnlyAccount) {
            int numKeys = ((Bip44Account) walletAccount).getPrivateKeyCount();
            if (numKeys > 1) {
               result.displayAddress = resources.getString(R.string.contains_addresses, Integer.toString(numKeys));
            } else {
               result.displayAddress = resources.getString(R.string.account_contains_one_address_info);
            }
         } else if (walletAccount instanceof Bip44Account) {
            int numKeys = ((Bip44Account) walletAccount).getPrivateKeyCount();
            if (numKeys > 1) {
               result.displayAddress = resources.getString(R.string.contains_keys, Integer.toString(numKeys));
            } else {
               result.displayAddress = resources.getString(R.string.account_contains_one_key_info);
            }
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
      if(result.isActive) {
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
