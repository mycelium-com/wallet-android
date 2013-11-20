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
import android.os.Handler;
import android.view.Window;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import com.mrd.bitlib.model.Transaction;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.BroadcastTransactionResponse;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;

public class BroadcastTransactionActivity extends Activity {

   private MbwManager _mbwManager;
   private AsyncTask _task;

   public static void callMe(Activity currentActivity, Transaction transaction) {
      Intent intent = new Intent(currentActivity, BroadcastTransactionActivity.class);
      intent.putExtra("transaction", transaction);
      currentActivity.startActivity(intent);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.broadcast_transaction_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      Intent intent = Preconditions.checkNotNull(getIntent());
      Transaction transaction = Preconditions.checkNotNull((Transaction) intent.getSerializableExtra("transaction"),
            "unable to deserialize Transaction from Intent");

      // If the user rotates the screen or otherwise forces us to re-create the
      // activity we will broadcast it twice. This is not a problem as
      // broadcasting a transaction is idempotent.
      broadcastTransaction(transaction);
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
         _task = null;
      }
   }

   private void broadcastTransaction(Transaction transaction) {
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.broadcastTransaction(transaction, new BroadcastTransactionHandler());
   }

   class BroadcastTransactionHandler implements AbstractCallbackHandler<BroadcastTransactionResponse> {

      @Override
      public void handleCallback(BroadcastTransactionResponse response, ApiError exception) {
         _task = null;
         Activity me = BroadcastTransactionActivity.this;
         if (exception != null) {
            Toast.makeText(me, getResources().getString(R.string.transaction_not_sent), Toast.LENGTH_LONG).show();
         } else {
            // Include the transaction hash in the response
            Intent result = new Intent();
            result.putExtra("transaction_hash", response.hash.toString());
            setResult(RESULT_OK, result);
            Toast.makeText(me, getResources().getString(R.string.transaction_sent), Toast.LENGTH_LONG).show();
         }

         // Delay finish slightly
         new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
               BroadcastTransactionActivity.this.finish();
            }
         }, 200);
      }
   }

}