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
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.DecryptBip38PrivateKeyActivity;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.activity.pop.PopActivity;
import com.mycelium.wallet.activity.send.GetSpendingRecordActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;
import com.mycelium.wallet.external.glidera.activities.GlideraSendToNextStep;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44Account;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class StartupActivity extends Activity {
   private static final int MINIMUM_SPLASH_TIME = 500;
   private static final int REQUEST_FROM_URI = 2;
   private static final int IMPORT_WORDLIST = 0;

   private static final String URI_HOST_GLIDERA_REGISTRATION = "glideraRegistration";

   private boolean _hasClipboardExportedPrivateKeys;
   private MbwManager _mbwManager;
   private AlertDialog _alertDialog;
   private PinDialog _pinDialog;
   private ProgressDialog _progress;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      _progress = new ProgressDialog(this);
      setContentView(R.layout.startup_activity);
      // Do slightly delayed initialization so we get a chance of displaying the
      // splash before doing heavy initialization
      new Handler().postDelayed(delayedInitialization, 200);
   }

   @Override
   public void onPause() {
      _progress.dismiss();
      if (_pinDialog != null) {
         _pinDialog.dismiss();
      }
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      if (_alertDialog != null && _alertDialog.isShowing()) {
         _alertDialog.dismiss();
      }
      super.onDestroy();
   }

   private Runnable delayedInitialization = new Runnable() {
      @Override
      public void run() {
         long startTime = System.currentTimeMillis();
         _mbwManager = MbwManager.getInstance(StartupActivity.this.getApplication());

         //in case this is a fresh startup, import backup or create new seed
         if (_mbwManager.getWalletManager(false).getAccountIds().isEmpty()) {
            initMasterSeed();
            //we return here, delayed finish will get posted once have our first account
            return;
         } else if (!_mbwManager.getWalletManager(false).hasBip32MasterSeed()) {
            //user has accounts, but no seed. we just create one for him
            //first show an upgrade info, then make seed
            showUpgradeInfo();
            //the Asynctask will execute delayedfinish
            return;
         }

         // Check if we have lingering exported private keys, we want to warn
         // the user if that is the case
         _hasClipboardExportedPrivateKeys = hasPrivateKeyOnClipboard(_mbwManager.getNetwork());
         // Calculate how much time we spent initializing, and do a delayed
         // finish so we display the splash a minimum amount of time
         long timeSpent = System.currentTimeMillis() - startTime;
         long remainingTime = MINIMUM_SPLASH_TIME - timeSpent;
         if (remainingTime < 0) {
            remainingTime = 0;
         }
         new Handler().postDelayed(delayedFinish, remainingTime);
      }

      private boolean hasPrivateKeyOnClipboard(NetworkParameters network) {
         // do we have a private key on the clipboard?
         try {
            new InMemoryPrivateKey(Utils.getClipboardString(StartupActivity.this), network);
            return true;
         } catch (IllegalArgumentException e) {
            return false;
         }
      }
   };

   private void showUpgradeInfo() {
      //show upgrade info
      new AlertDialog.Builder(this)
              .setTitle(R.string.title_upgrade_info)
              .setMessage(R.string.release_notes_hd)
              .setPositiveButton(R.string.button_continue, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                    // continue with master seed init
                    startMasterSeedTask();
                 }
              })
              .setNegativeButton(R.string.butto_show_release_notes, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                    // navigate to website
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Constants.MYCELIUM_2_RELEASE_NOTES_URL));
                    startActivity(intent);
                    //and get everything up while they read it :)
                    startMasterSeedTask();
                 }
              })
              .show();
   }

   private void initMasterSeed() {
      AlertDialog.Builder importDialog = new AlertDialog.Builder(this);
      importDialog.setCancelable(false);
      importDialog.setTitle(R.string.master_seed_configuration_title);
      importDialog.setMessage(getString(R.string.master_seed_configuration_description));
      importDialog.setNegativeButton(R.string.master_seed_restore_backup_button, new DialogInterface.OnClickListener() {
         //import master seed from wordlist
         public void onClick(DialogInterface arg0, int arg1) {
            EnterWordListActivity.callMe(StartupActivity.this, IMPORT_WORDLIST);
         }
      });
      importDialog.setPositiveButton(R.string.master_seed_create_new_button, new DialogInterface.OnClickListener() {
         //configure new random seed
         public void onClick(DialogInterface arg0, int arg1) {
            startMasterSeedTask();
         }
      });

      importDialog.show();
   }

   private void startMasterSeedTask() {
      _progress.setCancelable(false);
      _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      _progress.setMessage(getString(R.string.preparing_wallet_on_first_startup_info));
      _progress.show();
      new ConfigureSeedAsyncTask().execute();
   }

   private class ConfigureSeedAsyncTask extends AsyncTask<Void, Integer, UUID> {
      @Override
      protected UUID doInBackground(Void... params) {
         Bip39.MasterSeed masterSeed = Bip39.createRandomMasterSeed(_mbwManager.getRandomSource());
         try {
            WalletManager walletManager = _mbwManager.getWalletManager(false);
            walletManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());
            return walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
         } catch (KeyCipher.InvalidKeyCipher e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      protected void onPostExecute(UUID accountid) {
         _progress.dismiss();
         //set default label for the created HD account
         WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountid);
         String defaultName = getString(R.string.account) + " " + (((Bip44Account) account).getAccountIndex() + 1);
         _mbwManager.getMetadataStorage().storeAccountLabel(accountid, defaultName);
         //finish initialization
         delayedFinish.run();
      }
   }

   private Runnable delayedFinish = new Runnable() {

      @Override
      public void run() {
         if (_mbwManager.isUnlockPinRequired()) {

            // set a click handler to the background, so that
            // if the PIN-Pad closes, you can reopen it by touching the background
            getWindow().getDecorView().findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                  delayedFinish.run();
               }
            });

            Runnable start = new Runnable() {
               @Override
               public void run() {
                  _mbwManager.setStartUpPinUnlocked(true);
                  start();
               }
            };

            // set the pin dialog to not cancelable
            _pinDialog = _mbwManager.runPinProtectedFunction(StartupActivity.this, start, false);
         } else {
            start();
         }
      }

      private void start() {
         // Check whether we should handle this intent in a special way if it
         // has a bitcoin URI in it
         if (handleIntent()) {
            return;
         }

         if (_hasClipboardExportedPrivateKeys) {
            warnUserOnClipboardKeys();
         } else {
            normalStartup();
         }
      }

   };


   private void warnUserOnClipboardKeys() {
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

      // Set title
      alertDialogBuilder.setTitle(R.string.found_clipboard_private_key_title);
      // Set dialog message
      alertDialogBuilder.setMessage(R.string.found_clipboard_private_keys_message);
      // Yes action
      alertDialogBuilder.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            Utils.clearClipboardString(StartupActivity.this);
            normalStartup();
            dialog.dismiss();
         }
      });
      // No action
      alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            normalStartup();
            dialog.cancel();
         }
      });
      _alertDialog = alertDialogBuilder.create();
      _alertDialog.show();
   }

   private void normalStartup() {
      // Normal startup, show the selected account in the BalanceActivity
      Intent intent = new Intent(StartupActivity.this, ModernMain.class);
      startActivity(intent);
      finish();
   }

   private boolean handleIntent() {
      Intent intent = getIntent();
      final String action = intent.getAction();

      if ("application/bitcoin-paymentrequest".equals(intent.getType())) {
         // called via paymentrequest-file
         final Uri paymentRequest = intent.getData();
         handlePaymentRequest(paymentRequest);
         return true;
      } else {
         final Uri intentUri = intent.getData();
         final String scheme = intentUri != null ? intentUri.getScheme() : null;

         if (intentUri != null && (Intent.ACTION_VIEW.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))) {
            if ("bitcoin".equals(scheme)) {
               handleBitcoinUri(intentUri);
            } else if ("bitid".equals(scheme)) {
               handleBitIdUri(intentUri);
            } else if ("btcpop".equals(scheme)) {
               handlePopUri(intentUri);
            } else if ("mycelium".equals(scheme)) {
               handleMyceliumUri(intentUri);
            }
            return true;
         }
      }
      return false;
   }

   private void handlePaymentRequest(Uri paymentRequest) {
      try {
         InputStream inputStream = getContentResolver().openInputStream(paymentRequest);
         byte[] bytes = ByteStreams.toByteArray(inputStream);

         MbwManager mbwManager = MbwManager.getInstance(StartupActivity.this.getApplication());

         List<WalletAccount> spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccountsWithBalance();
         if (spendingAccounts.isEmpty()) {
            //if we dont have an account which can spend and has a balance, we fetch all accounts with priv keys
            spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccounts();
         }
         if (spendingAccounts.size() == 1) {
            SendInitializationActivity.callMeWithResult(this, spendingAccounts.get(0).getId(), bytes, false, REQUEST_FROM_URI);
         } else {
            GetSpendingRecordActivity.callMeWithResult(this, bytes, REQUEST_FROM_URI);
         }
      } catch (FileNotFoundException e) {
         Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_LONG).show();
         finish();
      } catch (IOException e) {
         Toast.makeText(this, getString(R.string.payment_request_unable_to_read_payment_request), Toast.LENGTH_LONG).show();
         finish();
      }
   }

   private void handleMyceliumUri(Uri intentUri) {
      final String host = intentUri.getHost();
      if (host.equals(URI_HOST_GLIDERA_REGISTRATION)) {
         Intent glideraIntent = new Intent(this, GlideraSendToNextStep.class);
         glideraIntent.putExtra("uri", intentUri.toString());
         startActivity(glideraIntent);
      } else {
         // If we dont understand the url, just call the balance screen
         Intent balanceIntent = new Intent(this, ModernMain.class);
         startActivity(balanceIntent);
      }
      // close the startup activity to not pollute the backstack
      finish();
   }

   private void handleBitIdUri(Uri intentUri) {
      //We have been launched by a bitid authentication request
      Optional<BitIDSignRequest> bitid = BitIDSignRequest.parse(intentUri);
      if (!bitid.isPresent()) {
         //Invalid bitid URI
         Toast.makeText(this, R.string.invalid_bitid_uri, Toast.LENGTH_LONG).show();
         finish();
         return;
      }
      Intent bitIdIntent = new Intent(this, BitIDAuthenticationActivity.class);
      bitIdIntent.putExtra("request", bitid.get());
      startActivity(bitIdIntent);

      finish();
   }

   private void handlePopUri(Uri intentUri) {
      // a proof of payment request
      Intent popIntent = new Intent(this, PopActivity.class);
      PopRequest popRequest = new PopRequest(intentUri.toString());
      popIntent.putExtra("popRequest", popRequest);
      startActivity(popIntent);
      finish();
   }

   private void handleBitcoinUri(Uri intentUri) {
      // We have been launched by a Bitcoin URI
      MbwManager mbwManager = MbwManager.getInstance(StartupActivity.this.getApplication());
      Optional<? extends BitcoinUri> bitcoinUri = BitcoinUri.parse(intentUri.toString(), mbwManager.getNetwork());
      if (!bitcoinUri.isPresent()) {
         // Invalid Bitcoin URI
         Toast.makeText(this, R.string.invalid_bitcoin_uri, Toast.LENGTH_LONG).show();
         finish();
         return;
      }

      // the bitcoin uri might actually be encrypted private key, where the user wants to spend funds from
      if (bitcoinUri.get() instanceof BitcoinUri.PrivateKeyUri) {
         final BitcoinUri.PrivateKeyUri privateKeyUri = (BitcoinUri.PrivateKeyUri) bitcoinUri.get();
         DecryptBip38PrivateKeyActivity.callMe(this, privateKeyUri.keyString, StringHandlerActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE);
      } else {
         if (bitcoinUri.get().address == null && Strings.isNullOrEmpty(bitcoinUri.get().callbackURL)) {
            // Invalid Bitcoin URI
            Toast.makeText(this, R.string.invalid_bitcoin_uri, Toast.LENGTH_LONG).show();
            finish();
            return;
         }

         List<WalletAccount> spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccountsWithBalance();
         if (spendingAccounts.isEmpty()) {
            //if we dont have an account which can spend and has a balance, we fetch all accounts with priv keys
            spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccounts();
         }
         if (spendingAccounts.size() == 1) {
            SendInitializationActivity.callMeWithResult(this, spendingAccounts.get(0).getId(), bitcoinUri.get(), false, REQUEST_FROM_URI);
         } else {
            GetSpendingRecordActivity.callMeWithResult(this, bitcoinUri.get(), REQUEST_FROM_URI);
         }
         //don't finish just yet we want to stay on the stack and observe that we emit a txid correctly.
      }
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //todo make sure delayed init has finished (ev mit countdownlatch)
      if (requestCode == IMPORT_WORDLIST) {
         if (resultCode != RESULT_OK) {
            //user cancelled the import, so just ask what he wants again
            initMasterSeed();
            return;
         }
         //we have restored a backup
         UUID accountid = (UUID) data.getSerializableExtra(AddAccountActivity.RESULT_KEY);
         //set default label for the created HD account
         WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountid);
         String defaultName = getString(R.string.account) + " " + (((Bip44Account) account).getAccountIndex() + 1);
         _mbwManager.getMetadataStorage().storeAccountLabel(accountid, defaultName);
         //finish initialization
         delayedFinish.run();
         return;
      } else if (requestCode == StringHandlerActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE) {
         String content = data.getStringExtra("base58Key");
         if (content != null) {
            InMemoryPrivateKey key = InMemoryPrivateKey.fromBase58String(content, _mbwManager.getNetwork()).get();
            UUID account = MbwManager.getInstance(this).createOnTheFlyAccount(key);
            SendInitializationActivity.callMe(this, account, true);
            finish();
            return;
         }
      }

      // double-check result data, in case some downstream code messes up.
      if (requestCode == REQUEST_FROM_URI) {
         if (resultCode == RESULT_OK) {
            Bundle extras = Preconditions.checkNotNull(data.getExtras());
            Preconditions.checkState(extras.keySet().size() == 1); // check no additional data
            Preconditions.checkState(extras.getString(Constants.TRANSACTION_HASH_INTENT_KEY) != null);
            // return the tx hash to our external caller, if he cares...
            setResult(RESULT_OK, data);
         } else {
            setResult(RESULT_CANCELED);
         }
      } else {
         setResult(RESULT_CANCELED);
      }
      finish();
   }
}
