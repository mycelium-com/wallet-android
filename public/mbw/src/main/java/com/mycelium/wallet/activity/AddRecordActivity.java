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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.EncryptionUtils;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.BackupState;
import com.mycelium.wallet.Record.Source;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddRecordActivity extends Activity {

   public static void callMe(Fragment fragment, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), AddRecordActivity.class);
      fragment.startActivityForResult(intent, requestCode);
   }

   public static final String RESULT_KEY = "record";
   private static final int SCAN_RESULT_CODE = 0;
   private static final int CREATE_RESULT_CODE = 1;
   private Toaster _toaster;

   private NetworkParameters _network;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_record_activity);
      final Activity activity = AddRecordActivity.this;
      _network = MbwManager.getInstance(this).getNetwork();
      _toaster= new Toaster(this);

      findViewById(R.id.btScan).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            ScanActivity.callMe(activity, SCAN_RESULT_CODE);
         }

      });

      findViewById(R.id.btRandom).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Intent intent = new Intent(activity, CreateKeyActivity.class);
            startActivityForResult(intent, CREATE_RESULT_CODE);
         }
      });
   }

   @Override
   public void onResume() {
      super.onResume();

      findViewById(R.id.btClipboard).setEnabled(Record.isRecord(Utils.getClipboardString(this), _network));
      findViewById(R.id.btClipboard).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Record record = Record.fromString(Utils.getClipboardString(AddRecordActivity.this), _network);
            if (record == null) {
               Toast.makeText(AddRecordActivity.this, R.string.unrecognized_format, Toast.LENGTH_LONG).show();
               return;
            }
            // If the record has a private key delete the contents of the
            // clipboard
            if (record.hasPrivateKey()) {
               Utils.clearClipboardString(AddRecordActivity.this);
            }
            finishOk(record, BackupState.VERIFIED);
         }
      });

      if (Utils.findAndroidWalletBackupFiles(_network).size() > 0) {
         findViewById(R.id.btAndroidWalletBackup).setEnabled(true);
         findViewById(R.id.btAndroidWalletBackup).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               importAndroidWalletDialog(Utils.findAndroidWalletBackupFiles(_network));
            }

         });
      } else {
         findViewById(R.id.btAndroidWalletBackup).setEnabled(false);
      }

   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            Record record = (Record) intent.getSerializableExtra(ScanActivity.RESULT_RECORD_KEY);
            finishOk(record, BackupState.VERIFIED);
         } else {
            ScanActivity.toastScanError(resultCode, intent, this);
         }
      } else if (requestCode == CREATE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         String base58Key = intent.getStringExtra("base58key");
         Record record = Record.recordFromBase58Key(base58Key, _network);
         // Since the record is extracted from SIPA format the source defaults
         // to SIPA, set it to CREATED
         record.source = Source.CREATED_PRIVATE_KEY;
         finishOk(record, BackupState.UNKNOWN);
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void finishOk(Record record, BackupState backupState) {
      record.backupState = backupState;
      Intent result = new Intent();
      result.putExtra("record", record);
      setResult(RESULT_OK, result);
      finish();
   }

   /**
    * Show alert dialog with list of found "Bitcoin Wallet" backup files to import from.
    * Starts import for chosen file.
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
               String fileContent = Utils.getFileContent(backupFile);

               // Differentiate old/new backup type by filename:
               //   - old backup format (plain text) as used by Schildbach Wallet until version 3.46:
               //      bitcoin-wallet-keys-yyyy-mm-dd
               //   - new backup format (protocol buffers) as used by Schildbach Wallet from version 3.47+:
               //      bitcoin-wallet-backup-yyyy-mm-dd

               if (backupFile.getName().startsWith("bitcoin-wallet-backup")) {
                  // new protobuf based backup
                  byte[] decryptedBytes = EncryptionUtils.decryptOpenSslAes256CbcBytes(Utils.getFileContent(backupFile), password);
                  List<Record> privateKeys = Utils.getPrivKeysFromBitcoinJProtobufBackup(new ByteArrayInputStream(decryptedBytes), _network);
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
    * @param recordList list of found Records in backup file
    */
   private void chooseKeyForImportDialog(final List<Record> recordList) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      CharSequence[] keys = new CharSequence[recordList.size()];
      int pos = 0;
      for (Record rec : recordList) {
         keys[pos] = rec.address.toString();
         pos++;
      }
      builder.setTitle(R.string.pick_android_wallet_address_for_import).setItems(keys, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
            finishOk(recordList.get(which), BackupState.UNKNOWN);
         }
      });
      builder.create().show();
   }

   /**
    * Parse possible Base58 private keys from text (as used in "Bitcoin Wallet" backup files)
    * @param plainBackupText
    * @return list of found records
    */
   private List<Record> parseRecordsFromBackupText(String plainBackupText) {
      //todo try to implement this with Splitter.on(\"n") and String.indexOf(' ') to avoid regex
      //todo this could use a unit test with sample data
      StringTokenizer lines = new StringTokenizer(plainBackupText, "\n", false);
      List<Record> foundRecords = new ArrayList<Record>();
      // single line with key looks like:
      // KzCxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx 2014-02-11T08:55:35Z
      Pattern p = Pattern.compile("^([A-Za-z0-9]{30,60}) \\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ$");
      while (lines.hasMoreTokens()) {
         String currLine = lines.nextToken();
         Matcher m = p.matcher(currLine);
         if(m.matches()) {
            Record importRec = Record.recordFromBase58Key(m.group(1), _network);
            if (importRec!=null) {
               foundRecords.add(importRec);
            }
         }
      }
      return foundRecords;
   }

}
