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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.UUID;

public class ColdStorageSummaryActivity extends Activity {
   private static final int SEND_MAIN_REQUEST_CODE = 1;
   private MbwManager _mbwManager;
   private WalletAccount _account;

   public static void callMe(Activity currentActivity, UUID account) {
      Intent intent = new Intent(currentActivity, ColdStorageSummaryActivity.class)
              .putExtra("account", account)
              .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.cold_storage_summary_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra("account"));
      if (_mbwManager.getWalletManager(true).getUniqueIds().contains(accountId)) {
         _account = _mbwManager.getWalletManager(true).getAccount(accountId);
      } else {
         //this can happen if we were in background for long time and then came back
         //just go back and have the user scan again is probably okay as a workaround
         finish();
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

      if (!(_account instanceof AbstractAccount)) {
         // Address
         Optional<Address> receivingAddress = _account.getReceivingAddress();
         ((TextView) findViewById(R.id.tvAddress)).setText(receivingAddress.isPresent() ? receivingAddress.get().toMultiLineString() : "");
      } else {
         findViewById(R.id.tvAddress).setVisibility(View.GONE);

         AbstractAccount account = (AbstractAccount) _account;
         Address p2pkhAddress = account.getReceivingAddress(AddressType.P2PKH);
         if (p2pkhAddress != null) {
            final TextView P2PKH = findViewById(R.id.tvAddressP2PKH);
            P2PKH.setVisibility(View.VISIBLE);
            P2PKH.setText(p2pkhAddress.toMultiLineString());
         }
         Address p2shAddress = account.getReceivingAddress(AddressType.P2SH_P2WPKH);
         if (p2shAddress != null) {
            final TextView P2SH = findViewById(R.id.tvAddressP2SH);
            P2SH.setVisibility(View.VISIBLE);
            P2SH.setText(p2shAddress.toMultiLineString());
         }
         Address p2wpkhAddress = account.getReceivingAddress(AddressType.P2WPKH);
         if (p2wpkhAddress != null) {
            final TextView P2WPKH = findViewById(R.id.tvAddressP2WPKH);
            P2WPKH.setVisibility(View.VISIBLE);
            P2WPKH.setText(p2wpkhAddress.toMultiLineString());
         }
      }

      // Balance
      ((TextView) findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(balance.getSpendableBalance()));

      Double price = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();

      // Fiat
      TextView tvFiat = findViewById(R.id.tvFiat);
      if (!_mbwManager.hasFiatCurrency() || price == null) {
         tvFiat.setVisibility(View.INVISIBLE);
      } else {
         String converted = Utils.getFiatValueAsString(balance.getSpendableBalance(), price);
         String currency = _mbwManager.getFiatCurrency();
         tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
      }

      // Show/Hide Receiving
      if (balance.getReceivingBalance() > 0) {
         String receivingString = _mbwManager.getBtcValueString(balance.getReceivingBalance());
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvReceiving).setVisibility(View.GONE);
      }

      // Show/Hide Sending
      if (balance.getSendingBalance() > 0) {
         String sendingString = _mbwManager.getBtcValueString(balance.getSendingBalance());
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         findViewById(R.id.tvSending).setVisibility(View.GONE);
      }

      // Send Button
      Button btSend = findViewById(R.id.btSend);
      if (_account.canSpend()) {
         if (balance.getSpendableBalance() > 0) {
            btSend.setEnabled(true);
            btSend.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View arg0) {
                  Intent intent = SendMainActivity.getIntent(ColdStorageSummaryActivity.this, _account.getId(), true);
                  ColdStorageSummaryActivity.this.startActivityForResult(intent, SEND_MAIN_REQUEST_CODE);
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
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == SEND_MAIN_REQUEST_CODE) {
         setResult(resultCode, data);
         finish();
      } else {
         super.onActivityResult(requestCode, resultCode, data);
      }
   }
}