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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.StringHandleConfig;

import java.util.UUID;

public class ImportSeedActivity extends AppCompatActivity {

   public static void callMe(Activity activity, int requestCode) {
      Intent intent = new Intent(activity, ImportSeedActivity.class);
      activity.startActivityForResult(intent, requestCode);
   }

   private static final int SCAN_SEED_CODE = 0;
   private static final int WORDLIST_CODE = 1;


   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_import_seed);
      final Activity activity = ImportSeedActivity.this;

      findViewById(R.id.btScan).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            ScanActivity.callMe(activity, SCAN_SEED_CODE, StringHandleConfig.importMasterSeed());
         }

      });

      findViewById(R.id.btWords).setOnClickListener(new View.OnClickListener() {

         @Override
         public void onClick(View v) {
            EnterWordListActivity.callMe(activity, WORDLIST_CODE);
         }
      });
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_SEED_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            UUID acc = StringHandlerActivity.getAccount(intent);
            finishOk(acc);
         } else {
            ScanActivity.toastScanError(resultCode, intent, this);
         }
      } else if (requestCode == WORDLIST_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            UUID account = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
            finishOk(account);
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void finishOk(UUID account) {
      Intent result = new Intent();
      result.putExtra(AddAccountActivity.RESULT_KEY, account);
      setResult(RESULT_OK, result);
      finish();
   }
}
