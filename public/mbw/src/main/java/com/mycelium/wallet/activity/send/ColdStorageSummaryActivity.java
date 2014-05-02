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
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.BalanceInfo;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;

public class ColdStorageSummaryActivity extends Activity {

   public static final int SCAN_RESULT_CODE = 0;

   private MbwManager _mbwManager;
   private Wallet _wallet;
   private SpendableOutputs _spendable;
   private Double _oneBtcInFiat;

   public static void callMe(Activity currentActivity, Wallet wallet, SpendableOutputs spendable, Double oneBtcInFiat) {
      Intent intent = new Intent(currentActivity, ColdStorageSummaryActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("spendable", spendable);
      intent.putExtra("oneBtcInFiat", oneBtcInFiat);
      intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.cold_storage_summary_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _wallet = Preconditions.checkNotNull((Wallet) getIntent().getSerializableExtra("wallet"));
      _spendable = Preconditions.checkNotNull((SpendableOutputs) getIntent().getSerializableExtra("spendable"));
      // May be null
      _oneBtcInFiat = (Double) getIntent().getSerializableExtra("oneBtcInFiat");

      BalanceInfo balance = _wallet.getLocalBalance(_mbwManager.getBlockChainAddressTracker());

      // Description
      if (_wallet.canSpend()) {
         ((TextView) findViewById(R.id.tvDescription)).setText(R.string.cs_private_key_description);
      } else {
         ((TextView) findViewById(R.id.tvDescription)).setText(R.string.cs_address_description);
      }

      // Address
      ((TextView) findViewById(R.id.tvAddress)).setText(_wallet.getReceivingAddress().toMultiLineString());

      // Balance
      ((TextView) findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(balance.unspent
            + balance.pendingChange));

      // Fiat
      if (_oneBtcInFiat == null) {
         findViewById(R.id.tvFiat).setVisibility(View.INVISIBLE);
      } else {
         TextView tvFiat = (TextView) findViewById(R.id.tvFiat);
         String converted = Utils.getFiatValueAsString(balance.unspent + balance.pendingChange, _oneBtcInFiat);
         String currency = _mbwManager.getFiatCurrency();
         tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
      }

      // Show/Hide Receiving
      if (balance.pendingReceiving > 0) {
         String receivingString = _mbwManager.getBtcValueString(balance.pendingReceiving);
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = (TextView) findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvReceiving).setVisibility(View.GONE);
      }

      // Show/Hide Sending
      if (balance.pendingSending > 0) {
         String sendingString = _mbwManager.getBtcValueString(balance.pendingSending);
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = (TextView) findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvSending).setVisibility(View.GONE);
      }

      // Send Button
      Button btSend = (Button) findViewById(R.id.btSend);
      if (_wallet.canSpend()) {
         if (balance.unspent + balance.pendingChange > 0) {
            btSend.setEnabled(true);
            btSend.setOnClickListener(new OnClickListener() {

               @Override
               public void onClick(View arg0) {
                  SendMainActivity.callMe(ColdStorageSummaryActivity.this, _wallet, _spendable, _oneBtcInFiat, true);
                  finish();
               }
            });
         } else {
            btSend.setEnabled(false);
         }
      } else {
         btSend.setVisibility(View.GONE);
      }

   }

}