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

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;

import java.util.UUID;


public abstract class ExtSigAccountImportActivity extends ExtSigAccountSelectorActivity {
   @Override
   protected AdapterView.OnItemClickListener accountClickListener() {
      return new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            HdAccountWrapper item = (HdAccountWrapper) adapterView.getItemAtPosition(i);

            // create the new account and get the uuid of it
            MbwManager mbwManager = MbwManager.getInstance(ExtSigAccountImportActivity.this);

            UUID acc = mbwManager.getWalletManager(false)
                  .createExternalSignatureAccount(
                        item.xPub,
                        (ExternalSignatureDeviceManager) masterseedScanManager,
                        item.accountHdKeyPath.getLastIndex()
                  );

            // Mark this account as backup warning ignored
            mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);

            Intent result = new Intent();
            result.putExtra("account", acc);
            setResult(RESULT_OK, result);
            finish();
         }
      };
   }

   @Override
   protected void updateUi() {
      super.updateUi();
      if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.done) {
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
      ((TextView) findViewById(R.id.btNextAccount)).setVisibility(View.VISIBLE);

      findViewById(R.id.btNextAccount).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            Utils.showSimpleMessageDialog(ExtSigAccountImportActivity.this, getString(R.string.ext_sig_next_unused_account_info), new Runnable() {
               @Override
               public void run() {

                  Optional<HdKeyNode> nextAccount = masterseedScanManager.getNextUnusedAccount();

                  MbwManager mbwManager = MbwManager.getInstance(ExtSigAccountImportActivity.this);

                  if (nextAccount.isPresent()) {
                     UUID acc = mbwManager.getWalletManager(false)
                           .createExternalSignatureAccount(
                                 nextAccount.get(),
                                 (ExternalSignatureDeviceManager) masterseedScanManager,
                                 nextAccount.get().getIndex()
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
