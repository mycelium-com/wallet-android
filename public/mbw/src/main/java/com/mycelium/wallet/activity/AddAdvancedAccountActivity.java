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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;

import java.util.UUID;

public class AddAdvancedAccountActivity extends Activity {

   public static void callMe(Activity activity, int requestCode) {
      Intent intent = new Intent(activity, AddAdvancedAccountActivity.class);
      activity.startActivityForResult(intent, requestCode);
   }

   public static final String RESULT_KEY = "account";
   private static final int SCAN_RESULT_CODE = 0;
   private static final int CREATE_RESULT_CODE = 1;
   private Toaster _toaster;
   private MbwManager _mbwManager;

   private NetworkParameters _network;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_advanced_account_activity);
      final Activity activity = AddAdvancedAccountActivity.this;
      _mbwManager = MbwManager.getInstance(this);
      _network = _mbwManager.getNetwork();
      _toaster = new Toaster(this);

      findViewById(R.id.btScan).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            ScanActivity.callMe(activity, SCAN_RESULT_CODE, ScanRequest.returnKeyOrAddress());
         }

      });
   }

   @Override
   public void onResume() {
      super.onResume();

      boolean canImportFromClipboard = Record.isRecord(Utils.getClipboardString(this), _network);
      Button clip = (Button) findViewById(R.id.btClipboard);
      clip.setEnabled(canImportFromClipboard);
      if (canImportFromClipboard) {
         clip.setText(R.string.clipboard);
      } else {
         clip.setText(R.string.clipboard_not_available);
      }
      clip.setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Optional<Record> record = Record.fromString(Utils.getClipboardString(AddAdvancedAccountActivity.this), _network);
            if (!record.isPresent()) {
               Toast.makeText(AddAdvancedAccountActivity.this, R.string.unrecognized_format, Toast.LENGTH_LONG).show();
               return;
            }
            // If the record has a private key delete the contents of the clipboard
            UUID account;
            if (record.get().hasPrivateKey()) {
               Utils.clearClipboardString(AddAdvancedAccountActivity.this);
               try {
                  account = _mbwManager.getWalletManager(false).createSingleAddressAccount(record.get().key, AesKeyCipher.defaultKeyCipher());
                  _mbwManager.getEventBus().post(new AccountChanged(account));
               } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                  throw new RuntimeException(invalidKeyCipher);
               }
            } else {
               account = _mbwManager.getWalletManager(false).createSingleAddressAccount(record.get().address);
               _mbwManager.getEventBus().post(new AccountChanged(account));
            }
            finishOk(account);
         }
      });

   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            ScanActivity.ResultType type = (ScanActivity.ResultType) intent.getSerializableExtra(ScanActivity.RESULT_TYPE_KEY);
            if (type == ScanActivity.ResultType.PRIVATE_KEY) {
               InMemoryPrivateKey key = ScanActivity.getPrivateKey(intent);
               returnAccount(key);
            } else if (type == ScanActivity.ResultType.ADDRESS) {
               Address address = ScanActivity.getAddress(intent);
               returnAccount(address);
            } else {
               throw new IllegalStateException("Unexpected result type from scan: " + type.toString());
            }
         } else {
            ScanActivity.toastScanError(resultCode, intent, this);
         }
      } else if (requestCode == CREATE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         String base58Key = intent.getStringExtra("base58key");
         Optional<InMemoryPrivateKey> key = InMemoryPrivateKey.fromBase58String(base58Key, _network);
         if (key.isPresent()) {
            returnAccount(key.get());
         } else {
            throw new RuntimeException("Creating private key from string unexpectedly failed.");
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void returnAccount(InMemoryPrivateKey key) {
      UUID acc;
      try {
         acc = _mbwManager.getWalletManager(false).createSingleAddressAccount(key, AesKeyCipher.defaultKeyCipher());
      } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
         throw new RuntimeException(invalidKeyCipher);
      }
      finishOk(acc);
   }

   private void returnAccount(Address address) {
      UUID acc = _mbwManager.getWalletManager(false).createSingleAddressAccount(address);
      finishOk(acc);
   }

   private void finishOk(UUID account) {
      Intent result = new Intent();
      result.putExtra(RESULT_KEY, account);
      setResult(RESULT_OK, result);
      finish();
   }

}
