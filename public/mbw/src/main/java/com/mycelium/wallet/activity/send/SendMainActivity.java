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
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.ScanRequest;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.modern.AddressBookFragment;
import com.mycelium.wallet.activity.modern.GetFromAddressBookActivity;
import com.mycelium.wallet.api.AsyncTask;

public class SendMainActivity extends Activity {

   private static final int GET_AMOUNT_RESULT_CODE = 1;
   private static final int SCAN_RESULT_CODE = 2;
   private static final int ADDRESS_BOOK_RESULT_CODE = 3;
   private static final int MANUAL_ENTRY_RESULT_CODE = 4;

   private enum TransactionStatus {
      MissingArguments, OutputTooSmall, InsufficientFunds, OK
   }

   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private Wallet _wallet;
   private SpendableOutputs _spendable;
   private Double _oneBtcInFiat; // May be null
   //todo Andreas refactor this to hold bitocoin Uri
   private Long _amountToSend;
   private Address _receivingAddress;
   private boolean _isColdStorage;
   private TransactionStatus _transactionStatus;
   private PrivateKeyRing _privateKeyRing;
   private UnsignedTransaction _unsigned;
   private AsyncTask _task;

   public static void callMe(Activity currentActivity, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat,
         boolean isColdStorage) {
      callMe(currentActivity, wallet, spendable, oneBtcInFiat, null, null, isColdStorage);
   }

