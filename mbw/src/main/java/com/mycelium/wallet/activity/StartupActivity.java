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
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.DecryptBip38PrivateKeyActivity;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.news.NewsUtils;
import com.mycelium.wallet.activity.pop.PopActivity;
import com.mycelium.wallet.activity.send.GetSpendingRecordActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;
import com.mycelium.wallet.content.actions.HdNodeAction;
import com.mycelium.wallet.content.actions.PrivateKeyAction;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.AccountCreated;
import com.mycelium.wallet.event.MigrationPercentChanged;
import com.mycelium.wallet.event.MigrationStatusChanged;
import com.mycelium.wallet.fio.FioRequestNotificator;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.content.AssetUri;
import com.mycelium.wapi.content.PrivateKeyUri;
import com.mycelium.wapi.content.WithCallback;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig;
import com.mycelium.wapi.wallet.fio.FIOMasterseedConfig;
import com.mycelium.wapi.wallet.manager.Config;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StartupActivity extends Activity implements AccountCreatorHelper.AccountCreationObserver {
   private static final int MINIMUM_SPLASH_TIME = 500;
   private static final int REQUEST_FROM_URI = 2;
   private static final int IMPORT_WORDLIST = 0;

   public static final List<Config> mainAccounts = new ArrayList<>(
           Arrays.asList(new AdditionalHDAccountConfig(), new EthereumMasterseedConfig(), new FIOMasterseedConfig()));

   private static final String LAST_STARTUP_TIME = "startupTme";

   private MbwManager _mbwManager;
   private AlertDialog _alertDialog;
   private PinDialog _pinDialog;
   private ProgressDialog _progress;
   private Bus eventBus;
   @BindView(R.id.progressBar)
   ProgressBar progressBar;
   @BindView(R.id.status)
   TextView status;
   private SharedPreferences sharedPreferences;
   private long lastStartupTime;
   private boolean isFirstRun;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      sharedPreferences = getApplicationContext().getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
      isFirstRun = (PreferenceManager.getDefaultSharedPreferences(
              getApplicationContext()).getInt("ckChangeLog_last_version_code", -1) == -1);
      lastStartupTime = sharedPreferences.getLong(LAST_STARTUP_TIME, TimeUnit.SECONDS.toMillis(10));
      _progress = new ProgressDialog(this);
      setContentView(R.layout.startup_activity);
      ButterKnife.bind(this);
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
   }

   @Override
   protected void onStart() {
      super.onStart();
      eventBus = MbwManager.getEventBus();
      eventBus.register(this);
      new Thread(delayedInitialization).start();
   }

   @Override
   public void onStop() {
      _progress.dismiss();
      if (_pinDialog != null) {
         _pinDialog.dismiss();
      }
      eventBus.unregister(this);
      super.onStop();
   }

   @Override
   protected void onDestroy() {
      if (_alertDialog != null && _alertDialog.isShowing()) {
         _alertDialog.dismiss();
      }
      super.onDestroy();
   }

   @Subscribe
   public void onMigrationProgressChanged(MigrationPercentChanged migrationPercent) {
      progressBar.setProgress(migrationPercent.getPercent());
   }

   @Subscribe
   public void onMigrationCommentChanged(MigrationStatusChanged migrationStatusChanged) {
      status.setText(StartupActivityHelperKt.format(migrationStatusChanged.getNewStatus(), getApplicationContext()));
   }

   private Runnable delayedInitialization = new Runnable() {
      @Override
      public void run() {
         if (lastStartupTime > TimeUnit.SECONDS.toMillis(5) && !isFirstRun) {
            new Handler(getMainLooper()).post(new Runnable() {
               @Override
               public void run() {
                  progressBar.setVisibility(View.VISIBLE);
                  status.setVisibility(View.VISIBLE);
               }
            });
         }
         long startTime = System.currentTimeMillis();
         _mbwManager = MbwManager.getInstance(StartupActivity.this.getApplication());

         // in case this is a fresh startup, import backup or create new seed
         if (!_mbwManager.getMasterSeedManager().hasBip32MasterSeed()) {
            new Handler(getMainLooper()).post(new Runnable() {
               @Override
               public void run() {
                  initMasterSeed();
               }
            });
            return;
         }

         // in case the masterSeed was created but account does not exist yet (rotation problem)
         if (_mbwManager.getWalletManager(false).getActiveSpendingAccounts().isEmpty()) {
            new AccountCreatorHelper.CreateAccountAsyncTask(StartupActivity.this, StartupActivity.this, mainAccounts).execute();
            return;
         }

         List<Config> needsToBeCreatedMasterSeedAccounts = _mbwManager.checkMainAccountsCreated();
         if (needsToBeCreatedMasterSeedAccounts.size() != 0) {
            new AccountCreatorHelper.CreateAccountAsyncTask(StartupActivity.this,
                    StartupActivity.this, needsToBeCreatedMasterSeedAccounts).execute();
            return;
         }

         // Calculate how much time we spent initializing, and do a delayed
         // finish so we display the splash a minimum amount of time
         long timeSpent = System.currentTimeMillis() - startTime;
         long remainingTime = MINIMUM_SPLASH_TIME - timeSpent;
         if (remainingTime < 0) {
            remainingTime = 0;
         }

         new Handler(getMainLooper()).postDelayed(delayedFinish, remainingTime);
         sharedPreferences.edit()
                 .putLong(LAST_STARTUP_TIME, timeSpent)
                 .apply();
      }
   };

   private void initMasterSeed() {
      new AlertDialog
              .Builder(this)
              .setCancelable(false)
              .setTitle(R.string.master_seed_configuration_title)
              .setMessage(getString(R.string.master_seed_configuration_description))
              .setNegativeButton(R.string.master_seed_restore_backup_button, new DialogInterface.OnClickListener() {
                 //import master seed from wordlist
                 public void onClick(DialogInterface arg0, int arg1) {
                    EnterWordListActivity.callMe(StartupActivity.this, IMPORT_WORDLIST);
                 }
              })
              .setPositiveButton(R.string.master_seed_create_new_button, new DialogInterface.OnClickListener() {
                 //configure new random seed
                 public void onClick(DialogInterface arg0, int arg1) {
                    startMasterSeedTask();
                 }
              })
              .show();
   }

   private void startMasterSeedTask() {
      _progress.setCancelable(false);
      _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      _progress.setMessage(getString(R.string.preparing_wallet_on_first_startup_info));
      _progress.show();
      new ConfigureSeedAsyncTask(new WeakReference<>(this)).execute();
   }

   private static class ConfigureSeedAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private WeakReference<StartupActivity> startupActivity;

      ConfigureSeedAsyncTask(WeakReference<StartupActivity> startupActivity) {
         this.startupActivity = startupActivity;
      }

      @Override
      protected UUID doInBackground(Void... params) {
         StartupActivity activity = this.startupActivity.get();
         if (activity == null) {
            return null;
         }
         Bip39.MasterSeed masterSeed = Bip39.createRandomMasterSeed(activity._mbwManager.getRandomSource());
         try {
            activity._mbwManager.getMasterSeedManager().configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());
            return activity._mbwManager.createAdditionalBip44AccountsUninterruptedly(mainAccounts).get(0);
         } catch (KeyCipher.InvalidKeyCipher e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      protected void onPostExecute(UUID accountId) {
         StartupActivity activity = this.startupActivity.get();
         if (accountId == null || activity == null) {
            return;
         }
         activity._progress.dismiss();
         MbwManager.getEventBus().post(new AccountCreated(accountId));
         MbwManager.getEventBus().post(new AccountChanged(accountId));
         //finish initialization
         activity.delayedFinish.run();
      }
   }

   @Override
   public void onAccountCreated(UUID accountId) {
      MbwManager.getEventBus().post(new AccountCreated(accountId));
      MbwManager.getEventBus().post(new AccountChanged(accountId));
      delayedFinish.run();
   }

   private final Runnable delayedFinish = new Runnable() {
      @Override
      public void run() {
         final MbwManager manager = _mbwManager;
         final boolean isUnlockPinRequired = manager != null && manager.isUnlockPinRequired();
         if (isUnlockPinRequired) {

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

         // Check if we have lingering exported private keys, we want to warn
         // the user if that is the case
         boolean _hasClipboardExportedPrivateKeys = hasPrivateKeyOnClipboard(_mbwManager.getNetwork());
         boolean hasClipboardExportedPublicKeys = hasPublicKeyOnClipboard(_mbwManager.getNetwork());

         if(hasClipboardExportedPublicKeys){
            warnUserOnClipboardKeys(false);
         }
         else if (_hasClipboardExportedPrivateKeys) {
            warnUserOnClipboardKeys(true);
         }
         else {
            normalStartup();
         }
      }

      private boolean hasPrivateKeyOnClipboard(NetworkParameters network) {
         // do we have a private key on the clipboard?
         try {
            InMemoryPrivateKey key = PrivateKeyAction.getPrivateKey(network, Utils.getClipboardString(StartupActivity.this));
            if (key != null) {
               return true;
            }
            HdKeyNode.parse(Utils.getClipboardString(StartupActivity.this), network);
            return true;
         } catch (HdKeyNode.KeyGenerationException ex) {
            return false;
         }
      }

      private boolean hasPublicKeyOnClipboard(NetworkParameters network) {
         // do we have a public key on the clipboard?
         try {
            if (HdNodeAction.Companion.isKeyNode(network, Utils.getClipboardString(StartupActivity.this))) {
               return true;
            }
            HdKeyNode.parse(Utils.getClipboardString(StartupActivity.this), network);
            return true;
         } catch (HdKeyNode.KeyGenerationException ex) {
            return false;
         }
      }
   };

   private void warnUserOnClipboardKeys(boolean isPrivate) {
      _alertDialog = new AlertDialog.Builder(this)
              // Set title
              .setTitle(isPrivate ? R.string.found_clipboard_private_key_title
                      : R.string.found_clipboard_public_key_title)
              // Set dialog message
              .setMessage(isPrivate ? R.string.found_clipboard_private_keys_message
                      : R.string.found_clipboard_public_keys_message)
              // Yes action
              .setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                    Utils.clearClipboardString(StartupActivity.this);
                    normalStartup();
                    dialog.dismiss();
                 }
              })
              // No action
              .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                    normalStartup();
                    dialog.cancel();
                 }
              })
              .create();
      _alertDialog.show();
   }

   private void normalStartup() {
       // Normal startup, show the selected account in the BalanceActivity
       Intent intent = new Intent(StartupActivity.this, ModernMain.class);
       if (Objects.equals(getIntent().getAction(), NewsUtils.MEDIA_FLOW_ACTION)) {
           intent.setAction(NewsUtils.MEDIA_FLOW_ACTION);
           if(getIntent().getExtras() != null) {
              intent.putExtras(getIntent().getExtras());
           }
       } else if (Objects.equals(getIntent().getAction(), FioRequestNotificator.FIO_REQUEST_ACTION)) {
          intent.setAction(FioRequestNotificator.FIO_REQUEST_ACTION);
          if (getIntent().getExtras() != null) {
             intent.putExtras(getIntent().getExtras());
          }
       } else if(getIntent().hasExtra("action")) {
          if (getIntent().getExtras() != null) {
             intent.putExtras(getIntent().getExtras());
          }
       }
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
            switch (scheme) {
               case "bitcoin":
                  handleUri(intentUri);
                  break;
               case "bitid":
                  handleBitIdUri(intentUri);
                  break;
               case "btcpop":
                  handlePopUri(intentUri);
                  break;
               case "mycelium":
                  handleMyceliumUri(intentUri);
                  break;
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

         List<WalletAccount<?>> spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccountsWithBalance();
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
         new Toaster(this).toast(R.string.file_not_found, false);
         finish();
      } catch (IOException e) {
         new Toaster(this).toast(R.string.payment_request_unable_to_read_payment_request, false);
         finish();
      }
   }

   private void handleMyceliumUri(Uri intentUri) {
      final String host = intentUri.getHost();
      // If we dont understand the url, just call the balance screen
      Intent balanceIntent = new Intent(this, ModernMain.class);
      startActivity(balanceIntent);
      // close the startup activity to not pollute the backstack
      finish();
   }

   private void handleBitIdUri(Uri intentUri) {
      //We have been launched by a bitid authentication request
      Optional<BitIDSignRequest> bitid = BitIDSignRequest.parse(intentUri);
      if (!bitid.isPresent()) {
         //Invalid bitid URI
         new Toaster(this).toast(R.string.invalid_bitid_uri, false);
         finish();
         return;
      }
      Intent bitIdIntent = new Intent(this, BitIDAuthenticationActivity.class)
              .putExtra("request", bitid.get());
      startActivity(bitIdIntent);

      finish();
   }

   private void handlePopUri(Uri intentUri) {
      // a proof of payment request
      PopRequest popRequest = new PopRequest(intentUri.toString());
      Intent popIntent = new Intent(this, PopActivity.class)
              .putExtra("popRequest", popRequest);
      startActivity(popIntent);
      finish();
   }

   private void handleUri(Uri intentUri) {
      // We have been launched by a Bitcoin URI
      MbwManager mbwManager = MbwManager.getInstance(getApplication());
      AssetUri uri = mbwManager.getContentResolver().resolveUri(intentUri.toString());
      if (uri == null) {
         // Invalid Bitcoin URI
         new Toaster(this).toast(R.string.invalid_bitcoin_uri, false);
         finish();
         return;
      }

      // the bitcoin uri might actually be encrypted private key, where the user wants to spend funds from
      if (uri instanceof PrivateKeyUri) {
         final PrivateKeyUri privateKeyUri = (PrivateKeyUri) uri;
         DecryptBip38PrivateKeyActivity.callMe(this, privateKeyUri.getKeyString(), StringHandlerActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE);
      } else {
         if (uri.getAddress() == null && uri instanceof WithCallback && Strings.isNullOrEmpty(((WithCallback) uri).getCallbackURL())) {
            // Invalid Bitcoin URI
            new Toaster(this).toast(R.string.invalid_bitcoin_uri, false);
            finish();
            return;
         }

         List<WalletAccount<?>> spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccountsWithBalance();
         if (spendingAccounts.isEmpty()) {
            //if we dont have an account which can spend and has a balance, we fetch all accounts with priv keys
            spendingAccounts = mbwManager.getWalletManager(false).getSpendingAccounts();
         }
         if (spendingAccounts.size() == 1) {
            SendInitializationActivity.callMeWithResult(this, spendingAccounts.get(0).getId(), uri, false, REQUEST_FROM_URI);
         } else {
            GetSpendingRecordActivity.callMeWithResult(this, uri, REQUEST_FROM_URI);
         }
         //don't finish just yet we want to stay on the stack and observe that we emit a txid correctly.
      }
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      //todo make sure delayed init has finished (ev mit countdownlatch)
      switch (requestCode) {
         case IMPORT_WORDLIST:
            if (resultCode != RESULT_OK) {
               //user cancelled the import, so just ask what he wants again
               initMasterSeed();
               return;
            }
            //finish initialization
            delayedFinish.run();
            return;
         case StringHandlerActivity.IMPORT_ENCRYPTED_BIP38_PRIVATE_KEY_CODE:
            String content = data.getStringExtra("base58Key");
            if (content != null) {
               InMemoryPrivateKey key = InMemoryPrivateKey.fromBase58String(content, _mbwManager.getNetwork()).get();
               UUID onTheFlyAccount = MbwManager.getInstance(this).createOnTheFlyAccount(key, Utils.getBtcCoinType());
               SendInitializationActivity.callMe(this, onTheFlyAccount, true);
               finish();
               return;
            }
         case REQUEST_FROM_URI:
            // double-check result data, in case some downstream code messes up.
            if (resultCode == RESULT_OK) {
               Bundle extras = Preconditions.checkNotNull(data.getExtras());
               for(String key: extras.keySet()) {
                  // make sure we only share TRANSACTION_ID_INTENT_KEY with external caller
                  if(!key.equals(Constants.TRANSACTION_ID_INTENT_KEY)) {
                     data.removeExtra(key);
                  }
               }
               // return the tx hash to our external caller, if he cares...
               setResult(RESULT_OK, data);
            } else {
               setResult(RESULT_CANCELED);
            }
            break;
         default:
            setResult(RESULT_CANCELED);
      }
      finish();
   }
}
