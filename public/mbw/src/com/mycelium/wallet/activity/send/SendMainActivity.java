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

import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Utils.BitcoinScanResult;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;
import com.mycelium.wallet.api.AsyncTask;

public class SendMainActivity extends Activity {

   private static final int GET_ADDRESS_RESULT_CODE = 0;
   private static final int GET_AMOUNT_RESULT_CODE = 1;
   public static final int SCANNER_RESULT_CODE = 2;

   private enum TransactionStatus {
      MissingArguments, OutputTooSmall, InsufficientFunds, OK
   };

   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private Wallet _wallet;
   private SpendableOutputs _spendable;
   private Double _oneBtcInFiat; // May be null
   private Long _amountToSend;
   private Address _receivingAddress;
   private TransactionStatus _transactionStatus;
   private PrivateKeyRing _privateKeyRing;
   private UnsignedTransaction _unsigned;
   private AsyncTask _task;

   public static void callMe(Activity currentActivity, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat) {
      callMe(currentActivity, wallet, spendable, oneBtcInFiat, null, null);
   }

   public static void callMe(Activity currentActivity, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat,
         Long amountToSend, Address receivingAddress) {
      Intent intent = new Intent(currentActivity, SendMainActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("spendable", spendable);
      intent.putExtra("oneBtcInFiat", oneBtcInFiat);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      currentActivity.startActivity(intent);
   }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.send_main_activity);
      _mbwManager = MbwManager.getInstance(getApplication());
      _recordManager = _mbwManager.getRecordManager();

      // Get intent parameters
      _wallet = (Wallet) getIntent().getSerializableExtra("wallet");
      _spendable = (SpendableOutputs) getIntent().getSerializableExtra("spendable");
      // May be null
      _oneBtcInFiat = (Double) getIntent().getSerializableExtra("oneBtcInFiat");
      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra("amountToSend");
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra("receivingAddress");

      // Load saved state, overwriting amount and address
      if (savedInstanceState != null) {
         _amountToSend = (Long) savedInstanceState.getSerializable("amountToSend");
         _receivingAddress = (Address) savedInstanceState.getSerializable("receivingAddress");
      }

      // Construct private key ring
      _privateKeyRing = _wallet.getPrivateKeyRing();

      // See if we can create the transaction with what we have
      _transactionStatus = tryCreateUnsignedTransaction();

      // Scan
      findViewById(R.id.btScan).setOnClickListener(scanClickListener);

      // Address Menu
      findViewById(R.id.btAddressMenu).setOnClickListener(addressMenuClickListener);

      // Enter Amount
      findViewById(R.id.btEnterAmount).setOnClickListener(amountClickListener);

      // Send button
      findViewById(R.id.btSend).setOnClickListener(sendClickListener);

