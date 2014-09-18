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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.EncryptionUtils;
import com.mycelium.wallet.*;
import com.mycelium.wallet.Record.Source;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

      Button android = (Button) findViewById(R.id.btAndroidWalletBackup);
      if (Utils.findAndroidWalletBackupFiles(_network).size() > 0) {
         android.setEnabled(true);
         android.setText(R.string.android_wallet_backup);
         android.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               importAndroidWalletDialog(Utils.findAndroidWalletBackupFiles(_network));
            }

         });
      } else {
         android.setEnabled(false);
         android.setText(R.string.no_android_wallet_backup);
      }

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

   /**
    * Show alert dialog with list of found "Bitcoin Wallet" backup files to import from.
    * Starts import for chosen file.
    *
    * @param backupList list of possible Android Wallet backup files
    */
   private void importAndroidWalletDialog(final List<File> backupList) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      CharSequence[] names = new CharSequence[backupList.size()];
      int pos = 0;
      for (File f : backupList) {
         names[pos] = f.getName();
         pos++;
      }
      builder.setTitle(R.string.pick_android_wallet_backup);
      builder.setItems(names, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
            androidWalletPasswordDialog(backupList.get(which));
         }
      });
      builder.create().show();
   }

   /**
    * Shows alert dialog with EditText field for entering decryption password
    * of Android wallet backup.
    *
    * @param backupFile chosen backup file to decrypt
    */
   private void androidWalletPasswordDialog(final File backupFile) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.enter_android_wallet_backup_password_title);
      builder.setMessage(R.string.enter_android_wallet_backup_password_message);
      final EditText input = new EditText(this);
      builder.setView(input);
      builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int whichButton) {
            try {
               String password = input.getText().toString();

               // Differentiate old/new backup type by filename:
               //   - old backup format (plain text) as used by Schildbach Wallet until version 3.46:
               //      bitcoin-wallet-keys-yyyy-mm-dd
               //   - new backup format (protocol buffers) as used by Schildbach Wallet from version 3.47+:
               //      bitcoin-wallet-backup-yyyy-mm-dd

               if (backupFile.getName().startsWith("bitcoin-wallet-backup")) {
                  // new protobuf based backup
                  byte[] decryptedBytes = EncryptionUtils.decryptOpenSslAes256CbcBytes(Utils.getFileContent(backupFile), password);
                  List<InMemoryPrivateKey> privateKeys = Utils.getPrivKeysFromBitcoinJProtobufBackup(new ByteArrayInputStream(decryptedBytes), _network);
                  chooseKeyForImportDialog(privateKeys);
               } else {
                  // old plaintext backup
                  String decryptedText = EncryptionUtils.decryptOpenSslAes256Cbc(Utils.getFileContent(backupFile), password);
                  chooseKeyForImportDialog(parseRecordsFromBackupText(decryptedText));
               }
            } catch (GeneralSecurityException se) {
               _toaster.toast(R.string.import_android_wallet_backup_decryption_error, false);
            } catch (IOException io) {
               _toaster.toast(R.string.import_android_wallet_backup_io_error, false);
            }
         }
      });
      builder.show();
   }

   /**
    * Shows a list with found Bitcoin addresses in the "Bitcoin Wallet" backup file,
    * returns the one the user chooses to the calling activity
    *
    * @param keyList list of found Records in backup file
    */
   private void chooseKeyForImportDialog(final List<InMemoryPrivateKey> keyList) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      CharSequence[] keys = new CharSequence[keyList.size()];
      int pos = 0;
      for (InMemoryPrivateKey key : keyList) {
         keys[pos] = key.getPublicKey().toAddress(_network).toString();
         pos++;
      }
      builder.setTitle(R.string.pick_android_wallet_address_for_import).setItems(keys, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
            returnAccount(keyList.get(which));
         }
      });
      builder.create().show();
   }

   /**
    * Parse possible Base58 private keys from text (as used in "Bitcoin Wallet" backup files)
    *
    * @param plainBackupText
    * @return list of found records
    */
   private List<InMemoryPrivateKey> parseRecordsFromBackupText(String plainBackupText) {
      //todo try to implement this with Splitter.on(\"n") and String.indexOf(' ') to avoid regex
      //todo this could use a unit test with sample data
      StringTokenizer lines = new StringTokenizer(plainBackupText, "\n", false);
      List<InMemoryPrivateKey> foundKeys = new ArrayList<InMemoryPrivateKey>();
      // single line with key looks like:
      // KzCxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx 2014-02-11T08:55:35Z
      Pattern p = Pattern.compile("^([A-Za-z0-9]{30,60}) \\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ$");
      while (lines.hasMoreTokens()) {
         String currLine = lines.nextToken();
         Matcher m = p.matcher(currLine);
         if (m.matches()) {
            Optional<InMemoryPrivateKey> importKey = InMemoryPrivateKey.fromBase58String(m.group(1), _network);
            if (importKey.isPresent()) {
               foundKeys.add(importKey.get());
            }
         }
      }
      return foundKeys;
   }

}
