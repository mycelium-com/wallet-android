/*
 * Copyright 2015 Megion Research and Development GmbH
 * Copyright 2015 Ledger
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

package com.mycelium.wallet.extsig.ledger.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wallet.extsig.common.activity.ExtSigAccountImportActivity;
import com.mycelium.wallet.extsig.ledger.LedgerManager;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;
import nordpol.android.TagDispatcher;

import java.util.List;
import java.util.UUID;


public class LedgerAccountImportActivity extends LedgerAccountSelectorActivity implements LoaderManager.LoaderCallbacks<UUID> {

   private static final String ITEM_WRAPPER = "ITEM_WRAPPER";

   private final LedgerManager ledgerManager = MbwManager.getInstance(this).getLedgerManager();
   private TagDispatcher dispatcher;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      dispatcher = TagDispatcher.get(this, ledgerManager);
   }

   @Override
   protected void onResume() {
      super.onResume();
      updateUi();
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
         dispatcher.enableExclusiveNfc();
      }
   }

   @Override
   protected void onPause() {
      super.onPause();
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
         dispatcher.disableExclusiveNfc();
      }
   }

   @Override
   protected void onNewIntent(Intent intent) {
      dispatcher.interceptIntent(intent);
   }

   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, LedgerAccountImportActivity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @Override
   protected AdapterView.OnItemClickListener accountClickListener() {
      return new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(ITEM_WRAPPER,(HdAccountWrapper) adapterView.getItemAtPosition(i));
            getLoaderManager().initLoader(1, bundle, LedgerAccountImportActivity.this).forceLoad();

            ProgressDialog dialog = new ProgressDialog(LedgerAccountImportActivity.this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setTitle(getString(R.string.hardware_account_create));
            dialog.setMessage(getString(R.string.please_wait_hardware));
            dialog.show();
         }
      };
   }


   @NonNull
   @Override
   public Loader onCreateLoader(int i, @Nullable Bundle bundle) {
      return new LedgerAccountImportActivity.AccountCreationLoader(getApplicationContext(),
              (HdAccountWrapper) bundle.getSerializable(ITEM_WRAPPER), ledgerManager);
   }

   @Override
   public void onLoadFinished(@NonNull Loader loader, UUID uuid) {
      Intent result = new Intent();
      result.putExtra("account", uuid);
      setResult(RESULT_OK, result);
      finish();
   }

   @Override
   public void onLoaderReset(@NonNull Loader loader) { }

   public static class AccountCreationLoader extends AsyncTaskLoader<UUID> {

      private HdAccountWrapper item;
      private final LedgerManager ledgerManager;

      AccountCreationLoader(@NonNull Context context, HdAccountWrapper item, LedgerManager ledgerManager) {
         super(context);
         this.item = item;
         this.ledgerManager = ledgerManager;
      }

      @Nullable
      @Override
      public UUID loadInBackground() {

         // create the new account and get the uuid of it
         MbwManager mbwManager = MbwManager.getInstance(getContext());

         UUID acc = mbwManager.getWalletManager(false)
                 .createExternalSignatureAccount(
                         item.publicKeyNodes,
                         ledgerManager,
                         item.accountHdKeysPaths.iterator().next().getLastIndex());

         // Mark this account as backup warning ignored
         mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);

         return acc;
      }

   }

   @Override
   protected void updateUi() {
      super.updateUi();
      if (masterseedScanManager.getCurrentAccountState() == AccountScanManager.AccountStatus.done) {
         findViewById(R.id.btNextAccount).setEnabled(true);
      } else {
         findViewById(R.id.btNextAccount).setEnabled(false);
      }
   }

   @Override
   protected void setView() {
      setContentView(R.layout.activity_instant_ledger);
      ((TextView) findViewById(R.id.tvCaption)).setText(getString(R.string.ledger_import_account_caption));
      ((TextView) findViewById(R.id.tvSelectAccount)).setText(getString(R.string.ledger_select_account_to_import));
      ((TextView) findViewById(R.id.btNextAccount)).setVisibility(View.VISIBLE);

      findViewById(R.id.btNextAccount).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            Utils.showSimpleMessageDialog(LedgerAccountImportActivity.this, getString(R.string.ledger_next_unused_account_info), new Runnable() {
               @Override
               public void run() {

                  // The TEE calls are asynchronous, and simulate a blocking call
                  // To avoid locking up the UI thread, a new one is started

                  new Thread() {
                     public void run() {
                        List<HdKeyNode> nextAccounts = masterseedScanManager.getNextUnusedAccounts();

                        MbwManager mbwManager = MbwManager.getInstance(LedgerAccountImportActivity.this);

                        if (!nextAccounts.isEmpty()) {
                           final UUID acc = mbwManager.getWalletManager(false)
                                 .createExternalSignatureAccount(
                                       nextAccounts,
                                       (LedgerManager) masterseedScanManager,
                                       nextAccounts.get(0).getIndex()
                                 );

                           mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);

                           runOnUiThread(new Runnable() {
                              public void run() {
                                 Intent result = new Intent();
                                 result.putExtra("account", acc);
                                 setResult(RESULT_OK, result);
                                 finish();
                              }
                           });
                        }

                     }
                  }.start();

               }
            });
         }
      });
   }

   @Subscribe()
   public void onPinRequest(LedgerManager.OnPinRequest event) {
      LedgerPinDialog pin = new LedgerPinDialog(this, true);
      pin.setTitle(R.string.ledger_enter_pin);
      pin.setOnPinValid(new PinDialog.OnPinEntered() {
         @Override
         public void pinEntered(PinDialog dialog, Pin pin) {
            ((LedgerManager) masterseedScanManager).enterPin(pin.getPin());
            dialog.dismiss();
         }
      });
      pin.show();
   }

   // Otto.EventBus does not traverse class hierarchy to find subscribers
   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event){
      super.onScanError(event);
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event){
      super.onStatusChanged(event);
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event){
      super.onAccountFound(event);
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event){
      super.onPassphraseRequest(event);
   }

}