      // Amount Hint
      ((TextView) findViewById(R.id.tvAmount)).setHint(getResources().getString(R.string.amount_hint_denomination,
            _mbwManager.getBitcoinDenomination().toString()));

   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("amountToSend", _amountToSend);
      savedInstanceState.putSerializable("receivingAddress", _receivingAddress);
   }

   private OnClickListener scanClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         Utils.startScannerIntent(SendMainActivity.this, SCANNER_RESULT_CODE, _mbwManager.getContinuousFocus());
      }
   };

   private OnClickListener addressMenuClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         GetReceivingAddressActivity.callMe(SendMainActivity.this, GET_ADDRESS_RESULT_CODE);
      }
   };

   private OnClickListener amountClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         GetSendingAmountActivity.callMe(SendMainActivity.this, GET_AMOUNT_RESULT_CODE, _wallet, _spendable,
               _oneBtcInFiat, _amountToSend);
      }
   };

   private OnClickListener sendClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         _mbwManager.runPinProtectedFunction(SendMainActivity.this, pinProtectedSignAndSend);
      }
   };

   private TransactionStatus tryCreateUnsignedTransaction() {
      _unsigned = null;

      if (_amountToSend == null || _receivingAddress == null) {
         return TransactionStatus.MissingArguments;
      }

      // Construct list of outputs
      List<UnspentTransactionOutput> outputs = new LinkedList<UnspentTransactionOutput>();
      outputs.addAll(_spendable.unspent);
      outputs.addAll(_spendable.change);

      // Create unsigned transaction
      StandardTransactionBuilder stb = new StandardTransactionBuilder(Constants.network);

      // Add the output
      try {
         stb.addOutput(_receivingAddress, _amountToSend);
      } catch (OutputTooSmallException e1) {
         Toast.makeText(this, getResources().getString(R.string.amount_too_small), Toast.LENGTH_LONG).show();
         return TransactionStatus.OutputTooSmall;
      }

      // Create the unsigned transaction
      try {
         // note that change address is explicitly not set here - change will
         // flow back to the address supplying the highest input
         _unsigned = stb.createUnsignedTransaction(outputs, null, _privateKeyRing, Constants.network);
         return TransactionStatus.OK;
      } catch (InsufficientFundsException e) {
         Toast.makeText(this, getResources().getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
         return TransactionStatus.InsufficientFunds;
      }

   }

   private void updateUi() {

      // Update receiving address
      if (_receivingAddress == null) {
         // Clear Receiving Address
         ((TextView) findViewById(R.id.tvReceiver)).setText("");
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
         findViewById(R.id.tvReceiverLabel).setVisibility(View.GONE);
      } else {
         String address = _receivingAddress.toString();

         // Set Receiver Label
         String label = _mbwManager.getAddressBookManager().getNameByAddress(address);
         if (label == null || label.length() == 0) {
            // Hide label, show address book, and show button
            findViewById(R.id.tvReceiverLabel).setVisibility(View.GONE);
         } else {
            // Show label, hide address book
            findViewById(R.id.tvReceiverLabel).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvReceiverLabel)).setText(label);
         }

         // Set Address
         String choppedAddress = _receivingAddress.toMultiLineString();
         ((TextView) findViewById(R.id.tvReceiver)).setText(choppedAddress);
         ((TextView) findViewById(R.id.tvReceiver)).setTypeface(Typeface.MONOSPACE);

         // Show / hide warning
         Record record = _mbwManager.getRecordManager().getRecord(_receivingAddress);
         if (record != null && !record.hasPrivateKey()) {
            findViewById(R.id.tvWarning).setVisibility(View.VISIBLE);
         } else {
            findViewById(R.id.tvWarning).setVisibility(View.GONE);
         }
      }

      // Update Amount
      if (_amountToSend == null) {
         // Clear Amount
         ((TextView) findViewById(R.id.tvAmount)).setText("");
         findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
         ((TextView) findViewById(R.id.tvError)).setVisibility(View.GONE);
      } else {
         if (_transactionStatus == TransactionStatus.OutputTooSmall) {
            // Amount too small
            ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amountToSend));
            findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvError)).setText(R.string.amount_too_small_short);
            ((TextView) findViewById(R.id.tvError)).setVisibility(View.VISIBLE);
         } else if (_transactionStatus == TransactionStatus.InsufficientFunds) {
            // Insufficient funds
            ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amountToSend));
            findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvError)).setText(R.string.insufficient_funds);
            ((TextView) findViewById(R.id.tvError)).setVisibility(View.VISIBLE);
         } else {
            // Set Amount
            ((TextView) findViewById(R.id.tvAmount)).setText(_mbwManager.getBtcValueString(_amountToSend));
            if (_oneBtcInFiat == null) {
               findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
            } else {
               // Set approximate amount in fiat
               TextView tvAmountFiat = ((TextView) findViewById(R.id.tvAmountFiat));
               tvAmountFiat.setText(getFiatValue(_amountToSend, _oneBtcInFiat));
               tvAmountFiat.setVisibility(View.VISIBLE);
            }
            ((TextView) findViewById(R.id.tvError)).setVisibility(View.GONE);
         }
      }

      if (_unsigned == null) {
         // Hide fee show synchronizing
         findViewById(R.id.llFee).setVisibility(View.GONE);
      } else {
         // Show and set Fee, and hide synchronizing
         findViewById(R.id.llFee).setVisibility(View.VISIBLE);

         long fee = _unsigned.calculateFee();
         TextView tvFee = (TextView) findViewById(R.id.tvFee);
         tvFee.setText(_mbwManager.getBtcValueString(fee));
         if (_oneBtcInFiat == null) {
            findViewById(R.id.tvFeeFiat).setVisibility(View.INVISIBLE);
         } else {
            // Set approximate fee in fiat
            TextView tvFeeFiat = ((TextView) findViewById(R.id.tvFeeFiat));
            tvFeeFiat.setText(getFiatValue(fee, _oneBtcInFiat));
            tvFeeFiat.setVisibility(View.VISIBLE);
         }
      }

      // Enable/disable send button
      findViewById(R.id.btSend).setEnabled(_transactionStatus == TransactionStatus.OK);
   }

   private String getFiatValue(long satoshis, Double oneBtcInFiat) {
      String currency = _mbwManager.getFiatCurrency();
      Double converted = Utils.getFiatValue(satoshis, oneBtcInFiat);
      return getResources().getString(R.string.approximate_fiat_value, currency, converted);
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
         _task = null;
      }
   }

   final Runnable pinProtectedSignAndSend = new Runnable() {

      @Override
      public void run() {
         signAndSendTransaction();
      }
   };

   private void signAndSendTransaction() {
      findViewById(R.id.pbSend).setVisibility(View.VISIBLE);
      findViewById(R.id.btSend).setEnabled(false);
      findViewById(R.id.btAddressMenu).setEnabled(false);
      findViewById(R.id.btScan).setEnabled(false);
      findViewById(R.id.btEnterAmount).setEnabled(false);

      // Sign transaction in the background
      new android.os.AsyncTask<Handler, Integer, Void>() {

         @Override
         protected Void doInBackground(Handler... handler) {
            _unsigned.getSignatureInfo();
            List<byte[]> signatures = StandardTransactionBuilder.generateSignatures(_unsigned.getSignatureInfo(),
                  _privateKeyRing, _recordManager.getRandomSource());
            final Transaction tx = StandardTransactionBuilder.finalizeTransaction(_unsigned, signatures);
            // execute broadcasting task from UI thread
            handler[0].post(new Runnable() {

               @Override
               public void run() {
                  BroadcastTransactionActivity.callMe(SendMainActivity.this, tx);
                  SendMainActivity.this.finish();
               }
            });
            return null;
         }
      }.execute(new Handler[] { new Handler() });
   }

   private boolean checkForAutoSend() {
      if (_amountToSend == null || _oneBtcInFiat == null || _receivingAddress == null) {
         return false;
      }
      double oneSatoshiInFiat = _oneBtcInFiat / Math.pow(10, 8);
      long maxSatoshis = (long) (_mbwManager.getAutoPay() / 100 / oneSatoshiInFiat);
      if (_amountToSend < maxSatoshis) { // emulate send button. shall we even
                                         // skip pin protection? I think not.
         _mbwManager.runPinProtectedFunction(SendMainActivity.this, pinProtectedSignAndSend);
         return true;
      }
      return false;
   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == GET_AMOUNT_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser
         _amountToSend = Preconditions.checkNotNull((Long) intent.getSerializableExtra("amountToSend"));
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
         checkForAutoSend();
      } else if (requestCode == GET_ADDRESS_RESULT_CODE && resultCode == RESULT_OK) {
         _receivingAddress = Preconditions.checkNotNull((Address) intent.getSerializableExtra("receivingAddress"));
         Long amount = (Long) intent.getSerializableExtra("amountToSend");
         if (amount != null) {
            _amountToSend = amount;
         }
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
         checkForAutoSend();
      } else if (requestCode == SCANNER_RESULT_CODE && resultCode == RESULT_OK) {
         BitcoinScanResult scanResult = Utils.parseScanResult(intent);

         // Bail out if we don't like the result
         if (scanResult == null || scanResult.address == null) {
            _mbwManager.vibrate();
            Toast.makeText(this, R.string.invalid_bitcoin_uri, Toast.LENGTH_LONG).show();
            return;
         }

         _receivingAddress = scanResult.address;
         // Return result in an intent
         if (scanResult.amount != null) {
            _amountToSend = scanResult.amount;
         }
         _transactionStatus = tryCreateUnsignedTransaction();
         updateUi();
         checkForAutoSend();
      }

   }

}