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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44PubOnlyAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencySum;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

public class RecordRowBuilder {

   private final MbwManager mbwManager;
   private final Resources resources;
   private final LayoutInflater inflater;

   public RecordRowBuilder(MbwManager mbwManager, Resources resources, LayoutInflater inflater) {
      this.mbwManager = mbwManager;
      this.resources = resources;
      this.inflater = inflater;
   }

   public View buildRecordView(ViewGroup parent, WalletAccount walletAccount, boolean isSelected, boolean hasFocus) {
      return buildRecordView(parent, walletAccount, isSelected, hasFocus, null);
   }

   public View buildRecordView(ViewGroup parent, WalletAccount walletAccount, boolean isSelected, boolean hasFocus, View convertView) {
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
      ImageView icon = (ImageView) rowView.findViewById(R.id.ivIcon);

      Drawable drawableForAccount = Utils.getDrawableForAccount(walletAccount, isSelected, resources);
      if (drawableForAccount == null) {
         icon.setVisibility(View.INVISIBLE);
      } else {
         icon.setVisibility(View.VISIBLE);
         icon.setImageDrawable(drawableForAccount);
      }

//      ImageView iconPrivKey = (ImageView) rowView.findViewById(R.id.ivIconPrivKey);
//      if (walletAccount instanceof ColuAccount) {
//         iconPrivKey.setVisibility(walletAccount.canSpend() ? View.VISIBLE : View.GONE);
//      } else {
//         iconPrivKey.setVisibility(View.GONE);
//      }

      TextView tvLabel = ((TextView) rowView.findViewById(R.id.tvLabel));
      TextView tvWhatIsIt = ((TextView) rowView.findViewById(R.id.tvWhatIsIt));
      String name = mbwManager.getMetadataStorage().getLabelByAccount(walletAccount.getId());
      WalletAccount linked = Utils.getLinkedAccount(walletAccount, mbwManager.getColuManager().getAccounts().values());
      if (linked != null && linked instanceof ColuAccount) {
          if (((ColuAccount) linked).getColuAsset().assetType == ColuAccount.ColuAssetType.RMC) {
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
      } else {
          tvWhatIsIt.setVisibility(View.GONE);
      }
      if (name.length() == 0) {
         rowView.findViewById(R.id.tvLabel).setVisibility(View.GONE);
      } else {
         // Display name

         tvLabel.setVisibility(View.VISIBLE);
         tvLabel.setText(Html.fromHtml(name));
         tvLabel.setTextColor(textColor);
      }

      String displayAddress;
      if (walletAccount.isActive()) {
         if (walletAccount instanceof Bip44PubOnlyAccount) {
            int numKeys = ((Bip44Account) walletAccount).getPrivateKeyCount();
            if (numKeys > 1) {
               displayAddress = resources.getString(R.string.contains_addresses, Integer.toString(numKeys));
            } else {
               displayAddress = resources.getString(R.string.account_contains_one_address_info);
            }
         } else if (walletAccount instanceof Bip44Account) {
            int numKeys = ((Bip44Account) walletAccount).getPrivateKeyCount();
            if (numKeys > 1) {
               displayAddress = resources.getString(R.string.contains_keys, Integer.toString(numKeys));
            } else {
               displayAddress = resources.getString(R.string.account_contains_one_key_info);
            }
         } else {

            Optional<Address> receivingAddress = walletAccount.getReceivingAddress();
            if (receivingAddress.isPresent()) {
               if (name.length() == 0) {
                  // Display address in it's full glory, chopping it into three
                  displayAddress = receivingAddress.get().toMultiLineString();
               } else {
                  // Display address in short form
                  displayAddress = receivingAddress.get().getShortAddress();
               }
            } else {
               displayAddress = "";
            }
         }
      } else {
         displayAddress = ""; //dont show key count of archived accs
      }


      TextView tvAddress = ((TextView) rowView.findViewById(R.id.tvAddress));
      tvAddress.setText(displayAddress);
      tvAddress.setTextColor(textColor);

      // Set tag
      rowView.setTag(walletAccount);

      // Set balance
      if (walletAccount.isActive()) {
         CurrencyBasedBalance balance = walletAccount.getCurrencyBasedBalance();
         rowView.findViewById(R.id.tvBalance).setVisibility(View.VISIBLE);
         String balanceString = Utils.getFormattedValueWithUnit(balance.confirmed, mbwManager.getBitcoinDenomination());
         if(walletAccount instanceof ColuAccount) {
            balanceString = Utils.getColuFormattedValueWithUnit(walletAccount.getCurrencyBasedBalance().confirmed);
         }
         TextView tvBalance = ((TextView) rowView.findViewById(R.id.tvBalance));
         tvBalance.setText(balanceString);
         tvBalance.setTextColor(textColor);

         // Show legacy account with funds warning if necessary
//         boolean showLegacyAccountWarning = showLegacyAccountWarning(walletAccount, mbwManager);
//         rowView.findViewById(R.id.tvLegacyAccountWarning).setVisibility(showLegacyAccountWarning ? View.VISIBLE : View.GONE);

         // Show legacy account with funds warning if necessary
         boolean showBackupMissingWarning = showBackupMissingWarning(walletAccount, mbwManager);
         rowView.findViewById(R.id.tvBackupMissingWarning).setVisibility(showBackupMissingWarning ? View.VISIBLE : View.GONE);

      } else {
         // We don't show anything if the account is archived
         rowView.findViewById(R.id.tvBalance).setVisibility(View.GONE);
//         rowView.findViewById(R.id.tvLegacyAccountWarning).setVisibility(View.GONE);
         rowView.findViewById(R.id.tvBackupMissingWarning).setVisibility(View.GONE);
      }


      // Show/hide trader account message
      if (walletAccount.getId().equals(mbwManager.getLocalTraderManager().getLocalTraderAccountId())) {
         rowView.findViewById(R.id.tvTraderKey).setVisibility(View.VISIBLE);
      } else {
         rowView.findViewById(R.id.tvTraderKey).setVisibility(View.GONE);
      }

      return rowView;
   }

   public View buildTotalView(LinearLayout parent, CurrencySum balanceSum) {
      View rowView = inflater.inflate(R.layout.record_row_total, parent, false);
      ToggleableCurrencyButton tcdBalance = ((ToggleableCurrencyButton) rowView.findViewById(R.id.tcdBalance));
      tcdBalance.setEventBus(mbwManager.getEventBus());
      tcdBalance.setCurrencySwitcher(mbwManager.getCurrencySwitcher());
      tcdBalance.setValue(balanceSum);
      return rowView;
   }

   public static boolean showLegacyAccountWarning(WalletAccount account, MbwManager mbwManager) {
      if (account.isArchived()) {
         return false;
      }
      Balance balance = account.getBalance();
      boolean showLegacyAccountWarning = (account instanceof SingleAddressAccount) &&
            balance.getReceivingBalance() + balance.getSpendableBalance() > 0 &&
            account.canSpend() &&
            !mbwManager.getMetadataStorage().getIgnoreLegacyWarning(account.getId());
      return showLegacyAccountWarning;
   }

   public static boolean showBackupMissingWarning(WalletAccount account, MbwManager mbwManager) {
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
