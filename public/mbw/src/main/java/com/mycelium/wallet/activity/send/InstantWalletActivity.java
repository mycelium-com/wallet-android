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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.activity.ScanActivity;

public class InstantWalletActivity extends Activity {

   public static final int SCAN_RESULT_CODE = 0;

   private Long _amountToSend;
   private Address _receivingAddress;

   public static void callMe(Activity currentActivity) {
      callMe(currentActivity, null, null);
   }

   public static void callMe(Activity currentActivity, Long amountToSend, Address receivingAddress) {
      Intent intent = new Intent(currentActivity, InstantWalletActivity.class);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      currentActivity.startActivity(intent);
   }

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.instant_wallet_activity);

      // Get intent parameters
      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra("amountToSend");
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra("receivingAddress");

      final Record record = getRecordFromClipboard();
      if (record == null) {
         findViewById(R.id.btClipboard).setEnabled(false);
      } else {
         findViewById(R.id.btClipboard).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
               Wallet wallet = new Wallet(record);
               SendInitializationActivity.callMe(InstantWalletActivity.this, wallet, _amountToSend, _receivingAddress,
                     true);
               InstantWalletActivity.this.finish();
            }
         });
      }

      findViewById(R.id.btScan).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            ScanActivity.callMe(InstantWalletActivity.this, SCAN_RESULT_CODE);
         }
      });

   }

   private Record getRecordFromClipboard() {
      String content = Utils.getClipboardString(InstantWalletActivity.this);
      if (content.length() == 0) {
         return null;
      }
      NetworkParameters network = MbwManager.getInstance(this).getNetwork();
      return Record.fromString(content.toString(), network);
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE) {
         if (resultCode == RESULT_OK) {
            Record record = Preconditions.checkNotNull((Record) intent
                  .getSerializableExtra(ScanActivity.RESULT_RECORD_KEY));
            Wallet wallet = new Wallet(record);
            SendInitializationActivity.callMe(this, wallet, _amountToSend, _receivingAddress, true);
            // We don't call finish() here, so that this activity stays on the back stack.
            // So the user can click back and scan the next cold storage.
         } else {
            ScanActivity.toastScanError(resultCode, intent, this);
         }
      } else {
         throw new IllegalStateException("unknown return codes after scanning... " + requestCode + " " + resultCode);
      }
   }

}