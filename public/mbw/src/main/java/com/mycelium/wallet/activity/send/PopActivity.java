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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.Script;
import com.mrd.bitlib.model.ScriptOutput;
import com.mrd.bitlib.model.ScriptOutputStandard;
import com.mrd.bitlib.model.ScriptOutputStrange;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionInput;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.AddressBookFragment;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.List;

public class PopActivity extends Activity {
   private PopRequest popRequest;
   private MbwManager _mbwManager;
   private Sha256Hash txidToProve;
   protected static final int SIGN_TRANSACTION_REQUEST_CODE = 6;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.pop_activity);

      _mbwManager = MbwManager.getInstance(getApplication());


      PopRequest popRequest = (PopRequest) getIntent().getSerializableExtra("popRequest");
      if (popRequest == null) {
         finish();
      }
      this.popRequest = popRequest;

      // Get history ordered by block heigh descending
      List<TransactionSummary> transactionHistory = _mbwManager.getSelectedAccount().getTransactionHistory(0, 10000);
      TransactionSummary matchingTransaction = findFirstMatchingTransaction(popRequest, transactionHistory);
      if (matchingTransaction == null) {
         Toast.makeText(this, "No matching transaction for pop request: " + popRequest, Toast.LENGTH_LONG).show();
         finish();
         return;
      }
      txidToProve = matchingTransaction.txid;

      updateUi(matchingTransaction);
   }

   private TransactionSummary findFirstMatchingTransaction(PopRequest popRequest, List<TransactionSummary> transactions) {
      MetadataStorage metadataStorage = _mbwManager.getMetadataStorage();
      for (TransactionSummary transactionSummary : transactions) {
         if (popRequest.getTxid() != null && !transactionSummary.txid.equals(popRequest.getTxid())) {
            continue;
         }
         Long amountSatoshis = popRequest.getAmountSatoshis();
         if (amountSatoshis != null) {
            if (amountSatoshis != getPaymentAmountSatoshis(transactionSummary)) {
               continue;
            }
         }
         if (popRequest.getText() != null) {
            String label = metadataStorage.getLabelByTransaction(transactionSummary.txid);
            if (!popRequest.getText().equals(label)) {
               continue;
            }
         }
         return transactionSummary;
      }
      return null;
   }

   private void setText(int viewId, String value) {
      TextView textView = (TextView) findViewById(viewId);
      textView.setText(value);
   }



   private long getFee(TransactionDetails tx) {
      long inputs = sum(tx.inputs);
      long outputs = sum(tx.outputs);
      return inputs - outputs;
   }

   private long sum(TransactionDetails.Item[] items) {
      long sum = 0;
      for (TransactionDetails.Item item : items) {
         sum += item.value;
      }
      return sum;
   }

   private void updateUi(TransactionSummary transactionSummary) {
      MetadataStorage metadataStorage = _mbwManager.getMetadataStorage();
      String label = metadataStorage.getLabelByTransaction(transactionSummary.txid);

      setText(R.id.pop_recipient_host, popRequest.getUrl());
      setText(R.id.pop_transaction_id, transactionSummary.txid.toString());

      long amountSatoshis = getPaymentAmountSatoshis(transactionSummary);

      String value =_mbwManager.getBtcValueString(amountSatoshis);

      setText(R.id.pop_transaction_amount, value);
      setText(R.id.pop_transaction_label, label);
   }

   private long getPaymentAmountSatoshis(TransactionSummary transactionSummary) {
      long amountSatoshis = transactionSummary.value;
      if (amountSatoshis < 0) {
         amountSatoshis = -amountSatoshis;
      }
      TransactionDetails transactionDetails = _mbwManager.getSelectedAccount().getTransactionDetails(transactionSummary.txid);
      amountSatoshis -= getFee(transactionDetails);
      return amountSatoshis;
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("popRequest", popRequest);
   }


   public void sendPop(View view) {
      if (txidToProve == null) {
         Toast.makeText(this, R.string.pop_no_transaction_selected, Toast.LENGTH_LONG);
      }
      WalletAccount account = _mbwManager.getSelectedAccount();

      final UnsignedTransaction unsignedPop = account.createUnsignedPop(txidToProve, popRequest.getNonce());

      _mbwManager.runPinProtectedFunction(PopActivity.this, new Runnable() {

         @Override
         public void run() {
            disableButtons();
            SignTransactionActivity.callMe(PopActivity.this, _mbwManager.getSelectedAccount().getId(),
                    false, unsignedPop, SIGN_TRANSACTION_REQUEST_CODE);
         }
      });
   }

   protected void disableButtons() {
      findViewById(R.id.btSend).setEnabled(false);
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SIGN_TRANSACTION_REQUEST_CODE){
         if (resultCode == RESULT_OK) {
            Transaction pop = (Transaction) Preconditions.checkNotNull(intent.getSerializableExtra("signedTx"));
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
               new SendPopTask().execute(pop);
            } else {
               Toast.makeText(this, "No network available", Toast.LENGTH_LONG).show();
            }
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private class SendPopTask extends AsyncTask<Transaction, Void, String> {
      @Override
      protected String doInBackground(Transaction... pop) {

         // params comes from the execute() call: params[0] is the url.
         return sendPop(pop[0]);
      }
      // onPostExecute displays the results of the AsyncTask.
      @Override
      protected void onPostExecute(String result) {
         Toast.makeText(PopActivity.this, result, Toast.LENGTH_LONG).show();
         finish();
      }
   }

   private String sendPop(Transaction tx) {
      URL url;
      try {
         url = new URL(popRequest.getUrl());
      } catch (MalformedURLException e) {
         return "Invalid Url: " + popRequest.getUrl();
      }
      HttpURLConnection urlConnection;
      try {
         urlConnection = (HttpURLConnection) url.openConnection();
      } catch (IOException e) {
         return "Cannot connect to " + url + ": " + e.getMessage();
      }
      try {
         urlConnection.setDoOutput(true);
         byte[] bytes = tx.toBytes();
     //    urlConnection.setFixedLengthStreamingMode(bytes.length);
         OutputStream out = null;
         try {
            out = new BufferedOutputStream(urlConnection.getOutputStream());
         } catch (IOException e) {
            return "Cannot get OutputStream on " + url + ": " + e.getMessage();
         }
         try {
            out.write(bytes);
            out.close();
         } catch (IOException e) {
            return "Cannot write to " + url + ": " + e.getMessage();
         }
         try {
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
               return "Got response code: " + responseCode;
            }
         } catch (IOException e) {
            return "Cannot get response code: " + e.getMessage();
         }

         InputStream in = null;
         try {
            in = new BufferedInputStream(urlConnection.getInputStream());
         } catch (IOException e) {
            return "Cannot get InputStream on " + url + ": " + e.getMessage();
         }
         InputStreamReader inputStreamReader = null;
         try {
            inputStreamReader = new InputStreamReader(in, "US-ASCII");
         } catch (UnsupportedEncodingException e) {
            return "Unknown encoding 'US-ASCII':" + e.getMessage();
         }

         BufferedReader bufReader = new BufferedReader(inputStreamReader);
         StringBuffer reply = new StringBuffer();
         try {
            String line = bufReader.readLine();
            while (line != null) {
               if (reply.length() > 0) {
                  reply.append("\n");
               }
               reply.append(line);
               line = bufReader.readLine();
            }
         } catch (IOException e) {
            return "Could not read reply: " + e.getMessage();
         }
         if ("valid".equals(reply.toString())) {
            return "Success!";
         } else {
            return "Fail!:\n" + reply;
         }
      } finally {
         urlConnection.disconnect();
      }
   }

}