   public static void callMe(Activity currentActivity, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat,
         Long amountToSend, Address receivingAddress, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendMainActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("spendable", spendable);
      intent.putExtra("oneBtcInFiat", oneBtcInFiat);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      intent.putExtra("isColdStorage", isColdStorage);
      intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   public static void callMe(Activity currentActivity, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat, BitcoinUri uri, boolean isColdStorage) {
      callMe(currentActivity, wallet, spendable, oneBtcInFiat, uri.amount, uri.address, isColdStorage);
   }

   public static void callMe(Fragment currentFragment, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat, Long amountToSend, Address receivingAddress, boolean isColdStorage) {
      Intent intent = new Intent(currentFragment.getActivity(), SendMainActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("spendable", spendable);
      intent.putExtra("oneBtcInFiat", oneBtcInFiat);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      intent.putExtra("isColdStorage", isColdStorage);
      currentFragment.startActivity(intent);
   }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.send_main_activity);
      _mbwManager = MbwManager.getInstance(getApplication());
      _recordManager = _mbwManager.getRecordManager();

      // Get intent parameters
      _wallet = Preconditions.checkNotNull((Wallet) getIntent().getSerializableExtra("wallet"));
      _spendable = Preconditions.checkNotNull((SpendableOutputs) getIntent().getSerializableExtra("spendable"));
      // May be null
      _oneBtcInFiat = (Double) getIntent().getSerializableExtra("oneBtcInFiat");
      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra("amountToSend");
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra("receivingAddress");
      _isColdStorage = getIntent().getBooleanExtra("isColdStorage", false);

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

      // Address Book
      findViewById(R.id.btAddressBook).setOnClickListener(addressBookClickListener);

      // Manual Entry
      findViewById(R.id.btManualEntry).setOnClickListener(manualEntryClickListener);

      // Clipboard
      findViewById(R.id.btClipboard).setOnClickListener(clipboardClickListener);

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
         ScanActivity.callMe(SendMainActivity.this, SCAN_RESULT_CODE, ScanRequest.returnKeyOrAddress());
      }
   };

   private OnClickListener addressBookClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         Intent intent = new Intent(SendMainActivity.this, GetFromAddressBookActivity.class);
         startActivityForResult(intent, ADDRESS_BOOK_RESULT_CODE);
      }
   };

   private OnClickListener manualEntryClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         Intent intent = new Intent(SendMainActivity.this, ManualAddressEntry.class);
         startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE);
      }
   };

   private OnClickListener clipboardClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         BitcoinUri uri = getUriFromClipboard();
         if (uri != null) {
            Toast.makeText(SendMainActivity.this, getResources().getString(R.string.using_address_from_clipboard),
                  Toast.LENGTH_SHORT).show();
            _receivingAddress = uri.address;
            if (uri.amount != null) {
               _amountToSend = uri.amount;
            }
            _transactionStatus = tryCreateUnsignedTransaction();
            updateUi();
         }
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
         if (_isColdStorage) {
            // We do not ask for pin when the key is from cold storage
            signAndSendTransaction();
         } else {
            _mbwManager.runPinProtectedFunction(SendMainActivity.this, pinProtectedSignAndSend);
         }
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
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_mbwManager.getNetwork());

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
         _unsigned = stb.createUnsignedTransaction(outputs, null, _privateKeyRing, _mbwManager.getNetwork());
         return TransactionStatus.OK;
      } catch (InsufficientFundsException e) {
         Toast.makeText(this, getResources().getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
         return TransactionStatus.InsufficientFunds;
      }

   }

   private void updateUi() {
      updateRecipient();
      updateAmount();

      // Enable/disable send button
      findViewById(R.id.btSend).setEnabled(_transactionStatus == TransactionStatus.OK);
      findViewById(R.id.root).invalidate();
   }

   private void updateRecipient() {
      if (_receivingAddress == null) {
         // Hide address, show "Enter"
         ((TextView) findViewById(R.id.tvRecipientTitle)).setText(R.string.enter_recipient_title);
         findViewById(R.id.llEnterRecipient).setVisibility(View.VISIBLE);
         findViewById(R.id.llRecipientAddress).setVisibility(View.GONE);
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
         return;
      }
      // Hide "Enter", show address
      ((TextView) findViewById(R.id.tvRecipientTitle)).setText(R.string.recipient_title);
      findViewById(R.id.llRecipientAddress).setVisibility(View.VISIBLE);
      findViewById(R.id.llEnterRecipient).setVisibility(View.GONE);

      // Set label if applicable
      TextView receiverLabel = (TextView) findViewById(R.id.tvReceiverLabel);

      // See if the address is in the address book
      String label = _mbwManager.getAddressBookManager().getNameByAddress(_receivingAddress.toString());

      if (label == null || label.length() == 0) {
         // Hide label
         receiverLabel.setVisibility(View.GONE);
      } else {
         // Show label
         receiverLabel.setText(label);
         receiverLabel.setVisibility(View.VISIBLE);
      }

      // Set Address
      String choppedAddress = _receivingAddress.toMultiLineString();
      ((TextView) findViewById(R.id.tvReceiver)).setText(choppedAddress);

      // Show / hide warning
      Record record = _mbwManager.getRecordManager().getRecord(_receivingAddress);
      if (record != null) {
         TextView tvWarning = (TextView) findViewById(R.id.tvWarning);
         if (record.hasPrivateKey()) {
            // Show a warning as we are sending to one of our own addresses
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText(R.string.my_own_address_warning);
            tvWarning.setTextColor(getResources().getColor(R.color.yellow));
         } else {
            // Show a warning as we are sending to one of our own addresses,
            // which is read-only
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText(R.string.read_only_warning);
            tvWarning.setTextColor(getResources().getColor(R.color.red));
         }

      } else {
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
      }
   }

   private void updateAmount() {
      // Update Amount
      if (_amountToSend == null) {
         // No amount to show
         ((TextView) findViewById(R.id.tvAmountTitle)).setText(R.string.enter_amount_title);
         ((TextView) findViewById(R.id.tvAmount)).setText("");
         findViewById(R.id.tvAmountFiat).setVisibility(View.GONE);
         ((TextView) findViewById(R.id.tvError)).setVisibility(View.GONE);
      } else {
         ((TextView) findViewById(R.id.tvAmountTitle)).setText(R.string.amount_title);
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

      // Update Fee
      if (_unsigned == null) {
         // Hide fee
         findViewById(R.id.llFee).setVisibility(View.GONE);
      } else {
         // Show fee
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

   }

   private String getFiatValue(long satoshis, Double oneBtcInFiat) {
      String currency = _mbwManager.getFiatCurrency();
      String converted = Utils.getFiatValueAsString(satoshis, _oneBtcInFiat);
      return getResources().getString(R.string.approximate_fiat_value, currency, converted);
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      findViewById(R.id.btClipboard).setEnabled(getUriFromClipboard() != null);
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
      findViewById(R.id.btAddressBook).setEnabled(false);
      findViewById(R.id.btManualEntry).setEnabled(false);
      findViewById(R.id.btClipboard).setEnabled(false);
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

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE) {
         if (resultCode != RESULT_OK) {
            if (intent != null) {
               String error = intent.getStringExtra(ScanActivity.RESULT_ERROR);
               if (error != null) {
                  Toast.makeText(this, error, Toast.LENGTH_LONG).show();
               }
            }
         } else {
         Record record = ScanActivity.getRecord(intent);
         _receivingAddress = record.address;
      }
      } else if (requestCode == ADDRESS_BOOK_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser
         String s = Preconditions.checkNotNull(intent.getStringExtra(AddressBookFragment.ADDRESS_RESULT_NAME));
         String result = s.trim();
         // Is it really an address?
         Address address = Address.fromString(result, _mbwManager.getNetwork());
         if (address == null) {
            return;
         }
         _receivingAddress = address;
      } else if (requestCode == MANUAL_ENTRY_RESULT_CODE && resultCode == RESULT_OK) {
         Address address = Preconditions.checkNotNull((Address) intent
               .getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME));
         _receivingAddress = address;
      } else if (requestCode == GET_AMOUNT_RESULT_CODE && resultCode == RESULT_OK) {
         // Get result from address chooser
         _amountToSend = Preconditions.checkNotNull((Long) intent.getSerializableExtra("amountToSend"));
      } else {
         // We didn't like what we got, bail
      }
      _transactionStatus = tryCreateUnsignedTransaction();
      updateUi();
   }

   private BitcoinUri getUriFromClipboard() {
      String content = Utils.getClipboardString(SendMainActivity.this);
      if (content.length() == 0) {
         return null;
      }
      String string = content.toString().trim();
      if (string.matches("[a-zA-Z0-9]*")) {
         // Raw format
         Address address = Address.fromString(string, _mbwManager.getNetwork());
         if (address == null){
            return null;
         }
         return new BitcoinUri(address,null,null);
      } else {
         Optional<BitcoinUri> b = BitcoinUri.parse(string, _mbwManager.getNetwork());
         if (b.isPresent()) {
            // On URI format
            return b.get();
         }
      }
      return null;
   }

}