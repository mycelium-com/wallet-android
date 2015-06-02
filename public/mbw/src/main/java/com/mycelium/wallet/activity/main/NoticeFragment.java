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

package com.mycelium.wallet.activity.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Subscribe;

import java.util.List;

public class NoticeFragment extends Fragment {

   private enum Notice {
      BACKUP_MISSING, SINGLEKEY_BACKUP_MISSING, MOVE_LEGACY_FUNDS, RESET_PIN_AVAILABLE, RESET_PIN_IN_PROGRESS, NONE
   }

   private MbwManager _mbwManager;
   private View _root;
   private Notice _notice;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_notice_fragment, container, false));
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(false);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(activity);
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      _notice = determineNotice();
      _root.findViewById(R.id.btWarning).setOnClickListener(warningClickListener);
      _root.findViewById(R.id.btBackupMissing).setOnClickListener(noticeClickListener);
      _root.findViewById(R.id.btPinResetNotice).setOnClickListener(noticeClickListener);
      updateUi();
      super.onResume();
   }

   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   private Notice determineNotice() {
      List<WalletAccount> accounts = _mbwManager.getWalletManager(false).getActiveAccounts();
      MetadataStorage meta = _mbwManager.getMetadataStorage();

      Optional<Integer> resetPinRemainingBlocksCount = _mbwManager.getResetPinRemainingBlocksCount();
      // Check first if a Pin-Reset is now possible
      if (resetPinRemainingBlocksCount.isPresent() && resetPinRemainingBlocksCount.get()==0){
         return Notice.RESET_PIN_AVAILABLE;
      }

      // Then check if a Pin-Reset is in process
      if (resetPinRemainingBlocksCount.isPresent()){
         return Notice.RESET_PIN_IN_PROGRESS;
      }

      // First check if we have HD accounts with funds, but have no master seed backup
      if (meta.getMasterSeedBackupState() != MetadataStorage.BackupState.VERIFIED) {
         for (WalletAccount account : accounts) {
            if (account instanceof Bip44Account) {
               Bip44Account ba = (Bip44Account) account;
               Balance balance = ba.getBalance();
               if (balance.getReceivingBalance() + balance.getSpendableBalance() > 0) {
                  // We have an HD account with funds, and no master seed backup, tell the user to act
                  return Notice.BACKUP_MISSING;
               }
            }
         }
      }

      // Then check if there are some SingleAddressAccounts with funds on it
      for (WalletAccount account : accounts){
         if (account instanceof SingleAddressAccount && account.canSpend()){
            if (meta.getOtherAccountBackupState(account.getId()) != MetadataStorage.BackupState.VERIFIED){
               Balance balance = account.getBalance();
               if (balance.getReceivingBalance() + balance.getSpendableBalance() > 0){
                  return Notice.SINGLEKEY_BACKUP_MISSING;
               }
            }
         }
      }

      // Second check whether to warn about legacy accounts with funds
      for (WalletAccount account : accounts) {
         if (RecordRowBuilder.showLegacyAccountWarning(account, _mbwManager)) {
            return Notice.MOVE_LEGACY_FUNDS;
         }
      }

      return Notice.NONE;
   }

   private OnClickListener noticeClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         switch (_notice) {
            case RESET_PIN_AVAILABLE:
            case RESET_PIN_IN_PROGRESS:
               showPinResetWarning();
               break;
            case BACKUP_MISSING:
               showBackupWarning();
               break;
            case SINGLEKEY_BACKUP_MISSING:
               showSingleKeyBackupWarning();
               break;
            case MOVE_LEGACY_FUNDS:
               showMoveLegacyFundsWarning();
               break;
            default:
               break;
         }
      }

   };

   private void showPinResetWarning() {
      Optional<Integer> resetPinRemainingBlocksCount = _mbwManager.getResetPinRemainingBlocksCount();

      if (!resetPinRemainingBlocksCount.isPresent()){
         recheckNotice();
         return;
      }

      if (resetPinRemainingBlocksCount.get()==0){
         // delay is done
         _mbwManager.showClearPinDialog(this.getActivity(), Optional.<Runnable>of(new Runnable() {
            @Override
            public void run() {
               recheckNotice();
            }
         }));
         return;
      }

      // delay is still remaining, provide option to abort
      String remaining = Utils.formatBlockcountAsApproxDuration(this.getActivity(), resetPinRemainingBlocksCount.or(1));
      new AlertDialog.Builder(this.getActivity())
            .setMessage(String.format(this.getActivity().getString(R.string.pin_forgotten_abort_pin_reset), remaining))
            .setTitle(this.getActivity().getString(R.string.pin_forgotten_reset_pin_dialog_title))
            .setPositiveButton(this.getActivity().getString(R.string.yes), new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                  _mbwManager.getMetadataStorage().clearResetPinStartBlockheight();
                  recheckNotice();
               }
            })
            .setNegativeButton(this.getActivity().getString(R.string.no), new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                  // nothing to do here
               }
            })
            .show();
   }

   private OnClickListener warningClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         if (!shouldWarnAboutHeartbleedBug()) {
            return;
         }
         Utils.showSimpleMessageDialog(getActivity(), R.string.heartbleed_alert);
      }
   };

   private void showBackupWarning() {
      if (!isAdded()) {
         return;
      }
      Utils.pinProtectedWordlistBackup(getActivity());
   }

   private void showSingleKeyBackupWarning() {
      if (!isAdded()) {
         return;
      }
      Utils.pinProtectedBackup(getActivity());
   }

   private void showMoveLegacyFundsWarning() {
      if (!isAdded()) {
         return;
      }
      Utils.showSimpleMessageDialog(getActivity(), R.string.move_legacy_funds_message);
   }

   private boolean shouldWarnAboutHeartbleedBug() {
      // The Heartbleed bug is only present in Android version 4.1.1
      return Build.VERSION.RELEASE.equals("4.1.1");
   }


   //this got replaced by VerifyWordlistBackup, but stays here unused, in case we ever need again the old backup functionality
   private class VerifyBackupDialog extends Dialog {

      public VerifyBackupDialog(final Activity activity) {
         super(activity);
         this.setContentView(R.layout.backup_verification_warning_dialog);
         this.setTitle(R.string.verify_backup_title);

         findViewById(R.id.btBackup).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               VerifyBackupDialog.this.dismiss();
               Utils.pinProtectedBackup(activity);
            }

         });

         findViewById(R.id.btVerifyBackup).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               VerifyBackupDialog.this.dismiss();
               VerifyBackupActivity.callMe(activity);
            }

         });
      }
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }

      // Show button, that a PIN reset is in progress and allow to abort it
      _root.findViewById(R.id.btPinResetNotice).setVisibility(_notice == Notice.RESET_PIN_AVAILABLE || _notice == Notice.RESET_PIN_IN_PROGRESS ? View.VISIBLE : View.GONE);

      // Only show the "Secure My Funds" button when necessary
      _root.findViewById(R.id.btBackupMissing).setVisibility(_notice == Notice.BACKUP_MISSING || _notice == Notice.SINGLEKEY_BACKUP_MISSING ? View.VISIBLE : View.GONE);

      // Only show the heartbleed warning when necessary
      _root.findViewById(R.id.btWarning).setVisibility(shouldWarnAboutHeartbleedBug() ? View.VISIBLE : View.GONE);

   }

   private void recheckNotice() {
      Notice notice = determineNotice();
      if (_notice != notice) {
         _notice = notice;
         updateUi();
      }
   }

   @Subscribe
   public void accountChanged(AccountChanged event) {
      recheckNotice();
   }

   @Subscribe
   public void balanceChanged(BalanceChanged event) {
      recheckNotice();
   }


}
