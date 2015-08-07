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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.coinapult.api.httpclient.CoinapultClient;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.CoinapultManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.HdAccountCreated;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Bus;

import java.util.UUID;

public class AddAccountActivity extends Activity {

   public static final int RESULT_COINAPULT = 2;

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

      findViewById(R.id.btAdvanced).setOnClickListener(advancedClickListener);
      findViewById(R.id.btHdCreate).setOnClickListener(createHdAccount);
      final View coinapultUSD = findViewById(R.id.btCoinapultCreate);
      coinapultUSD.setOnClickListener(createCoinapultAccount);
      coinapultUSD.setEnabled(!_mbwManager.getMetadataStorage().isPairedService("coinapult"));
      if (_mbwManager.getMetadataStorage().getMasterSeedBackupState() == MetadataStorage.BackupState.VERIFIED) {
         findViewById(R.id.tvWarningNoBackup).setVisibility(View.GONE);
      } else {
         findViewById(R.id.tvInfoBackup).setVisibility(View.GONE);
      }

      _progress = new ProgressDialog(this);
   }

   View.OnClickListener advancedClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
            @Override
            public void run() {
               AddAdvancedAccountActivity.callMe(AddAccountActivity.this, ADD_ADVANCED_CODE);
            }
         });
      }

   };

   View.OnClickListener createHdAccount = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
         _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
            @Override
            public void run() {
               createNewHdAccount();
            }
         });
      }
   };

   View.OnClickListener createCoinapultAccount = new View.OnClickListener() {
      @Override
      public void onClick(View view) {

         _mbwManager.getVersionManager().showFeatureWarningIfNeeded(
               AddAccountActivity.this, Feature.COINAPULT_NEW_ACCOUNT, true, new Runnable() {
                  @Override
                  public void run() {
                     _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
                        @Override
                        public void run() {
                           createCoinapultAccount();
                        }
                     });
                  }
               });

      }
   };

   private void createNewHdAccount() {
      final WalletManager wallet = _mbwManager.getWalletManager(false);
      // at this point, we have to have a master seed, since we created one on startup
      Preconditions.checkState(wallet.hasBip32MasterSeed());
      if (!wallet.canCreateAdditionalBip44Account()) {
         _toaster.toast(R.string.use_acc_first, false);
         return;
      }
      _progress.setCancelable(false);
      _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      _progress.setMessage(getString(R.string.hd_account_creation_started));
      _progress.show();
      new HdCreationAsyncTask(_mbwManager.getEventBus()).execute();
   }

   private void createCoinapultAccount() {

      AlertDialog.Builder b = new AlertDialog.Builder(this);
      b.setTitle(getString(R.string.coinapult_tos_question));
      View diaView = getLayoutInflater().inflate(R.layout.coinapult_tos, null);
      b.setView(diaView);
      b.setPositiveButton(getString(R.string.agree), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            askForMailAndAddCoinapultAccount();
         }
      });
      b.setNegativeButton(getString(R.string.dontagree), null);

      AlertDialog dialog = b.create();

      TextView link = (TextView) diaView.findViewById(R.id.tosLink);
      link.setClickable(true);
      link.setMovementMethod(LinkMovementMethod.getInstance());
      String linkUrl = getString(R.string.coinapult_tos_link_url);
      String text = "<a href='" + linkUrl + "'> " + link.getText() + "</a>";
      link.setText(Html.fromHtml(text));

      dialog.show();
   }

   private void askForMailAndAddCoinapultAccount() {
      AlertDialog.Builder b = new AlertDialog.Builder(this);
      b.setTitle(getString(R.string.coinapult_mail_question));
      View diaView = getLayoutInflater().inflate(R.layout.coinapult_mail, null);
      final EditText mailField = (EditText) diaView.findViewById(R.id.mail);
      b.setView(diaView);
      b.setPositiveButton(getString(R.string.button_done), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            _progress.setCancelable(false);
            _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            _progress.setMessage(getString(R.string.createCoinapult));
            _progress.show();
            String mailText = mailField.getText().toString();
            Optional<String> mail;
            if (mailText.isEmpty()) mail = Optional.absent(); else mail = Optional.of(mailText);
            new AddCoinapultAsyncTask(_mbwManager.getEventBus(), mail).execute();
         }
      });

      AlertDialog dialog = b.create();
      dialog.show();
   }

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
         bus.post(new AccountChanged(account));
      }
   }

   private class AddCoinapultAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private Bus bus;
      private Optional<String> mail;
      private CoinapultManager coinapultManager;

      public AddCoinapultAsyncTask(Bus bus, Optional<String> mail) {
         this.bus = bus;
         this.mail = mail;
      }

      @Override
      protected UUID doInBackground(Void... params) {
         _mbwManager.getMetadataStorage().setPairedService("coinapult", true);
         coinapultManager = _mbwManager.getCoinapultManager();
         try {
            coinapultManager.addUSD(mail);
         } catch (CoinapultClient.CoinapultBackendException e) {
            return null;
         }
         // at this point, we have to have a master seed, since we created one on startup
         return coinapultManager.getId();
      }

      @Override
      protected void onPostExecute(UUID account) {
         _progress.dismiss();
         if (account != null) {
            bus.post(new AccountChanged(account));
            Intent result = new Intent();
            result.putExtra(RESULT_KEY, coinapultManager.getId());
            setResult(RESULT_COINAPULT, result);
            finish();
         } else {
            Toast.makeText(AddAccountActivity.this, R.string.coinapult_unable_to_create_account, Toast.LENGTH_SHORT).show();
         }
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
      _mbwManager.getVersionManager().closeDialog();
      super.onPause();
   }
}
