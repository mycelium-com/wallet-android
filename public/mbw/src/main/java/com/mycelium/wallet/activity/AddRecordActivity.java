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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.BackupState;
import com.mycelium.wallet.Record.Source;
import com.mycelium.wallet.Utils;

public class AddRecordActivity extends Activity {

   public static void callMe(Fragment fragment, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), AddRecordActivity.class);
      fragment.startActivityForResult(intent, requestCode);
   }

   public static final String RESULT_KEY = "record";
   private static final int SCAN_RESULT_CODE = 0;
   private static final int CREATE_RESULT_CODE = 1;

   private NetworkParameters _network;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_record_activity);
      final Activity activity = AddRecordActivity.this;
      _network = MbwManager.getInstance(this).getNetwork();

      findViewById(R.id.btScan).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            ScanActivity.callMe(activity, SCAN_RESULT_CODE);
         }

      });

      findViewById(R.id.btClipboard).setEnabled(Record.isRecord(Utils.getClipboardString(this), _network));
      findViewById(R.id.btClipboard).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            Record record = Record.fromString(Utils.getClipboardString(AddRecordActivity.this), _network);
            if (record == null) {
               Toast.makeText(activity, R.string.unrecognized_format, Toast.LENGTH_LONG).show();
               return;
            }
            // If the record has a private key delete the contents of the
            // clipboard
            if (record != null && record.hasPrivateKey()) {
               Utils.clearClipboardString(activity);
            }
            finishOk(record, BackupState.VERIFIED);
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

}
