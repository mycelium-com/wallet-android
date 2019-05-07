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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.AddressLabel;
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay;
import com.mycelium.wallet.activity.util.TransactionDetailsLabel;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.json.ColuTxDetailsItem;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.WalletAccount;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Async task to perform fetching parent transactions of current transaction from server
 */
public class TransactionDetailsActivity extends Activity {

   @SuppressWarnings("deprecation")
   private static final LayoutParams FPWC = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
   private static final LayoutParams WCWC = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
   private TransactionDetails _tx;
   private TransactionSummary _txs;
   private int _white_color;
   private MbwManager _mbwManager;
   private boolean coluMode = false;

   /**
    * Called when the activity is first created.
    */
   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);

      _white_color = getResources().getColor(R.color.white);
      setContentView(R.layout.transaction_details_activity);
      _mbwManager = MbwManager.getInstance(this.getApplication());

      loadAndUpdate(false);

      startRemoteLoading(null);
   }

   public void startRemoteLoading(View view) {
      new UpdateParentTask().execute();
   }

   private void loadAndUpdate(boolean isAfterRemoteUpdate) {
      Sha256Hash txid = getTransactionFromIntent();
      _tx = _mbwManager.getSelectedAccount().getTransactionDetails(txid);
      _txs = _mbwManager.getSelectedAccount().getTransactionSummary(txid);

      coluMode = _mbwManager.getSelectedAccount() instanceof ColuAccount;
      updateUi(isAfterRemoteUpdate, false);
   }

   private void updateUi(boolean isAfterRemoteUpdate, boolean suggestRetryIfError) {
      // Set Hash
      TransactionDetailsLabel tvHash = findViewById(R.id.tvHash);
      tvHash.setColuMode(coluMode);
      tvHash.setTransaction(_tx);

      // Set Confirmed
      int confirmations = _tx.calculateConfirmations(_mbwManager.getSelectedAccount().getBlockChainHeight());

      String confirmed;
      if (_tx.height > 0) {
         confirmed = getResources().getString(R.string.confirmed_in_block, _tx.height);
      } else {
         confirmed = getResources().getString(R.string.no);
      }

      // check if tx is in outgoing queue
      TransactionConfirmationsDisplay confirmationsDisplay = findViewById(R.id.tcdConfirmations);
      TextView confirmationsCount = findViewById(R.id.tvConfirmations);

      if (_txs!=null && _txs.isQueuedOutgoing){
         confirmationsDisplay.setNeedsBroadcast();
         confirmationsCount.setText("");
         confirmed = getResources().getString(R.string.transaction_not_broadcasted_info);
      }else {
         confirmationsDisplay.setConfirmations(confirmations);
         confirmationsCount.setText(String.valueOf(confirmations));
      }

      ((TextView) findViewById(R.id.tvConfirmed)).setText(confirmed);

      // Set Date & Time
      Date date = new Date(_tx.time * 1000L);
      Locale locale = getResources().getConfiguration().locale;
      DateFormat dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale);
      String dateString = dayFormat.format(date);
      ((TextView) findViewById(R.id.tvDate)).setText(dateString);
      DateFormat hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale);
      String timeString = hourFormat.format(date);
      ((TextView) findViewById(R.id.tvTime)).setText(timeString);

       TextView tvInputsAmount = findViewById(R.id.tvInputsAmount);
       Button btInputsRetry = findViewById(R.id.btInputsRetry);
       Button btFeeRetry = findViewById(R.id.btFeeRetry);
       TextView tvFeeAmount = findViewById(R.id.tvFee);
       btFeeRetry.setVisibility(View.GONE);
       btInputsRetry.setVisibility(View.GONE);
       tvFeeAmount.setVisibility(View.VISIBLE);
       tvInputsAmount.setVisibility(View.VISIBLE);
      // Set Inputs
      final LinearLayout llInputs = findViewById(R.id.llInputs);
      llInputs.removeAllViews();
      if (_tx.inputs != null) {
         int sum = 0;
         for (TransactionDetails.Item input : _tx.inputs) {
            sum += input.value;
         }
         if (sum != 0) {
             tvInputsAmount.setVisibility(View.GONE);
            for (TransactionDetails.Item item : _tx.inputs) {
               llInputs.addView(getItemView(item));
            }
         }
      }

      // Set Outputs
      LinearLayout outputs = findViewById(R.id.llOutputs);
      outputs.removeAllViews();
      if(_tx.outputs != null) {
         for (TransactionDetails.Item item : _tx.outputs) {
            outputs.addView(getItemView(item));
         }
      }

      // Set Fee
      final long txFeeTotal = getFee(_tx);
      String fee;

      if(txFeeTotal > 0) {
         if (_mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHSINGLEADDRESS
             || _mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHBIP44) {
            fee = _mbwManager.getBchValueString(txFeeTotal);
         } else {
            fee = _mbwManager.getBtcValueString(txFeeTotal);
         }
         if (_tx.rawSize > 0) {
            final long txFeePerSat = txFeeTotal / _tx.rawSize;
            fee += String.format("\n%d sat/byte", txFeePerSat);
         }
         tvFeeAmount.setText(fee);
         tvFeeAmount.setVisibility(View.VISIBLE);
      } else {
         tvFeeAmount.setText(isAfterRemoteUpdate ? R.string.no_transaction_details : R.string.no_transaction_loading);
         if (isAfterRemoteUpdate) {
            if (suggestRetryIfError) {
               btFeeRetry.setVisibility(View.VISIBLE);
               btInputsRetry.setVisibility(View.VISIBLE);
               tvFeeAmount.setVisibility(View.GONE);
               tvInputsAmount.setVisibility(View.GONE);
            }
         } else {
            int length = _tx.inputs.length;
            String amountLoading;
            if (length > 0) {
               amountLoading = String.format("%s %s", String.valueOf(length), getString(R.string.no_transaction_loading));
            } else {
               amountLoading = getString(R.string.no_transaction_loading);
            }
            if (tvInputsAmount.isAttachedToWindow()) {
               tvInputsAmount.setText(amountLoading);
            }
         }
      }
   }

   private long getFee(TransactionDetails tx) {
      long inputs = sum(tx.inputs);
      long outputs = sum(tx.outputs);
      return inputs - outputs;
   }

   private long sum(TransactionDetails.Item[] items) {
      long sum = 0;
      if(items != null) {
         for (TransactionDetails.Item item : items) {
            sum += item.value;
         }
      }
      return sum;
   }

   private View getItemView(TransactionDetails.Item item) {
      // Create vertical linear layout
      LinearLayout ll = new LinearLayout(this);
      ll.setOrientation(LinearLayout.VERTICAL);
      ll.setLayoutParams(WCWC);
      if(item instanceof ColuTxDetailsItem) {
         ll.addView(getColuValue(((ColuTxDetailsItem) item).getAmount(),
                 ((ColuAccount)_mbwManager.getSelectedAccount()).getColuAsset().name));
      }
      if (item.isCoinbase) {
         // Coinbase input
         ll.addView(getValue(item.value, null));
         ll.addView(getCoinbaseText());
      } else {
         String address = item.address.toString();

         // Add BTC value
         ll.addView(getValue(item.value, address));

         AddressLabel adrLabel = new AddressLabel(this);
         adrLabel.setColuMode(coluMode);
         adrLabel.setAddress(item.address);
         ll.addView(adrLabel);
      }
      ll.setPadding(10, 10, 10, 10);
      return ll;
   }


   private View getCoinbaseText() {
      TextView tv = new TextView(this);
      tv.setLayoutParams(FPWC);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      tv.setText(R.string.newly_generated_coins_from_coinbase);
      tv.setTextColor(_white_color);
      return tv;
   }

   private View getValue(final long value, Object tag) {
      TextView tv = new TextView(this);
      tv.setLayoutParams(FPWC);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      if(_mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHSINGLEADDRESS
          || _mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHBIP44) {
         tv.setText(_mbwManager.getBchValueString(value));
      } else {
         tv.setText(_mbwManager.getBtcValueString(value));
      }
      tv.setTextColor(_white_color);
      tv.setTag(tag);

      tv.setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View v) {
            Utils.setClipboardString(CoinUtil.valueString(value, _mbwManager.getCurrencySwitcher().getBitcoinDenomination(), false), getApplicationContext());
            Toast.makeText(getApplicationContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            return true;
         }
      });


      return tv;
   }

   private View getColuValue(final BigDecimal value, String currency) {
      TextView tv = new TextView(this);
      tv.setLayoutParams(FPWC);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      tv.setText(String.format("%s %s", value.stripTrailingZeros().toPlainString(), currency));
      tv.setTextColor(_white_color);

      tv.setOnLongClickListener(new View.OnLongClickListener() {
         @Override
         public boolean onLongClick(View v) {
            Utils.setClipboardString(CoinUtil.valueString(value, _mbwManager.getCurrencySwitcher().getBitcoinDenomination(), false), getApplicationContext());
            Toast.makeText(getApplicationContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            return true;
         }
      });


      return tv;
   }

   /**
    * Async task to perform fetching parent transactions of current transaction from server
    */
   private class UpdateParentTask extends AsyncTask<Transaction, Void, Boolean> {
      @Override
      protected Boolean doInBackground(Transaction... pop) {
         Sha256Hash txid = getTransactionFromIntent();

         if (_mbwManager.getSelectedAccount() instanceof AbstractAccount) {
            AbstractAccount selectedAccount = (AbstractAccount) _mbwManager.getSelectedAccount();
            TransactionEx transactionEx = selectedAccount.getTransaction(txid);
            Transaction transaction = TransactionEx.toTransaction(transactionEx);
            try {
               selectedAccount.fetchStoreAndValidateParentOutputs(Collections.singletonList(transaction),true);
            } catch (WapiException e) {
               _mbwManager.retainingWapiLogger.logError("Can't load parent", e);
               return false;
            }
         }
         return true;
      }

      @Override
      protected void onPostExecute(Boolean isResultOk) {
         super.onPostExecute(isResultOk);
         if (isResultOk) {
            loadAndUpdate(true);
         } else {
            updateUi(true,true);
         }
      }
   }

   private Sha256Hash getTransactionFromIntent() {
      return (Sha256Hash) getIntent().getSerializableExtra("transaction");
   }
}