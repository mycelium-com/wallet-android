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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.UUID;

public class ColdStorageSummaryActivity extends Activity {

   private MbwManager _mbwManager;
   private WalletAccount _account;

   public static void callMe(Activity currentActivity, UUID account) {
      Intent intent = new Intent(currentActivity, ColdStorageSummaryActivity.class);
      intent.putExtra("account", account);
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
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra("account"));
      if (_mbwManager.getWalletManager(true).getAccountIds().contains(accountId)) {
         _account = _mbwManager.getWalletManager(true).getAccount(accountId);
      } else {
         //this can happen if we were in background for long time and then came back
         //just go back and have the user scan again is probably okay as a workaround
         finish();
         return;
      }

   }

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   private void updateUi(){
      Balance balance = _account.getBalance();

      // Description
      if (_account.canSpend()) {
         ((TextView) findViewById(R.id.tvDescription)).setText(R.string.cs_private_key_description);
      } else {
         ((TextView) findViewById(R.id.tvDescription)).setText(R.string.cs_address_description);
      }

      // Address
      ((TextView) findViewById(R.id.tvAddress)).setText(_account.getReceivingAddress().toMultiLineString());

      // Balance
      ((TextView) findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(balance.getSpendableBalance()));

      ExchangeRate rate = _mbwManager.getExchangeRateManager().getExchangeRate();
      Double oneBtcInFiat;
      if(rate!= null){
         oneBtcInFiat = rate.price;
      }else{
         oneBtcInFiat = null;
      }

      // Fiat
      if (!_mbwManager.hasFiatCurrency() || oneBtcInFiat == null) {
         findViewById(R.id.tvFiat).setVisibility(View.INVISIBLE);
      } else {
         TextView tvFiat = (TextView) findViewById(R.id.tvFiat);
         String converted = Utils.getFiatValueAsString(balance.getSpendableBalance(), oneBtcInFiat);
         String currency = _mbwManager.getFiatCurrency();
         tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
      }

      // Show/Hide Receiving
      if (balance.getReceivingBalance() > 0) {
         String receivingString = _mbwManager.getBtcValueString(balance.getReceivingBalance());
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = (TextView) findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvReceiving).setVisibility(View.GONE);
      }

      // Show/Hide Sending
      if (balance.getSendingBalance() > 0) {
         String sendingString = _mbwManager.getBtcValueString(balance.getSendingBalance());
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = (TextView) findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvSending).setVisibility(View.GONE);
      }

      // Send Button
      Button btSend = (Button) findViewById(R.id.btSend);
      if (_account.canSpend()) {
         if (balance.getSpendableBalance() > 0) {
            btSend.setEnabled(true);
            btSend.setOnClickListener(new OnClickListener() {

               @Override
               public void onClick(View arg0) {
                  SendMainActivity.callMe(ColdStorageSummaryActivity.this, _account.getId(), true);
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

   @Override
   public void onBackPressed() {
      //delete temporary accounts keys so we do not keep scanned private keys in memory when user presses back
      _mbwManager.forgetColdStorageWalletManager();
      super.onBackPressed();
   }

}