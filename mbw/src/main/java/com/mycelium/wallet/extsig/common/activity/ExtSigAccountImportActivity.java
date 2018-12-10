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

package com.mycelium.wallet.extsig.common.activity;

import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.UUID;


public abstract class ExtSigAccountImportActivity extends ExtSigAccountSelectorActivity implements LoaderManager.LoaderCallbacks<UUID> {

   private static final String ITEM_WRAPPER = "ITEM_WRAPPER";

   @Override
   protected AdapterView.OnItemClickListener accountClickListener() {
      return new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(ITEM_WRAPPER,(HdAccountWrapper) adapterView.getItemAtPosition(i));
            getLoaderManager().initLoader(1, bundle, ExtSigAccountImportActivity.this).forceLoad();

            ProgressDialog dialog = new ProgressDialog(ExtSigAccountImportActivity.this);
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
      return new AccountCreationLoader(getApplicationContext(),
              (HdAccountWrapper) bundle.getSerializable(ITEM_WRAPPER), (ExternalSignatureDeviceManager) masterseedScanManager);
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
      private final ExternalSignatureDeviceManager masterseedScanManager;

      AccountCreationLoader(@NonNull Context context, HdAccountWrapper item, ExternalSignatureDeviceManager masterseedScanManager) {
         super(context);
         this.item = item;
         this.masterseedScanManager = masterseedScanManager;
      }

      @Nullable
      @Override
      public UUID loadInBackground() {

         // create the new account and get the uuid of it
         MbwManager mbwManager = MbwManager.getInstance(getContext());

         UUID acc = mbwManager.getWalletManager(false)
                 .createExternalSignatureAccount(
                         item.publicKeyNodes,
                         masterseedScanManager ,
                         item.accountHdKeysPaths.iterator().next().getLastIndex()
                 );

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
      setContentView(R.layout.activity_instant_ext_sig);
      ((TextView) findViewById(R.id.tvCaption)).setText(getString(R.string.ext_sig_import_account_caption));
      ((TextView) findViewById(R.id.tvSelectAccount)).setText(getString(R.string.ext_sig_select_account_to_import));
      findViewById(R.id.btNextAccount).setVisibility(View.VISIBLE);

      findViewById(R.id.btNextAccount).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            Utils.showSimpleMessageDialog(ExtSigAccountImportActivity.this, getString(R.string.ext_sig_next_unused_account_info), new Runnable() {
               @Override
               public void run() {
                  List<HdKeyNode> nextAccount = masterseedScanManager.getNextUnusedAccounts();

                  MbwManager mbwManager = MbwManager.getInstance(ExtSigAccountImportActivity.this);

                  if (!nextAccount.isEmpty()) {
                     UUID acc = mbwManager.getWalletManager(false)
                           .createExternalSignatureAccount(
                                 nextAccount,
                                 (ExternalSignatureDeviceManager) masterseedScanManager,
                                 nextAccount.get(0).getIndex()
                           );

                     mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);

                     Intent result = new Intent();
                     result.putExtra("account", acc);
                     setResult(RESULT_OK, result);
                     finish();
                  }
               }
            });
         }
      });
   }

   // Otto.EventBus does not traverse class hierarchy to find subscribers
   @Subscribe
   public void onPinMatrixRequest(ExternalSignatureDeviceManager.OnPinMatrixRequest event) {
      super.onPinMatrixRequest(event);
   }

   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event) {
      super.onScanError(event);
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event) {
      super.onStatusChanged(event);
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event) {
      super.onAccountFound(event);
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event) {
      super.onPassphraseRequest(event);
   }
}
