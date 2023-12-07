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

package com.mycelium.wallet.activity.pop;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.BitcoinTransaction;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.send.SignTransactionActivity;
import com.mycelium.wallet.activity.util.AdaptiveDateFormat;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.wallet.Transaction;
import com.mycelium.wapi.wallet.TransactionSummary;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.BtcTransaction;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class PopActivity extends AppCompatActivity {
   private PopRequest popRequest;
   private MbwManager _mbwManager;
   private Sha256Hash txidToProve;
   private static final int SIGN_TRANSACTION_REQUEST_CODE = 6;
   private static final String SIGNED_TRANSACTION = "signedTransaction";

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      getSupportActionBar().hide();
      setContentView(R.layout.pop_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      if (savedInstanceState != null) {
         popRequest = (PopRequest) savedInstanceState.getSerializable("popRequest");
         txidToProve = (Sha256Hash) savedInstanceState.getSerializable("txidToProve");
         updateUi((_mbwManager.getSelectedAccount().getTxSummary(txidToProve.getBytes())));
         return;
      }

      popRequest = (PopRequest) getIntent().getSerializableExtra("popRequest");
      if (popRequest == null) {
         finish();
      }

      Sha256Hash userSelectedTransaction = (Sha256Hash) getIntent().getSerializableExtra("selectedTransactionToProve");
      TransactionSummary txToProve;
      if (userSelectedTransaction != null) {
         txidToProve = userSelectedTransaction;
         txToProve = null;
      } else {
         // Get history ordered by block height descending
         List<TransactionSummary> transactionHistory = _mbwManager.getSelectedAccount().getTransactionSummaries(0, 10000);
         TransactionSummary matchingTransaction = findFirstMatchingTransaction(popRequest, transactionHistory);
         if (matchingTransaction == null) {
            launchSelectTransactionActivity();
            return;
         }
         txidToProve = Sha256Hash.of(matchingTransaction.getId());
         txToProve = matchingTransaction;
      }

      updateUi(txToProve);
   }

   private void launchSelectTransactionActivity() {
      Intent intent = new Intent(this, PopSelectTransactionActivity.class);
      intent.putExtra("popRequest", popRequest);
      startActivity(intent);
      finish();
   }

   private TransactionSummary findFirstMatchingTransaction(PopRequest popRequest, List<TransactionSummary> transactions) {
      MetadataStorage metadataStorage = _mbwManager.getMetadataStorage();
      for (TransactionSummary transaction: transactions) {
         if (PopUtils.matches(popRequest, metadataStorage, transaction)) {
            return transaction;
         }
      }
      return null;
   }

   private void setText(int viewId, String value) {
      TextView textView = findViewById(viewId);
      textView.setText(value);
   }

   private void updateUi(TransactionSummary transaction) {
      MetadataStorage metadataStorage = _mbwManager.getMetadataStorage();

      // Set Date
      Date date = new Date(transaction.getTimestamp() * 1000L);
      DateFormat dateFormat = new AdaptiveDateFormat(getApplicationContext());
      setText(R.id.pop_transaction_date, dateFormat.format(date));

      // Set amount
      long amountSatoshis = getPaymentAmountSatoshis(transaction);
      String value = _mbwManager.getBtcValueString(amountSatoshis);
      Value btcValue = Utils.getBtcCoinType().value(amountSatoshis);
      Value fiatValue = _mbwManager.getCurrencySwitcher().getAsFiatValue(btcValue);
      String fiatAppendment = "";
      if (fiatValue != null) {
         fiatAppendment = " (" + ValueExtensionsKt.toStringWithUnit(fiatValue) + ")";
      }
      setText(R.id.pop_transaction_amount, value + fiatAppendment);

      // Set label
      String label = metadataStorage.getLabelByTransaction(HexUtils.toHex(transaction.getId()));
      setText(R.id.pop_transaction_label, label);

      URL url = getUrl(popRequest.getP());
      if (url == null) {
         new Toaster(this).toast("Invalid URL:" + popRequest.getP(), false);
         finish();
         return;
      }

      TextView textView = findViewById(R.id.pop_recipient_host);
      textView.setText(url.getHost());
      String protocol = url.getProtocol();
      if ("https".equals(protocol)) {
         textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.holo_dark_ic_action_secure, 0, 0, 0);
      } else if ("http".equals(protocol)) {
         textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
      } else {
         new Toaster(this).toast("Unsupported protocol:" + url.getProtocol(), false);
         finish();
      }
   }

   private URL getUrl(String pParam) {
      URL url;
      try {
         url = new URL(pParam);
      } catch (MalformedURLException e) {
         new Toaster(this).toast("Not a proper destination URL:" + pParam, false);
         finish();
         return null;
      }
      return url;
   }

   private long getPaymentAmountSatoshis(TransactionSummary transaction) {
      if (transaction.getType() != BitcoinMain.get()) {
         return 0;
      }
      return transaction.getTransferred().abs().getValueAsLong();
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("popRequest", popRequest);
      savedInstanceState.putSerializable("txidToProve", txidToProve);
   }

   public void sendPop(View view) {
      try {
         if (txidToProve == null) {
            new Toaster(this).toast(R.string.pop_no_transaction_selected, false);
         }
         WalletAccount account = _mbwManager.getSelectedAccount();

         final UnsignedTransaction unsignedPop = ((WalletBtcAccount)account).createUnsignedPop(txidToProve, popRequest.getN());

         _mbwManager.runPinProtectedFunction(this, new Runnable() {
            @Override
            public void run() {
               disableButtons();
               CryptoCurrency cryptoCurrency = _mbwManager.getSelectedAccount().getCoinType();
               BtcTransaction unsignedTransaction = new BtcTransaction(cryptoCurrency, unsignedPop);
               Intent intent = SignTransactionActivity.getIntent(PopActivity.this, _mbwManager.getSelectedAccount().getId(), false, unsignedTransaction);
               startActivityForResult(intent, SIGN_TRANSACTION_REQUEST_CODE);
            }
         });
      } catch (Exception e) {
         new Toaster(this).toast("An internal error occurred:" + e.getMessage(), false);
      }
   }

   public void selectOtherTransaction(View view) {
      launchSelectTransactionActivity();
   }

   private void disableButtons() {
      findViewById(R.id.btSend).setEnabled(false);
      findViewById(R.id.btSelectOther).setEnabled(false);
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SIGN_TRANSACTION_REQUEST_CODE) {
         if (resultCode == RESULT_OK) {
            Transaction signedTransaction = (Transaction) Preconditions.checkNotNull(intent.getSerializableExtra(SIGNED_TRANSACTION));
            BtcTransaction btcTransaction = (BtcTransaction)signedTransaction;
            BitcoinTransaction pop = btcTransaction.getTx();
            ConnectivityManager connMgr = (ConnectivityManager)
                  getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
               new SendPopTask().execute(pop);
            } else {
               new Toaster(this).toast("No network available", false);
            }
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private class SendPopTask extends AsyncTask<BitcoinTransaction, Void, String> {
      @Override
      protected String doInBackground(BitcoinTransaction... pop) {
         // params comes from the execute() call: params[0] is the url.
         return sendPop(pop[0]);
      }

      private String sendPop(BitcoinTransaction tx) {
         RequestBody requestBody = RequestBody.create(MediaType.parse("application/bitcoin-pop"), tx.toBytes());

         URL url;
         try {
            url = new URL(popRequest.getP());
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
               return "Invalid Url, expected protocol http or https: " + popRequest.getP();
            }
         } catch (MalformedURLException e) {
            return "Invalid Url: " + popRequest.getP();
         }

         Request request = new Request.Builder().url(url).post(requestBody).build();

         OkHttpClient httpClient = new OkHttpClient();
         if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR && _mbwManager.getTorManager() != null) {
            httpClient = _mbwManager.getTorManager().setupClient(httpClient);
         }

         try {
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
               return response.body().string();
            } else {
               return "Error occurred: " + response.code();
            }
         } catch (IOException e) {
            return "Cannot communicate with server: " + e.getMessage();
         }

      }

      // onPostExecute displays the results of the AsyncTask.
      @Override
      protected void onPostExecute(String result) {
         if (result.equals("valid")) {
            new Toaster(PopActivity.this).toast(R.string.pop_success, false);
            finish();
         } else {
            String serverMessage = result;
            String prefix = "invalid\n";
            if (result.startsWith(prefix)) {
               serverMessage = result.substring(prefix.length());
            }
            String message = s(R.string.pop_invalid_pop) + " " + s(R.string.pop_message_from_server) + "\n" + serverMessage;
            Utils.showSimpleMessageDialog(PopActivity.this, message, new Runnable() {
               @Override
               public void run() {
                  launchSelectTransactionActivity();
               }
            }, R.string.pop_select_other_tx, null);
         }
      }
   }

   private String s(@StringRes int resId) {
      return getResources().getText(resId).toString();
   }


}
