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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import android.widget.Toast;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.spvmodule.providers.TransactionContract;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.util.AddressLabel;
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay;
import com.mycelium.wallet.activity.util.TransactionDetailsLabel;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.json.ColuTxDetailsItem;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;

import org.bitcoinj.wallet.Wallet;

import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE;
import static com.mycelium.wallet.external.changelly.bch.ExchangeFragment.BCH_EXCHANGE_TRANSACTIONS;

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

      Sha256Hash txid = (Sha256Hash) getIntent().getSerializableExtra("transaction");
      if(_mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHBIP44
          || _mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHSINGLEADDRESS) {
         _tx = _mbwManager.getSpvBchFetcher().retrieveTransactionDetails(txid);
         _txs = _mbwManager.getSpvBchFetcher().retrieveTransactionSummary(txid);
      } else {
         _tx = _mbwManager.getSelectedAccount().getTransactionDetails(txid);
         _txs = _mbwManager.getSelectedAccount().getTransactionSummary(txid);
      }

      if(_mbwManager.getSelectedAccount() instanceof ColuAccount) {
         coluMode = true;
      } else {
         coluMode = false;
      }
      updateUi();
   }

   private void updateUi() {
      // Set Hash
      TransactionDetailsLabel tvHash = findViewById(R.id.tvHash);
      tvHash.setColuMode(coluMode);
      tvHash.setTransaction(_tx);


      // Set Confirmed
      int confirmations = _tx.calculateConfirmations(_mbwManager.getSpvBchFetcher().getBlockchainHeight());
      String confirmed;
      if (_tx.height > 0) {
         confirmed = getResources().getString(R.string.confirmed_in_block, _tx.height);
      } else {
         confirmed = getResources().getString(R.string.no);
      }

      // check if tx is in outgoing queue
      TransactionConfirmationsDisplay confirmationsDisplay = (TransactionConfirmationsDisplay) findViewById(R.id.tcdConfirmations);
      TextView confirmationsCount = (TextView) findViewById(R.id.tvConfirmations);

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

      // Set Inputs
      LinearLayout inputs = (LinearLayout) findViewById(R.id.llInputs);
      if(_tx.inputs != null) {
         for (TransactionDetails.Item item : _tx.inputs) {
            inputs.addView(getItemView(item));
         }
      }

      // Set Outputs
      LinearLayout outputs = (LinearLayout) findViewById(R.id.llOutputs);
      if(_tx.outputs != null) {
         for (TransactionDetails.Item item : _tx.outputs) {
            outputs.addView(getItemView(item));
         }
      }

      // Set Fee
      final long txFeeTotal = getFee(_tx);
      String fee = null;
      if(txFeeTotal > 0) {
         ((TextView) findViewById(R.id.tvFeeLabel)).setVisibility(View.VISIBLE);
         ((TextView) findViewById(R.id.tvInputsLabel)).setVisibility(View.VISIBLE);
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
         ((TextView) findViewById(R.id.tvFee)).setText(fee);
      } else {
         ((TextView) findViewById(R.id.tvFee)).setText("check in Block Explorer");
         ((TextView) findViewById(R.id.tvInputsLabel)).setVisibility(View.GONE);
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
      tv.setText(value.stripTrailingZeros().toPlainString() + " " + currency);
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

}