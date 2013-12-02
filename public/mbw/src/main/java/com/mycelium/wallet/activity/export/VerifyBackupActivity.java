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

package com.mycelium.wallet.activity.export;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.ScanActivity;

public class VerifyBackupActivity extends Activity {

   public static final int SCAN_RESULT_CODE = 0;
   public static final int IMPORT_ENCRYPTED_PRIVATE_KEY_CODE = 1;

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, VerifyBackupActivity.class);
      currentActivity.startActivity(intent);
   }

   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private MrdExport.V1.EncryptionParameters _cachedParameters;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.verify_backup_activity);

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _recordManager = _mbwManager.getRecordManager();

      // We may have cached parameters, so we could avoid to input them again
      _cachedParameters = _mbwManager.getCachedEncryptionParameters();

      // We may have saved the parameters, and are recreated because the user
      // rotated the device or something
      if (savedInstanceState != null) {
         _cachedParameters = (MrdExport.V1.EncryptionParameters) savedInstanceState.getSerializable("cachedParameters");
      }

      findViewById(R.id.btScan).setOnClickListener(new android.view.View.OnClickListener() {

         @Override
         public void onClick(View v) {
            ScanActivity.callMe(VerifyBackupActivity.this, SCAN_RESULT_CODE);
         }

      });

      findViewById(R.id.btClipboard).setEnabled(isVerifiable());
      findViewById(R.id.btClipboard).setOnClickListener(new android.view.View.OnClickListener() {

         @Override
         public void onClick(View v) {
            String privateKey = Utils.getClipboardString(VerifyBackupActivity.this);
            verifyPrivateKeyBackup(privateKey);
         }

      });

   }

   private boolean isVerifiable() {
      String clipboardString = Utils.getClipboardString(this);
      return Record.isRecord(clipboardString, _mbwManager.getNetwork())
            || isEncryptedPrivateKey(clipboardString);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("cachedParameters", _cachedParameters);
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.setCachedEncryptionParameters(_cachedParameters);
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      _mbwManager.clearCachedEncryptionParameters();
      super.onDestroy();
   }

   private void updateUi() {
      TextView tvNumKeys = (TextView) findViewById(R.id.tvNumKeys);
      int num = countKeysToVerify();
      if (num == 0) {
         tvNumKeys.setVisibility(View.GONE);
      } else if (num == 1) {
         tvNumKeys.setVisibility(View.VISIBLE);
         tvNumKeys.setText(getResources().getString(R.string.verify_backup_one_key));
      } else {
         tvNumKeys.setVisibility(View.VISIBLE);
         tvNumKeys.setText(getResources().getString(R.string.verify_backup_num_keys, num));
      }
   }

   private int countKeysToVerify() {
      int num = 0;
      for (Record record : _recordManager.getAllRecords()) {
         if (record.needsBackupVerification()) {
            num++;
         }
      }
      return num;
   }

   private void verifyPrivateKeyBackup(String keyString) {
      Record record = Record.fromString(keyString, _mbwManager.getNetwork());
      if (record != null) {
         verify(record);
         return;
      }

      // Could be an encrypted private key
      if (isEncryptedPrivateKey(keyString)) {
         handleEncryptedPrivateKey(keyString);
      } else {
         Toast.makeText(VerifyBackupActivity.this, R.string.unrecognized_private_key_format, Toast.LENGTH_SHORT).show();
      }
   }

   private void verify(Record record) {
      if (!record.hasPrivateKey()) {
         Toast.makeText(VerifyBackupActivity.this, R.string.unrecognized_private_key_format, Toast.LENGTH_SHORT).show();
         return;
      }

      // Do verification
      boolean success = _mbwManager.getRecordManager().verifyPrivateKeyBackup(record.key);
      updateUi();
      if (success) {
         String message = getResources().getString(R.string.verify_backup_ok, record.address.toMultiLineString());
         ShowDialogMessage(message, false);
      } else {
         ShowDialogMessage(R.string.verify_backup_no_such_record, false);
      }
   }

   private void handleEncryptedPrivateKey(String encryptedPrivateKey) {

      // Check the version
      int version = 0;
      try {
         version = MrdExport.decodeVersion(encryptedPrivateKey);
      } catch (DecodingException e) {
         // Ignore, we fail gracefully in the version check below
      }
      if (version != MrdExport.V1_VERSION) {
         Toast.makeText(VerifyBackupActivity.this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
      }

      // Try and decrypt with cached parameters if we have them
      if (_cachedParameters != null) {
         try {
            String key = MrdExport.V1.decrypt(_cachedParameters, encryptedPrivateKey, _mbwManager.getNetwork());
            Record record = Record.fromString(key, _mbwManager.getNetwork());
            verify(record);
            return;
         } catch (InvalidChecksumException e) {
            // We cannot reuse the cached password, fall through and decrypt
            // with an entered password
         } catch (DecodingException e) {
            Toast.makeText(VerifyBackupActivity.this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
            return;
         }
      }

      // Start activity to ask the user to enter a password and decrypt the key
      DecryptPrivateKeyActivity.callMe(this, encryptedPrivateKey, IMPORT_ENCRYPTED_PRIVATE_KEY_CODE);
   }

   private boolean isEncryptedPrivateKey(String string) {
      int version;
      try {
         version = MrdExport.decodeVersion(string);
      } catch (DecodingException e) {
         return false;
      }
      return version == MrdExport.V1_VERSION;
   }

   private void ShowDialogMessage(int messageResource, final boolean quit) {
      ShowDialogMessage(getResources().getString(messageResource), quit);
   }

   private void ShowDialogMessage(String message, final boolean quit) {
      Utils.showSimpleMessageDialog(this, message);
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE) {
         if (resultCode == RESULT_OK) {
            Record record = (Record) intent.getSerializableExtra(ScanActivity.RESULT_RECORD_KEY);
            Preconditions.checkNotNull(record);
            verify(record);
         } else {
            String error = intent.getStringExtra(ScanActivity.RESULT_ERROR);
            if (error != null) {
               Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
         }
      }
   }

}