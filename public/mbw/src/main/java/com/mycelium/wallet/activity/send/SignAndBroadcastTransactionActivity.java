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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wallet.*;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Subscribe;

import java.util.UUID;

public class SignAndBroadcastTransactionActivity extends Activity {
   private MbwManager _mbwManager;
   private WalletAccount _account;
   private boolean _isColdStorage;
   private StandardTransactionBuilder.UnsignedTransaction _unsigned;
   private Transaction _transaction;
   private AsyncTask<Void, Integer, Transaction> _signingTask;
   private AsyncTask<Void, Integer, WalletAccount.BroadcastResult> _broadcastingTask;
   private WalletAccount.BroadcastResult _broadcastResult;

   public static void callMe(Activity currentActivity, UUID account, boolean isColdStorage, StandardTransactionBuilder.UnsignedTransaction unsigned) {
      Intent intent = new Intent(currentActivity, SignAndBroadcastTransactionActivity.class);
      intent.putExtra("account", account);
      intent.putExtra("isColdStorage", isColdStorage);
      intent.putExtra("unsigned", unsigned);
      intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.sign_and_broadcast_transaction_activity);
      _mbwManager = MbwManager.getInstance(getApplication());
      // Get intent parameters
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra("account"));
      _isColdStorage = getIntent().getBooleanExtra("isColdStorage", false);
      _account = Preconditions.checkNotNull(_mbwManager.getWalletManager(_isColdStorage).getAccount(accountId));
      _unsigned = Preconditions.checkNotNull((StandardTransactionBuilder.UnsignedTransaction) getIntent().getSerializableExtra("unsigned"));

      // Load state
      if (savedInstanceState != null) {
         // May be null
         _transaction = (Transaction) savedInstanceState.getSerializable("transaction");
      }

   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      if (_transaction != null) {
         outState.putSerializable("transaction", _transaction);
      }
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);

      ensureProgress();
      super.onResume();
   }

   private void ensureProgress() {
      if (_transaction == null) {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.signing_transaction);
         if (_signingTask == null) {
            _signingTask = startSigningTask();
         }
      } else if (_broadcastResult == null) {
         ((TextView) findViewById(R.id.tvTitle)).setText(R.string.broadcasting_transaction);
         if (_broadcastingTask == null) {
            _broadcastingTask = startBroadcastingTask();
         }
      } else {
         showResult();
      }

   }

   private AsyncTask<Void, Integer, Transaction> startSigningTask() {
      // Sign transaction in the background
      AsyncTask<Void, Integer, Transaction> task = new AsyncTask<Void, Integer, Transaction>() {

         @Override
         protected Transaction doInBackground(Void... args) {
            try {
               return _account.signTransaction(_unsigned, AesKeyCipher.defaultKeyCipher(), new AndroidRandomSource());
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
               throw new RuntimeException(invalidKeyCipher);
            }
         }

         @Override
         protected void onPostExecute(Transaction transaction) {
            _transaction = transaction;
            ensureProgress();
         }
      };

      task.execute();
      return task;
   }

   private AsyncTask<Void, Integer, WalletAccount.BroadcastResult> startBroadcastingTask() {
      // Broadcast the transaction in the background
      AsyncTask<Void, Integer, WalletAccount.BroadcastResult> task = new AsyncTask<Void, Integer, WalletAccount.BroadcastResult>() {

         @Override
         protected WalletAccount.BroadcastResult doInBackground(Void... args) {
            return _account.broadcastTransaction(_transaction);
         }

         @Override
         protected void onPostExecute(WalletAccount.BroadcastResult result) {
            _broadcastResult = result;
            ensureProgress();
         }
      };

      task.execute();
      return task;
   }

   private void showResult() {
      if (_broadcastResult == WalletAccount.BroadcastResult.REJECTED) {
         // Transaction rejected, display message and exit
         Utils.showSimpleMessageDialog(this, R.string.transaction_rejected_message, new Runnable() {
            @Override
            public void run() {
               SignAndBroadcastTransactionActivity.this.finish();
            }
         });
      } else if (_broadcastResult == WalletAccount.BroadcastResult.NO_SERVER_CONNECTION) {
         if (_isColdStorage) {
            // When doing cold storage spending we do not offer to queue the transaction
            Utils.showSimpleMessageDialog(this, R.string.transaction_not_sent, new Runnable() {
               @Override
               public void run() {
                  SignAndBroadcastTransactionActivity.this.finish();
               }
            });
         } else {
            // Offer the user to queue the transaction
            AlertDialog.Builder queueDialog = new AlertDialog.Builder(this);
            queueDialog.setTitle(R.string.no_server_connection);
            queueDialog.setMessage(R.string.queue_transaction_message);

            queueDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

               public void onClick(DialogInterface arg0, int arg1) {
                  _account.queueTransaction(_transaction);
                  SignAndBroadcastTransactionActivity.this.finish();
               }
            });
            queueDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

               public void onClick(DialogInterface arg0, int arg1) {
                  SignAndBroadcastTransactionActivity.this.finish();
               }
            });
            queueDialog.show();

         }
      } else if (_broadcastResult == WalletAccount.BroadcastResult.SUCCESS) {
         // Toast success and finish
         Toast.makeText(this, getResources().getString(R.string.transaction_sent),
               Toast.LENGTH_LONG).show();

         // Include the transaction hash in the response
         Intent result = new Intent();
         result.putExtra(Constants.TRANSACTION_HASH_INTENT_KEY, _transaction.getHash().toString());
         setResult(RESULT_OK, result);
         Toast.makeText(this, getResources().getString(R.string.transaction_sent),
               Toast.LENGTH_LONG).show();

         finish();
      } else {
         throw new RuntimeException();
      }
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      _mbwManager.forgetColdStorageWalletManager();
      super.onDestroy();
   }

   @Subscribe
   public void syncFailed(SyncFailed event) {
      Utils.toastConnectionError(this);
   }

   @Subscribe
   public void syncStopped(SyncStopped sync) {
   }

}
