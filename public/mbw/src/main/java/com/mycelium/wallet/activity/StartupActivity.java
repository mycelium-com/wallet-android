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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.WalletMode;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.activity.send.GetSpendingRecordActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;

public class StartupActivity extends Activity {

   private static final int MINIMUM_SPLASH_TIME = 500;
   private boolean _hasClipboardExportedPrivateKeys;
   private AlertDialog _alertDialog;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.startup_activity);
      // Do slightly delayed initialization so we get a chance of displaying the
      // splash before doing heavy initialization
      new Handler().postDelayed(delayedInitialization, 200);
   }

   @Override
   protected void onResume() {
      super.onResume();
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

         // This forces the Manager to load and create the initial key if
         // necessary
         MbwManager mbwManager = MbwManager.getInstance(StartupActivity.this.getApplication());

         // Check if we have lingering exported private keys, we want to warn
         // the user if that is the case
         _hasClipboardExportedPrivateKeys = hasPrivateKeyOnClipboard(mbwManager.getNetwork());
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

   private Runnable delayedFinish = new Runnable() {

      @Override
      public void run() {
         // Check whether we should handle this intent in a special way if it
         // has a bitcoin URI in it
         MbwManager mbwManager = MbwManager.getInstance(StartupActivity.this.getApplication());
         if (handleIntent(mbwManager)) {
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
      // Normal startup, show the selected record in the BalanceActivity
      // Intent intent = new Intent(StartupActivity.this,
      // BalanceActivity.class);
      Intent intent = new Intent(StartupActivity.this, ModernMain.class);
      startActivity(intent);
      finish();
   }

   private boolean handleIntent(MbwManager mbwManager) {
      Intent intent = getIntent();
      final String action = intent.getAction();
      final Uri intentUri = intent.getData();
      final String scheme = intentUri != null ? intentUri.getScheme() : null;

      if (Intent.ACTION_VIEW.equals(action) && intentUri != null && "bitcoin".equals(scheme)) {
         // We have been launched by a Bitcoin URI

         BitcoinUri b = BitcoinUri.parse(intentUri.toString(), mbwManager.getNetwork());
         if (b == null) {
            // Invalid Bitcoin URI
            Toast.makeText(this, R.string.invalid_bitcoin_uri, Toast.LENGTH_LONG).show();
            finish();
            return true;
         }

         Address receivingAddress = b.address;
         if (receivingAddress == null) {
            Toast.makeText(this, R.string.invalid_bitcoin_uri, Toast.LENGTH_LONG).show();
            finish();
            return true;
         }
         Long amountToSend = b.amount;

         RecordManager recordManager = mbwManager.getRecordManager();
         if (mbwManager.getWalletMode() == WalletMode.Segregated) {
            // If we are in segregated mode let the user choose which record to
            // use
            GetSpendingRecordActivity.callMe(this, amountToSend, receivingAddress);
         } else {
            Wallet wallet = recordManager.getWallet(mbwManager.getWalletMode());
            SendInitializationActivity.callMe(this, wallet, amountToSend, receivingAddress, false);
         }
         finish();
         return true;
      }

      // The intent was not a Bitcoin URI
      return false;
   }

}
