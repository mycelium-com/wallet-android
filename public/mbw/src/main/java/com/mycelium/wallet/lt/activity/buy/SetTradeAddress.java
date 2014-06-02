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

package com.mycelium.wallet.lt.activity.buy;

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
import com.mrd.bitlib.model.Address;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.lt.activity.SendRequestActivity;
import com.mycelium.wallet.lt.api.SetTradeReceivingAddress;

public class SetTradeAddress extends Activity {

   public static void callMe(Activity currentActivity, TradeSession tradeSession) {
      Intent intent = new Intent(currentActivity, SetTradeAddress.class);
      Preconditions.checkNotNull(tradeSession);
      Preconditions.checkNotNull(tradeSession.id);
      intent.putExtra("tradeSession", tradeSession);
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      currentActivity.startActivity(intent);
   }

   private TradeSession _tradeSession;
   private MbwManager _mbwManager;
   private Address _address;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_set_trade_address_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _tradeSession = (TradeSession) getIntent().getSerializableExtra("tradeSession");
      Preconditions.checkNotNull(_tradeSession);
      Preconditions.checkNotNull(_tradeSession.id);

      _address = _mbwManager.getRecordManager().getWallet(_mbwManager.getWalletMode()).getReceivingAddress();
      // Set label if applicable
      TextView addressLabel = (TextView) findViewById(R.id.tvAddressLabel);
      String label = _mbwManager.getAddressBookManager().getNameByAddress(_address.toString());
      if (label == null || label.length() == 0) {
         // Hide label
         addressLabel.setVisibility(View.GONE);
      } else {
         // Show label
         addressLabel.setText(label);
         addressLabel.setVisibility(View.VISIBLE);
      }

      // Set Address
      ((TextView) findViewById(R.id.tvAddress)).setText(_address.toMultiLineString());

      // Show / hide warning
      Record record = _mbwManager.getRecordManager().getRecord(_address);
      TextView tvWarning = (TextView) findViewById(R.id.tvWarning);
      if (record != null && record.hasPrivateKey()) {
         // We send to an address where we have the private key
         findViewById(R.id.tvWarning).setVisibility(View.GONE);
      } else {
         // Show a warning as we are sending to an address where we don't have
         // the private key
         tvWarning.setVisibility(View.VISIBLE);
         tvWarning.setText(R.string.read_only_warning);
         tvWarning.setTextColor(getResources().getColor(R.color.red));
      }

      ((Button) findViewById(R.id.btOk)).setOnClickListener(startTradingClickListener);
   }

   OnClickListener startTradingClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         SetTradeReceivingAddress request = new SetTradeReceivingAddress(_tradeSession.id, _address);
         SendRequestActivity.callMe(SetTradeAddress.this, request, "");
         finish();
      }
   };

   @Override
   protected void onResume() {
      if(!hasMoreThanOneReceivingAddress()){
         Preconditions.checkNotNull(_tradeSession);
         Preconditions.checkNotNull(_tradeSession.id);
         SetTradeReceivingAddress request = new SetTradeReceivingAddress(_tradeSession.id, _address);
         SendRequestActivity.callMe(SetTradeAddress.this, request, "");
         finish();
      }
      super.onResume();
   }

   @Override
   protected void onPause() {
      super.onPause();
   }

   private boolean hasMoreThanOneReceivingAddress() {
      return _mbwManager.getRecordManager().numRecords() > 1;
   }
}
