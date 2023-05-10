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

package com.mycelium.wallet.activity.send;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.extsig.keepkey.activity.KeepKeySignTransactionActivity;
import com.mycelium.wallet.extsig.ledger.activity.LedgerSignTransactionActivity;
import com.mycelium.wallet.extsig.trezor.activity.TrezorSignTransactionActivity;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.Transaction;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature;

import java.util.UUID;

public class SignTransactionActivity extends AppCompatActivity {
   protected MbwManager _mbwManager;
   protected WalletAccount<?> _account;
   protected boolean _isColdStorage;
   protected Transaction _transaction;
   private AsyncTask<Void, Integer, Transaction> signingTask;
   private static final String TRANSACTION = "transaction";

   public static void callMe(Activity currentActivity, UUID account, boolean isColdStorage, Transaction transaction, int requestCode) {
      currentActivity.startActivityForResult(getIntent(currentActivity, account, isColdStorage, transaction), requestCode);
   }

   public static Intent getIntent(Activity currentActivity, UUID account, boolean isColdStorage, Transaction transaction) {
      WalletAccount walletAccount = MbwManager.getInstance(currentActivity).getWalletManager(isColdStorage).getAccount(account);

      Class targetClass;
      if (walletAccount instanceof HDAccountExternalSignature) {
         final int bip44AccountType = ((HDAccountExternalSignature) walletAccount).getBIP44AccountType();
         switch (bip44AccountType) {
            case (HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER):
               targetClass = LedgerSignTransactionActivity.class;
               break;
            case (HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY):
               targetClass = KeepKeySignTransactionActivity.class;
               break;
            case (HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR):
               targetClass = TrezorSignTransactionActivity.class;
               break;
            default:
               throw new RuntimeException("Unknown ExtSig Account type " + bip44AccountType);
         }
      } else {
         targetClass = SignTransactionActivity.class;
      }
      Preconditions.checkNotNull(account);

      return new Intent(currentActivity, targetClass)
              .putExtra(SendCoinsActivity.ACCOUNT, account)
              .putExtra(SendCoinsActivity.IS_COLD_STORAGE, isColdStorage)
              .putExtra(TRANSACTION, transaction);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setView();
      _mbwManager = MbwManager.getInstance(getApplication());
      // Get intent parameters
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra(SendCoinsActivity.ACCOUNT));
      _isColdStorage = getIntent().getBooleanExtra(SendCoinsActivity.IS_COLD_STORAGE, false);
      _account = Preconditions.checkNotNull(_mbwManager.getWalletManager(_isColdStorage).getAccount(accountId));
      _transaction = Preconditions.checkNotNull((Transaction) getIntent().getSerializableExtra(TRANSACTION));

      // Load state
      if (savedInstanceState != null) {
         // May be null
          _transaction = (Transaction) savedInstanceState.getSerializable(TRANSACTION);
      }
   }

   protected void setView() {
      setContentView(R.layout.sign_transaction_activity);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      if (_transaction != null) {
         outState.putSerializable(TRANSACTION, _transaction);
      }
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      if (signingTask == null) {
         signingTask = startSigningTask();
      }
      super.onResume();
   }

   @SuppressLint("StaticFieldLeak")
   protected AsyncTask<Void, Integer, Transaction> startSigningTask() {
      cancelSigningTask();
      // Sign transaction in the background
      signingTask = new AsyncTask<Void, Integer, Transaction>() {
         @Override
         protected Transaction doInBackground(Void... args) {
            try {
               _account.signTx(_transaction, AesKeyCipher.defaultKeyCipher());
               return _transaction.txBytes() != null ? _transaction : null;
            }
            catch (KeyCipher.InvalidKeyCipher e) {
               throw new RuntimeException("doInBackground" + e.getMessage());
            }
         }

         @Override
         protected void onPostExecute(Transaction signedTransaction) {
            if (signedTransaction != null) {
               Intent ret = new Intent();
               ret.putExtra(SendCoinsActivity.SIGNED_TRANSACTION, signedTransaction);
               setResult(RESULT_OK, ret);
            } else {
               setResult(RESULT_CANCELED);
            }
            SignTransactionActivity.this.finish();
         }
      };
      signingTask.execute();
      return signingTask;
   }

   protected void cancelSigningTask(){
      if (signingTask != null){
         signingTask.cancel(true);
      }
   }

   @Override
   protected void onDestroy() {
      if (signingTask != null){
         signingTask.cancel(true);
      }
      super.onDestroy();
   }
}
