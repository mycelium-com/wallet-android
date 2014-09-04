/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.WindowManager;

import com.mrd.bitlib.crypto.Bip39;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.event.HdAccountCreated;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Bus;

import java.util.UUID;

public class AddAccountActivity extends Activity {

   public static void callMe(Fragment fragment, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), AddAccountActivity.class);
      fragment.startActivityForResult(intent, requestCode);
   }

   public static final String RESULT_KEY = "account";
   private static final int IMPORT_SEED_CODE = 0;
   private static final int ADD_ADVANCED_CODE = 1;
   private Toaster _toaster;
   private MbwManager _mbwManager;
   private ProgressDialog _progress;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_account_activity);
      final Activity activity = AddAccountActivity.this;
      _mbwManager = MbwManager.getInstance(this);
      _toaster = new Toaster(this);

      findViewById(R.id.btAdvanced).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            AddAdvancedAccountActivity.callMe(activity, ADD_ADVANCED_CODE);
         }

      });

      View btHdCreate = findViewById(R.id.btHdCreate);
      btHdCreate.setOnClickListener(createHdAccount);
      _progress = new ProgressDialog(this);
   }

   View.OnClickListener createHdAccount = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
         final WalletManager wallet = _mbwManager.getWalletManager(false);
         // We can create an HD account if we have no master seed configured yet, or if the last account has had activity
         if (wallet.hasBip32MasterSeed() && !wallet.canCreateAdditionalBip44Account()) {
            _toaster.toast(R.string.use_acc_first, false);
            return;
         }

         if (wallet.hasBip32MasterSeed()) {
            _progress.setCancelable(false);
            _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            _progress.setMessage(getString(R.string.hd_creation_started));
            _progress.show();
            new HdCreationAsyncTask(_mbwManager.getEventBus()).execute();
            return;
         }

         AlertDialog.Builder importDialog = new AlertDialog.Builder(AddAccountActivity.this);
         importDialog.setTitle(R.string.master_seed_configuration_title);
         importDialog.setMessage(getString(R.string.master_seed_configuration_description));
         importDialog.setNegativeButton(R.string.master_seed_import_backup_button, new DialogInterface.OnClickListener() {
            //import master seed from qr code
            public void onClick(DialogInterface arg0, int arg1) {
               ImportSeedActivity.callMe(AddAccountActivity.this, IMPORT_SEED_CODE);
            }
         });
         importDialog.setPositiveButton(R.string.master_seed_create_new_button, new DialogInterface.OnClickListener() {
            //configure new random seed
            public void onClick(DialogInterface arg0, int arg1) {
               _progress.setCancelable(false);
               _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
               _progress.setMessage(getString(R.string.configure_seed_started));
               _progress.show();
               new ConfigureSeedAsyncTask(_mbwManager.getEventBus()).execute();
            }
         });
         importDialog.show();
      }
   };

   private class HdCreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private Bus bus;

      public HdCreationAsyncTask(Bus bus) {
         this.bus = bus;
      }

      @Override
      protected UUID doInBackground(Void... params) {
         try {
            return _mbwManager.getWalletManager(false).createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
         } catch (KeyCipher.InvalidKeyCipher e) {
            throw new RuntimeException(e);
         }

      }

      @Override
      protected void onPostExecute(UUID account) {
         bus.post(new HdAccountCreated(account));
      }
   }

   private class ConfigureSeedAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private Bus bus;

      public ConfigureSeedAsyncTask(Bus bus) {
         this.bus = bus;
      }

      @Override
      protected UUID doInBackground(Void... params) {
         Bip39.MasterSeed masterSeed = Bip39.createRandomMasterSeed(_mbwManager.getRandomSource());
         try {
            WalletManager walletManager = _mbwManager.getWalletManager(false);
            walletManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());
            UUID acc = walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
            return acc;
         } catch (KeyCipher.InvalidKeyCipher e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      protected void onPostExecute(UUID account) {
         bus.post(new HdAccountCreated(account));
      }
   }

   @com.squareup.otto.Subscribe
   public void hdAccountCreated(HdAccountCreated event) {
      _progress.dismiss();
      finishOk(event.account);
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == ADD_ADVANCED_CODE) {
         if (resultCode == RESULT_CANCELED) {
            //stay here
            return;
         }
         //just pass on what we got
         setResult(resultCode, intent);
         finish();
      } else if (requestCode == IMPORT_SEED_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            UUID account = (UUID) intent.getSerializableExtra(RESULT_KEY);
            finishOk(account);
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void finishOk(UUID account) {
      Intent result = new Intent();
      result.putExtra(RESULT_KEY, account);
      setResult(RESULT_OK, result);
      finish();
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      super.onResume();
   }

   @Override
   public void onPause() {
      _progress.dismiss();
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }
}
